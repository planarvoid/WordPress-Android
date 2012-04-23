package com.soundcloud.android.service.beta;

import static com.soundcloud.android.utils.CloudUtils.getAppVersionCode;
import static com.soundcloud.android.utils.CloudUtils.getElapsedTimeString;

import com.soundcloud.android.Consts;
import com.soundcloud.android.R;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.activity.settings.Settings;
import com.soundcloud.android.utils.IOUtils;
import org.apache.http.client.HttpClient;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.IBinder;
import android.os.PowerManager;
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
 * <p/>
 * Expects to find apks in the S3 bucket {@link BetaService#BETA_BUCKET} with the following filename convention:
 * <code>packagename-versioncode.apk</code>.
 */
public class BetaService extends Service {
    public static final String TAG = "BetaService";

    public static final Uri BETA_BUCKET = Uri.parse("http://soundcloud-android-beta.s3.amazonaws.com/");
    public static final File APK_PATH = new File(Consts.FILES_PATH, "beta");
    public static final String PREF_CHECK_UPDATES = "beta.check_for_updates";
    public static final String PREF_REQUIRE_WIFI = "beta.require_wifi";
    public static final String PREF_BETA_VERSION = "beta.beta_version";


    /**
     * How often should the update check run
     */
    public static final long INTERVAL = AlarmManager.INTERVAL_HALF_DAY;
    public static final String EXTRA_MANUAL = "com.soundcloud.android.extra.beta.manual";
    private static final String USER_AGENT = "SoundCloud Android BetaService";

    private HttpClient mClient;

    private WifiManager.WifiLock mWifiLock;
    private PowerManager.WakeLock mWakeLock;

    private static boolean sRunning;

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "onStartCommand(" + intent + "," + flags + "," + startId + ")");
        acquireLocks();

        final boolean manual = isManual(intent);
        synchronized (BetaService.class) {
            if (sRunning) {
                Log.d(TAG, "already running");
            } else if (!shouldCheckForUpdates(this) && !manual) {
                skip(null, "User disabled auto-update", intent);
            } else if (!IOUtils.isSDCardAvailable()) {
                skip(null, "SD card not available", intent);
            } else if (!isBackgroundDataEnabled() && !manual) {
                skip(null, "Background data disabled", intent);
            } else if (!isWifi()) {
                skip(null, "Wifi is disabled", intent);
            } else {
                sRunning = true;
                mClient = IOUtils.createHttpClient(USER_AGENT);

                checkForUpdates(intent);
            }
        }
        return START_NOT_STICKY;
    }

    private static boolean isManual(Intent intent) {
        return intent != null && intent.getBooleanExtra(EXTRA_MANUAL, false);
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "onDestroy()");
        sRunning = false;

        releaseLocks();
        IOUtils.closeHttpClient(mClient);
    }

    /* package */ void checkForUpdates(final Intent intent) {
        new GetS3ContentTask(mClient) {
            @Override
            protected void onPostExecute(List<Beta> betas) {
                if (betas != null) {
                    Beta recent = selectVersion(betas);
                     if (recent != null) {
                         if (!recent.isDownloaded()) {
                            download(recent, intent);
                        } else {
                             try {
                                 Beta local = Beta.fromJSON(recent.getMetaDataFile());
                                 if (!local.isInstalled(BetaService.this) &&
                                     !SoundCloudApplication.DEV_MODE) {
                                     // nag user to install the new beta version
                                     notifyNewVersion(local);
                                     Log.d(TAG, "new version downloaded but not installed");
                                 } else {
                                     Log.d(TAG, "nothing to download");
                                 }
                             } catch (IOException e) {
                                 Log.w(TAG, e);
                             }
                             stopSelf();
                         }
                     } else {
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

    private Beta selectVersion(List<Beta> available) {
        if (available.isEmpty()) {
            return null;
        } else {
            Collections.sort(available);
            Beta first = available.get(0);

            if (first.getVersionCode() >= getAppVersionCode(this, -1)) {
                return first;
            } else {
                return null;
            }
        }
    }

    private void download(final Beta beta, Intent intent) {
        if (!beta.isEnoughStorageLeft()) {
            notifyLowStorage(beta);
            skip(beta, "not enough diskspace", intent);
        } else {
            doDownload(beta, intent);
        }
    }

    private void skip(Beta beta, String reason, Intent intent) {
        String message;
        if (beta == null) {
            message = "skipping betaservice run: " + reason;
        } else {
            message = "skipping download of " + beta + ": " + reason;
        }
        Log.d(TAG, message);
        if (isManual(intent)) notifySkipped(message);
        stopSelf();
    }

    private void doDownload(final Beta beta, final Intent intent) {
        fetchMetadata(beta, intent,
                new DownloadBetaTask(mClient) {
                    private long start;

                    @Override
                    protected void onPreExecute() {
                        Log.d(TAG, "downloading " + beta);
                        start = System.currentTimeMillis();
                    }

                    @Override
                    protected void onPostExecute(File file) {
                        if (file != null && file.exists()) {
                            Log.d(TAG, "downloaded " + file +
                                    " in " + ((System.currentTimeMillis() - start) / 1000L) + " secs");

                            try {
                                beta.touch();
                                beta.persist();
                                clearPendingBeta(BetaService.this);

                                if (!beta.isInstalled(BetaService.this)) {
                                    notifyNewVersion(beta);
                                } else {
                                    Log.d(TAG, "version is already installed, not notifying");
                                }

                                new CleanupBetaTask() {
                                    @Override
                                    protected void onPostExecute(List<File> files) {
                                        stopSelf();
                                    }
                                }.execute(file);
                            } catch (IOException e) {
                                Log.w(TAG, "could not persist " + beta, e);
                                beta.deleteFiles();
                                stopSelf();
                            }
                        } else {
                            Log.w(TAG, "could not download " + beta);
                            if (isManual(intent)) {
                                notifyDownloadFailure(beta);
                            }
                            beta.deleteFiles();
                            stopSelf();
                        }
                    }
                });
    }

    private void fetchMetadata(final Beta beta, final Intent intent, final AsyncTask<Beta, Void, File> next) {
        new GetS3MetadataTask(mClient) {
            @Override
            protected void onPostExecute(Beta content) {
                if (content != null) {
                    next.execute(content);
                } else {
                    Log.w(TAG, "could not retrieve metadata for " + content);
                    if (isManual(intent)) {
                        notifyDownloadFailure(content);
                    }
                    stopSelf();
                }
            }
        }.execute(beta);
    }


    private void notifyNewVersion(Beta apk) {
        String title = getString(R.string.pref_beta_new_version_downloaded);
        String content = getString(R.string.pref_beta_new_version_downloaded_content,
                apk.getVersionName(),
                getElapsedTimeString(getResources(), apk.lastmodified, true));

        String ticker = getString(R.string.pref_beta_new_version_downloaded_ticker);

        Notification n = new Notification(R.drawable.ic_status, ticker, apk.lastmodified);
        n.flags |= defaultNotificationFlags();
        n.setLatestEventInfo(this, title, content, PendingIntent.getActivity(this, 0, apk.getInstallIntent(), 0));
        NotificationManager mgr = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        mgr.notify(Consts.Notifications.BETA_NOTIFY_ID, n);
    }


    private void notifyLowStorage(Beta beta) {
        String title = getString(R.string.pref_beta_not_enough_storage_title);
        String ncontent = getString(R.string.pref_beta_not_enough_storage_content, beta.key);
        Intent intent = new Intent(android.provider.Settings.ACTION_MEMORY_CARD_SETTINGS);

        Notification n = new Notification(R.drawable.ic_status, title, System.currentTimeMillis());
        n.flags |= defaultNotificationFlags();
        n.setLatestEventInfo(this, title, ncontent, PendingIntent.getActivity(this, 0, intent, 0));
        NotificationManager mgr = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        mgr.notify(Consts.Notifications.BETA_NOTIFY_ID, n);
    }

    private void notifyDownloadFailure(Beta beta) {
        String title = getString(R.string.pref_beta_download_failed_title);
        String ncontent = getString(R.string.pref_beta_download_failed_content, beta.key);
        Intent intent = new Intent(this, Settings.class)
                .setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);

        Notification n = new Notification(R.drawable.ic_status, title, System.currentTimeMillis());
        n.flags |= defaultNotificationFlags();
        n.setLatestEventInfo(this, title, ncontent, PendingIntent.getActivity(this, 0, intent, 0));
        NotificationManager mgr = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        mgr.notify(Consts.Notifications.BETA_NOTIFY_ID, n);
    }

    private void notifySkipped(String message) {
        String title = getString(R.string.pref_beta_skipped);
        Intent intent = new Intent(this, Settings.class)
                .setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);

        Notification n = new Notification(R.drawable.ic_status, title, System.currentTimeMillis());
        n.flags |= defaultNotificationFlags();
        n.setLatestEventInfo(this, title, message, PendingIntent.getActivity(this, 0, intent, 0));
        NotificationManager mgr = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        mgr.notify(Consts.Notifications.BETA_NOTIFY_ID, n);

    }


    public static void onNewBeta(Context context, Intent intent) {
        final String beta = intent.getStringExtra(Beta.EXTRA_BETA_VERSION);
        final String[] parts = beta != null ? beta.split(":", 2) : new String[0];
        if (parts.length == 2) {
            try {
                final int versionCode = Integer.parseInt(parts[0]);
                final String versionName = parts[1];
                if (!Beta.isInstalled(context, versionCode, versionName)) {
                    notifyNewVersion(context, versionName + "  ("+versionCode+")");
                    setPendingBeta(context, versionName);
                    scheduleNow(context, 2000l);
                } else {
                    if (Log.isLoggable(TAG, Log.DEBUG)) {
                        Log.d(TAG, "beta already installed");
                    }
                }
            } catch (NumberFormatException e) {
                Log.w(TAG, "could not parse version information: "+beta);
            }
        } else {
            Log.w(TAG, "could not get beta information from intent "+intent);
        }
    }

    /** @noinspection UnusedDeclaration*/
    private static void notifyNewVersion(Context context, String version) {
         String title = context.getString(R.string.pref_beta_new_version_available);
         Intent intent = new Intent(context, Settings.class)
                  .setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);

         Notification n = new Notification(R.drawable.ic_status, title, System.currentTimeMillis());
         n.flags |= BetaService.defaultNotificationFlags();
         n.setLatestEventInfo(context, title, version, PendingIntent.getActivity(context, 0, intent ,0 ));
         NotificationManager mgr = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
         mgr.notify(Consts.Notifications.BETA_NOTIFY_ID, n);
     }

    /* package */ static int defaultNotificationFlags() {
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

        Log.d(TAG, "BETA mode enabled, scheduling update checks " +
                "(every " + BetaService.INTERVAL / 1000 / 60 + " minutes, exact=" + exact + ")");
    }

    public static void scheduleNow(Context context, long delay) {
        AlarmManager alarm = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        alarm.set(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + delay,
                PendingIntent.getService(context, 0,
                        new Intent(context, BetaService.class), 0));
    }


    private boolean isBackgroundDataEnabled() {
        ConnectivityManager c = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        return c.getBackgroundDataSetting();
    }

    private boolean isWifi() {
        boolean requireWifi = PreferenceManager.getDefaultSharedPreferences(this)
                .getBoolean(PREF_REQUIRE_WIFI, true);

        if (!requireWifi || SoundCloudApplication.EMULATOR) {
            return true;
        } else {
            ConnectivityManager c = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
            WifiManager wMgr = (WifiManager) getSystemService(WIFI_SERVICE);
            return (wMgr.isWifiEnabled() &&
                    c.getNetworkInfo(ConnectivityManager.TYPE_WIFI) != null &&
                    c.getNetworkInfo(ConnectivityManager.TYPE_WIFI).isConnected());
        }
    }

    static List<Beta> getBetas() {
        if (IOUtils.isSDCardAvailable()) {
            File[] md = APK_PATH.listFiles(new FilenameFilter() {
                @Override
                public boolean accept(File file, String s) {
                    return s.endsWith(Beta.META_DATA_EXT);
                }
            });
            if (md != null) {
                List<Beta> betas = new ArrayList<Beta>(md.length);
                for (File f : md) {
                    try {
                        betas.add(Beta.fromJSON(f));
                    } catch (IOException e) {
                        Log.w(TAG, "unreadable metadata: " + f);
                    }
                }
                return betas;
            } else {
                return Collections.emptyList();
            }
        } else {
            return Collections.emptyList();
        }
    }

    static Beta getMostRecentContent() {
        List<Beta> betas = getBetas();
        Collections.sort(betas);
        return betas.isEmpty() ? null : betas.get(0);
    }

    static boolean isUptodate(Context context) {
        Beta recent = getMostRecentContent();
        return recent == null || recent.isInstalled(context);
    }

    private synchronized void acquireLocks() {
        acquireWifiLock();
        acquireWakeLock();
    }

    private synchronized void releaseLocks() {
        releaseWifiLock();
        releaseWakeLock();
    }

    private synchronized void acquireWifiLock() {
        if (mWifiLock == null) {
            WifiManager wMgr = (WifiManager) getSystemService(Context.WIFI_SERVICE);
            mWifiLock = wMgr.createWifiLock(TAG);
        }
        if (mWifiLock != null) mWifiLock.acquire();
    }

    private void releaseWifiLock() {
        if (mWifiLock != null) {
            mWifiLock.release();
        }
    }

    private synchronized void acquireWakeLock() {
        if (mWakeLock == null) {
            PowerManager mgr = (PowerManager) getSystemService(Context.POWER_SERVICE);
            mWakeLock = mgr.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, TAG);
        }
        mWakeLock.acquire();
    }

    private void releaseWakeLock() {
        if (mWakeLock != null) {
            mWakeLock.release();
        }
    }

    public static boolean isPendingBeta(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context).contains(PREF_BETA_VERSION);
    }

    public static void setPendingBeta(Context context, String version) {
        PreferenceManager.getDefaultSharedPreferences(context)
                .edit()
                .putString(PREF_BETA_VERSION, version)
                .commit();
    }

    public static void clearPendingBeta(Context context) {
        PreferenceManager.getDefaultSharedPreferences(context)
                .edit()
                .remove(PREF_BETA_VERSION)
                .commit();
    }
}

