package com.soundcloud.android.service.beta;

import static com.soundcloud.android.utils.CloudUtils.getTimeElapsed;

import com.soundcloud.android.Consts;
import com.soundcloud.android.R;
import com.soundcloud.android.SoundCloudApplication;
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
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.Uri;
import android.net.http.AndroidHttpClient;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Environment;
import android.os.IBinder;
import android.os.StatFs;
import android.preference.PreferenceManager;
import android.util.Log;

import java.io.File;
import java.io.IOException;
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

        synchronized (BetaService.class) {
            if (sRunning) {
                Log.d(TAG, "already running");
            } else if (!shouldCheckForUpdates(this)) {
                skip(null, "user disabled update check");
            } else if (!isDiskMounted()) {
                skip(null, "no SD card");
            } else if (!isBackgroundDataEnabled()) {
                skip(null, "no bg data");
            } else if (!isWifi()) {
                skip(null, "no wifi");
            } else {
                sRunning = true;
                mClient = createHttpClient();
                acquireLock();

                checkForUpdates(intent);
            }
        }
        return START_NOT_STICKY;
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
                        download(recent);
                    } else {
                        Log.d(TAG, "nothing to download, stopping service");
                        stopSelf();
                    }
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

            if (first.getVersion() >= getVersionCode()) {
                return first;
            } else {
                return null;
            }
        }
    }

    private void download(final Content content) {
        if (!isEnoughDiskLeft(content)) {
            notifyLowDiskspace(content);
            skip(content, "not enough diskspace");
        } else if (!isBackgroundDataEnabled()) {
            skip(content, "no bg data");
        } else if (!isWifi()) {
            skip(content, "no wifi");
        } else {
            doDownload(content);
        }
    }

    private void skip(Content content, String reason) {
        if (content == null) {
            Log.w(TAG, "skipping betaservice run: "+reason);
        } else {
            Log.w(TAG, "skipping download of " + content + ": " +reason);
        }
        stopSelf();
    }

    private void doDownload(final Content content) {
        fetchMetadata(content,
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

                            if (!file.setLastModified(content.lastmodified)) {
                                Log.w(TAG, "could not set last modified");
                            }
                            try {
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
                            content.deleteFiles();
                            stopSelf();
                        }
                    }
                });
    }

    private void fetchMetadata(final Content content, final AsyncTask<Content, Void, File> next) {
        new GetS3MetadataTask(mClient) {
            @Override
            protected void onPostExecute(Content content) {
                if (content != null) {
                    next.execute(content);
                } else {
                    Log.w(TAG, "could not retrieve metadata for "+content);
                    stopSelf();
                }
            }
        }.execute(content);
    }

    private boolean isEnoughDiskLeft(Content content) {
        StatFs fs = new StatFs(Environment.getExternalStorageDirectory().getAbsolutePath());
        long free = fs.getAvailableBlocks() * fs.getBlockCount();
        return content.size < free;
    }

    private boolean isDiskMounted() {
        return Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState());
    }

    private void notifyNewVersion(Content apk) {
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setDataAndType(Uri.fromFile(apk.getLocalFile()), "application/vnd.android.package-archive");

        String title   = "New beta version available";
        String content = "Updated " + getTimeElapsed(getResources(), apk.lastmodified);
        String ticker  = "Beta update";

        Notification n = new Notification(R.drawable.statusbar, ticker, apk.lastmodified);
        n.flags |= defaultNotificationFlags();
        n.setLatestEventInfo(this, title, content, PendingIntent.getActivity(this, 0, intent, 0));
        NotificationManager mgr = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        mgr.notify(Consts.Notifications.BETA_NOTIFY_ID, n);
    }

    private void notifyLowDiskspace(Content content) {
        String title = "Not enough diskspace";
        String ncontent = "to download beta " + content.key;

        Notification n = new Notification(R.drawable.statusbar, title, System.currentTimeMillis());
        n.flags |= defaultNotificationFlags();
        n.setLatestEventInfo(this, title, ncontent, null);
        NotificationManager mgr = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        mgr.notify(Consts.Notifications.BETA_NOTIFY_ID, n);
    }

    private int defaultNotificationFlags() {
       return Notification.FLAG_ONLY_ALERT_ONCE |
              Notification.FLAG_AUTO_CANCEL |
              Notification.DEFAULT_LIGHTS;
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
    }

    private int getVersionCode() {
        try {
            PackageInfo info = getPackageManager().getPackageInfo(getPackageName(), PackageManager.GET_META_DATA);
            return info.versionCode;
        } catch (PackageManager.NameNotFoundException e) {
            // should not happen
            throw new RuntimeException(e);
        }
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
}

