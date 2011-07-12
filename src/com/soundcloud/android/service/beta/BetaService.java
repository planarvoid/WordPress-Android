package com.soundcloud.android.service.beta;

import static com.soundcloud.android.utils.CloudUtils.getAppVersionCode;
import static com.soundcloud.android.utils.CloudUtils.getElapsedTimeString;

import com.soundcloud.android.Consts;
import com.soundcloud.android.R;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.activity.Settings;
import com.soundcloud.api.Http;
import org.apache.http.client.HttpClient;
import org.apache.http.impl.client.DefaultHttpClient;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.Uri;
import android.net.http.AndroidHttpClient;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Environment;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.util.Log;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;


/**
 * A service to automatically download SoundCloud betas + prompt the user to install them.
 *
 * Expects to find apks in the S3 bucket {@link BetaService#BETA_BUCKET} with the following filename convention:
 * <code>packagename-versioncode.apk</code>.
 */
public class BetaService extends Service {
    public static final String TAG = "BetaService";

    public static final Uri BETA_BUCKET = Uri.parse("http://soundcloud-android-beta.s3.amazonaws.com/");
    public static final File APK_PATH = new File(Consts.FILES_PATH, "beta");
    public static final String PREF_CHECK_UPDATES = "beta_check_for_updates";

    /** How often should the update check run */
    public static final long INTERVAL = AlarmManager.INTERVAL_HOUR;
    public static final String EXTRA_MANUAL = "com.soundcloud.android.extra.beta.manual";
    private static final String USER_AGENT = "SoundCloud Android BetaService";

    private HttpClient mClient;

    private WifiManager.WifiLock mWifiLock;
    private static boolean sRunning;

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "onStartCommand(" + intent + "," + flags + "," + startId + ")");

        final boolean manual = isManual(intent);
        synchronized (BetaService.class) {
            if (sRunning) {
                Log.d(TAG, "already running");
            } else if (!shouldCheckForUpdates(this) && !manual) {
                skip(null, "User disabled auto-update", intent);
            } else if (!isStorageAvailable()) {
                skip(null, "SD card not available", intent);
            } else if (!isBackgroundDataEnabled() && !manual) {
                skip(null, "Background data disabled", intent);
            } else if (!isWifi()) {
                skip(null, "Wifi is disabled", intent);
            } else {
                sRunning = true;
                mClient = createHttpClient();
                acquireLock();

                checkForUpdates(intent);
            }
        }
        return START_NOT_STICKY;
    }

    private static boolean isManual(Intent intent) {
        return intent.getBooleanExtra(EXTRA_MANUAL, false);
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "onDestroy()");
        sRunning = false;

        releaseLock();

        if (mClient != null) {
            if (mClient instanceof AndroidHttpClient) {
                // avoid leak error logging
                ((AndroidHttpClient)mClient).close();
            } else {
                mClient.getConnectionManager().shutdown();
            }
        }
    }

    private HttpClient createHttpClient() {
        if (Build.VERSION.SDK_INT >= 8) {
            return AndroidHttpClient.newInstance(USER_AGENT);
        } else {
            return new DefaultHttpClient(Http.defaultParams());
        }
    }

    private synchronized void acquireLock() {
        if (mWifiLock == null) {
            WifiManager wMgr = (WifiManager) getSystemService(Context.WIFI_SERVICE);
            mWifiLock = wMgr.createWifiLock(TAG);
        }
        mWifiLock.acquire();
    }

    private synchronized void releaseLock() {
        if (mWifiLock != null) {
            mWifiLock.release();
        }
    }

    private void checkForUpdates(final Intent intent) {
        new GetS3ContentTask(mClient) {
            @Override
            protected void onPostExecute(List<Content> contents) {
                if (contents != null) {
                    Content recent = selectVersion(contents);

                    if (recent != null && !recent.isDownloaded()) {
                        download(recent, intent);
                    } else {
                        Log.d(TAG, "nothing to download");
                        stopSelf();
                    }
                } else {
                    Log.d(TAG, "could not fetch content overview");
                    stopSelf();
                }
            }
        }.execute(BETA_BUCKET);
    }

    static boolean shouldCheckForUpdates(Context c) {
        return PreferenceManager.getDefaultSharedPreferences(c).getBoolean(PREF_CHECK_UPDATES, true);
    }

    private Content selectVersion(List<Content> available) {
        if (available.isEmpty()) {
            return null;
        } else {
            Collections.sort(available);
            Content first = available.get(0);

            if (first.getVersionCode() >= getAppVersionCode(this, -1)) {
                return first;
            } else {
                return null;
            }
        }
    }

    private void download(final Content content, Intent intent) {
        if (!content.isEnoughStorageLeft()) {
            notifyLowStorage(content);
            skip(content, "not enough diskspace", intent);
        } else {
            doDownload(content, intent);
        }
    }

    private void skip(Content content, String reason, Intent intent) {
        String message;
        if (content == null) {
            message = "skipping betaservice run: "+reason;
        } else {
            message = "skipping download of " + content + ": " +reason;
        }
        Log.d(TAG, message);
        if (isManual(intent)) notifySkipped(message);
        stopSelf();
    }

    private void doDownload(final Content content, final Intent intent) {
        fetchMetadata(content, intent,
                new DownloadContentTask(mClient) {
                    private long start;

                    @Override
                    protected void onPreExecute() {
                        Log.d(TAG, "downloading " + content);
                        start = System.currentTimeMillis();
                    }

                    @Override
                    protected void onPostExecute(File file) {
                        if (file != null) {
                            Log.d(TAG, "downloaded " + file +
                                    " in " + ((System.currentTimeMillis() - start) / 1000L) + " secs");

                            try {
                                content.touch();
                                content.persist();
                                notifyNewVersion(content);

                                new CleanupBetaTask() {
                                    @Override protected void onPostExecute(List<File> files) {
                                        stopSelf();
                                    }
                                }.execute(file);
                            } catch (IOException e) {
                                Log.w(TAG, "could not persist " + content);
                                content.deleteFiles();
                                stopSelf();
                            }
                        } else {
                            Log.w(TAG, "could not download " + content);
                            if (isManual(intent)) {
                                notifyDownloadFailure(content);
                            }

                            content.deleteFiles();
                            stopSelf();
                        }
                    }
                });
    }

    private void fetchMetadata(final Content content, final Intent intent, final AsyncTask<Content, Void, File> next) {
        new GetS3MetadataTask(mClient) {
            @Override
            protected void onPostExecute(Content content) {
                if (content != null) {
                    next.execute(content);
                } else {
                    Log.w(TAG, "could not retrieve metadata for "+content);
                    if (isManual(intent)) {
                        notifyDownloadFailure(content);
                    }
                    stopSelf();
                }
            }
        }.execute(content);
    }

    private static boolean isStorageAvailable() {
        return Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState());
    }

    private void notifyNewVersion(Content apk) {
        String title   = getString(R.string.pref_beta_new_version_available);
        String content = getString(R.string.pref_beta_new_version_available_content,
                apk.getVersionName(),
                getElapsedTimeString(getResources(), apk.lastmodified));

        String ticker  = getString(R.string.pref_beta_new_version_available_ticker);

        Notification n = new Notification(R.drawable.statusbar, ticker, apk.lastmodified);
        n.flags |= defaultNotificationFlags();
        n.setLatestEventInfo(this, title, content, PendingIntent.getActivity(this, 0, apk.getInstallIntent(), 0));
        NotificationManager mgr = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        mgr.notify(Consts.Notifications.BETA_NOTIFY_ID, n);
    }


    private void notifyLowStorage(Content content) {
        String title = getString(R.string.pref_beta_not_enough_storage_title);
        String ncontent = getString(R.string.pref_beta_not_enough_storage_content,  content.key);
        Intent intent = new Intent(android.provider.Settings.ACTION_MEMORY_CARD_SETTINGS);

        Notification n = new Notification(R.drawable.statusbar, title, System.currentTimeMillis());
        n.flags |= defaultNotificationFlags();
        n.setLatestEventInfo(this, title, ncontent, PendingIntent.getActivity(this, 0, intent ,0 ));
        NotificationManager mgr = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        mgr.notify(Consts.Notifications.BETA_NOTIFY_ID, n);
    }

     private void notifyDownloadFailure(Content content) {
        String title = getString(R.string.pref_beta_download_failed_title);
        String ncontent = getString(R.string.pref_beta_download_failed_content,  content.key);
        Intent intent = new Intent(this, Settings.class)
                 .setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);

        Notification n = new Notification(R.drawable.statusbar, title, System.currentTimeMillis());
        n.flags |= defaultNotificationFlags();
        n.setLatestEventInfo(this, title, ncontent, PendingIntent.getActivity(this, 0, intent ,0 ));
        NotificationManager mgr = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        mgr.notify(Consts.Notifications.BETA_NOTIFY_ID, n);
    }

    private void notifySkipped(String message) {
        String title = getString(R.string.pref_beta_skipped);
        Intent intent = new Intent(this, Settings.class)
                .setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);

        Notification n = new Notification(R.drawable.statusbar, title, System.currentTimeMillis());
        n.flags |= defaultNotificationFlags();
        n.setLatestEventInfo(this, title, message, PendingIntent.getActivity(this, 0, intent, 0));
        NotificationManager mgr = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        mgr.notify(Consts.Notifications.BETA_NOTIFY_ID, n);

    }

    private int defaultNotificationFlags() {
       return Notification.FLAG_ONLY_ALERT_ONCE |
              Notification.FLAG_AUTO_CANCEL |
              Notification.DEFAULT_LIGHTS;
    }

    public static boolean checkNow(Context context) {
        return context.startService(
                new Intent(context, BetaService.class).putExtra(EXTRA_MANUAL, true)) != null;
    }

    public static void scheduleCheck(Context context, boolean exact) {
        AlarmManager alarm = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        final PendingIntent pi = PendingIntent.getService(context, 0,
                        new Intent(context, BetaService.class), 0);

        alarm.cancel(pi);

        if (!exact) {
            alarm.setInexactRepeating(
                    AlarmManager.RTC,
                    System.currentTimeMillis(),
                    INTERVAL,
                    pi);
        } else {
            alarm.setRepeating(
                    AlarmManager.RTC,
                    System.currentTimeMillis(),
                    INTERVAL,
                    pi);
        }

        Log.d(TAG, "BETA mode enabled, scheduling update checks "+
                "(every "+BetaService.INTERVAL/1000/60+" minutes, exact="+exact+")");
    }

    private boolean isBackgroundDataEnabled() {
        ConnectivityManager c = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        return c.getBackgroundDataSetting();
    }

    private boolean isWifi() {
        if (SoundCloudApplication.EMULATOR) {
            return true;
        } else {
            ConnectivityManager c = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
            WifiManager wMgr = (WifiManager) getSystemService(WIFI_SERVICE);
            return (wMgr.isWifiEnabled() &&
                c.getNetworkInfo(ConnectivityManager.TYPE_WIFI) != null &&
                c.getNetworkInfo(ConnectivityManager.TYPE_WIFI).isConnected());
        }
    }


    static List<Content> getContents() {
        if (isStorageAvailable()) {
            File[] md = APK_PATH.listFiles(new FilenameFilter() {
                @Override
                public boolean accept(File file, String s) {
                    return s.endsWith(Content.META_DATA_EXT);
                }
            });
            if (md != null) {
                List<Content> contents = new ArrayList<Content>(md.length);
                for (File f : md) {
                    try {
                        contents.add(Content.fromJSON(f));
                    } catch (IOException e) {
                        Log.w(TAG, "unreadable metadata: "+f);
                    }
                }
                return contents;
            } else {
                return Collections.emptyList();
            }
        } else {
            return Collections.emptyList();
        }
    }

    static Content getMostRecentContent() {
        List<Content> contents = getContents();
        Collections.sort(contents);
        return contents.isEmpty() ? null : contents.get(0);
    }
}

