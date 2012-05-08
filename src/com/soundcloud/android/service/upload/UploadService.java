package com.soundcloud.android.service.upload;

import static com.soundcloud.android.Consts.Notifications.UPLOADED_NOTIFY_ID;
import static com.soundcloud.android.Consts.Notifications.UPLOADING_NOTIFY_ID;

import com.soundcloud.android.Actions;
import com.soundcloud.android.R;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.activity.UserBrowser;
import com.soundcloud.android.model.Recording;
import com.soundcloud.android.provider.SoundCloudDB;
import com.soundcloud.android.service.LocalBinder;
import com.soundcloud.android.service.record.SoundRecorderService;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.PowerManager;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.widget.RemoteViews;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class UploadService extends Service {
    /* package */ static final String TAG = UploadService.class.getSimpleName();

    public static final String EXTRA_RECORDING   = "recording";
    public static final String EXTRA_TRANSFERRED = "transferred";
    public static final String EXTRA_TOTAL       = "total";
    public static final String EXTRA_PROGRESS    = "progress";

    public static final String UPLOAD_STARTED    = "com.soundcloud.android.service.upload.started";
    public static final String UPLOAD_PROGRESS   = "com.soundcloud.android.service.upload.progress";
    public static final String UPLOAD_SUCCESS    = "com.soundcloud.android.service.upload.success";
    public static final String UPLOAD_ERROR      = "com.soundcloud.android.service.upload.error";
    public static final String UPLOAD_CANCELLED  = "com.soundcloud.android.service.upload.cancelled";
    public static final String UPLOAD_CANCEL     = "com.soundcloud.android.service.upload.cancel";

    public static final String ENCODING_STARTED  = "com.soundcloud.android.service.upload.encoding_started";
    public static final String ENCODING_SUCCESS  = "com.soundcloud.android.service.upload.encoding_success";
    public static final String ENCODING_ERROR    = "com.soundcloud.android.service.upload.encoding_error";
    public static final String ENCODING_CANCELED = "com.soundcloud.android.service.upload.encoding_cancelled";
    public static final String ENCODING_PROGRESS = "com.soundcloud.android.service.upload.encoding_progress";

    public static final String RESIZE_STARTED    = "com.soundcloud.android.service.upload.resize_started";
    public static final String RESIZE_SUCCESS    = "com.soundcloud.android.service.upload.resize_success";
    public static final String RESIZE_ERROR      = "com.soundcloud.android.service.upload.resize_error";

    public static final String[] ALL_ACTIONS = {
            UPLOAD_STARTED,
            UPLOAD_PROGRESS,
            UPLOAD_SUCCESS,
            UPLOAD_ERROR,
            UPLOAD_CANCELLED,
            UPLOAD_CANCEL,

            ENCODING_STARTED,
            ENCODING_SUCCESS,
            ENCODING_ERROR,
            ENCODING_PROGRESS,

            RESIZE_STARTED,
            RESIZE_SUCCESS,
            RESIZE_ERROR
    };

    private final Map<Long, Recording> mUploads = new HashMap<Long, Recording>();

    private PowerManager.WakeLock mWakeLock;
    private WifiManager.WifiLock mWifiLock;

    private ServiceHandler mServiceHandler;
    private UploadHandler mUploadHandler;
    private EncodingHandler mEncodingHandler;

    // notifications
    private NotificationManager nm;
    private RemoteViews mUploadNotificationView;

    private Notification mUploadNotification;
    private LocalBroadcastManager mBroadcastManager;

    private final class ServiceHandler extends Handler {
        public ServiceHandler(Looper looper) {
            super(looper);
        }
        @Override
        public void handleMessage(Message msg) {
            onHandleIntent((Intent)msg.obj);
        }
    }

    private final class UploadHandler extends Handler {
        public UploadHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {
            Recording recording = (Recording) msg.obj;
            if (recording.hasArtwork() && recording.resized_artwork_path == null) {
                post(new ImageResizer(UploadService.this, recording));
            } else {
                if (!recording.encodedFilename().exists()) {
                    mEncodingHandler.post(new Encoder(UploadService.this, recording));
                } else {
                    post(new Uploader((SoundCloudApplication) getApplication(), recording));
                }
            }
        }
    }

    private final class EncodingHandler extends Handler {
        public EncodingHandler(Looper looper) {
            super(looper);
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "upload service started");

        mBroadcastManager = LocalBroadcastManager.getInstance(this);
        mServiceHandler = new ServiceHandler(createLooper("UploadService"));
        mUploadHandler = new UploadHandler(createLooper("Uploader"));
        mEncodingHandler = new EncodingHandler(createLooper("Encoder"));

        mBroadcastManager.registerReceiver(mReceiver, getIntentFilter());
        nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        mWakeLock = ((PowerManager) getSystemService(Context.POWER_SERVICE))
                .newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, TAG);
        mWifiLock = ((WifiManager) getSystemService(Context.WIFI_SERVICE))
                .createWifiLock(Build.VERSION.SDK_INT >= 9 ? 3 /* WIFI_MODE_FULL_HIGH_PERF */ : WifiManager.WIFI_MODE_FULL, TAG);
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "onDestroy()");

        mServiceHandler.getLooper().quit();
        mUploadHandler.getLooper().quit();

        if (isUploading()) {
            Log.w(TAG, "Service being destroyed while still uploading.");
            for (Recording r : mUploads.values()) {
                onCancel(r);
            }
        }
        mBroadcastManager.unregisterReceiver(mReceiver);
        Log.d(TAG, "shutdown complete.");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Message msg = mServiceHandler.obtainMessage();
        msg.arg1 = startId;
        msg.obj = intent;
        mServiceHandler.sendMessage(msg);

        return START_REDELIVER_INTENT;
    }

    @Override public IBinder onBind(Intent intent) {
        return mBinder;
    }

    public Set<Long> getUploadLocalIds() {
        return mUploads.keySet();
    }

    public boolean isUploading() {
        return mUploads.size() > 0 || mUploadHandler.hasMessages(0);
    }

    private final IBinder mBinder = new LocalBinder<UploadService>() {
        public UploadService getService() {
            return UploadService.this;
        }
    };

    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            final Recording recording = intent.getParcelableExtra(EXTRA_RECORDING);

            if (UPLOAD_STARTED.equals(action)) {
                acquireLocks();
                showUploadingNotification(recording);

            } else if (UPLOAD_PROGRESS.equals(action)) {
                notifyProgress(intent.getIntExtra(EXTRA_PROGRESS, 0), R.string.cloud_uploader_event_uploading);
            } else if (UPLOAD_SUCCESS.equals(action) ||
                       UPLOAD_ERROR.equals(action) ||
                       UPLOAD_CANCELLED.equals(action)) {

                // XXX retry on temp. error?

                mUploads.remove(recording.id);
                Notification n = notifyUploadCurrentUploadFinished(recording);
                if (n != null) {
                    nm.cancel(UPLOADING_NOTIFY_ID);
                    nm.notify(UPLOADED_NOTIFY_ID, n);
                } else {
                    nm.cancel(UPLOADING_NOTIFY_ID);
                }
                releaseLocks();

                if (!isUploading()) { // last one switch off the lights
                    Log.d(TAG, "no more uploads, stopping service");
                    stopSelf();
                }
            } else if (RESIZE_STARTED.equals(action)) {
                acquireWakelock();
            } else if (RESIZE_SUCCESS.equals(action)) {
                releaseWakelock();
                queueUpload(recording);
            } else if (RESIZE_ERROR.equals(action)) {
                releaseWakelock();
                // XXX notify
            } else if (ENCODING_STARTED.equals(action)) {

            } else if (ENCODING_PROGRESS.equals(action)) {
                notifyProgress(intent.getIntExtra(EXTRA_PROGRESS, 0), R.string.cloud_uploader_event_encoding);
            } else if (ENCODING_SUCCESS.equals(action)) {
                queueUpload(recording);
            } else if (ENCODING_ERROR.equals(action)) {
                // XXX notify
            }
        }
    };

    private void notifyProgress(int progress, int resId) {
        mUploadNotificationView.setProgressBar(R.id.progress_bar, 100, progress, false);
        mUploadNotificationView.setTextViewText(R.id.percentage, getString(resId, progress));
        nm.notify(UPLOADING_NOTIFY_ID, mUploadNotification);
    }

    private void onHandleIntent(Intent intent) {
        Recording r = intent.getParcelableExtra(EXTRA_RECORDING);
        if (Actions.UPLOAD.equals(intent.getAction())) {
            if (r != null) {
                onUpload(r);
            }
        } else if (Actions.UPLOAD_CANCEL.equals(intent.getAction())) {
            onCancel(r);
        }
    }

    /* package */ void onUpload(Recording recording) {
        // make sure recording is saved before uploading
        if (!recording.isSaved() &&
            (recording = SoundCloudDB.insertRecording(getContentResolver(), recording)) == null) {
            Log.w(TAG, "could not insert "+recording);
        } else {
            recording.status = Recording.Status.UPLOADING;
            recording.updateStatus(getContentResolver());

            queueUpload(recording);
        }
    }

    private void queueUpload(Recording recording) {
        mUploads.put(recording.id, recording);
        Message.obtain(mUploadHandler, 0, recording).sendToTarget();
    }

    /* package */  void onCancel(Recording r) {
        mUploadHandler.removeMessages(0, r);

        if (mUploads.isEmpty()) {
            Log.d(TAG, "onCancel() called without any active uploads");
            stopSelf();
        }  else {
            mBroadcastManager.sendBroadcast(new Intent(UploadService.UPLOAD_CANCEL).putExtra(EXTRA_RECORDING, r));
        }
    }


    private Notification showUploadingNotification(Recording recording) {
        mUploadNotificationView = new RemoteViews(getPackageName(), R.layout.create_service_status_upload);
        mUploadNotificationView.setTextViewText(R.id.message, "");
        mUploadNotificationView.setTextViewText(R.id.percentage, "0");
        mUploadNotificationView.setProgressBar(R.id.progress_bar, 100, 0, true);

        mUploadNotification = SoundRecorderService.createOngoingNotification(
                getString(R.string.cloud_uploader_notification_ticker),
                PendingIntent.getActivity(this, 0, recording.getMonitorIntent(), PendingIntent.FLAG_UPDATE_CURRENT));
        mUploadNotification.contentView = mUploadNotificationView;
        startForeground(UPLOADING_NOTIFY_ID, mUploadNotification);

        return mUploadNotification;
    }

    private Notification notifyUploadCurrentUploadFinished(Recording recording) {
        final CharSequence title;
        final CharSequence message;
        final CharSequence tickerText;
        if (recording.isUploaded()) {
            title  = getString(R.string.cloud_uploader_notification_finished_title);
            message = getString(R.string.cloud_uploader_notification_finished_message, recording.title);
            tickerText = getString(R.string.cloud_uploader_notification_finished_ticker);
        } else if (!recording.isCanceled()) {
            title = getString(R.string.cloud_uploader_notification_error_title);
            message = getString(R.string.cloud_uploader_notification_error_message, recording.title);
            tickerText = getString(R.string.cloud_uploader_notification_error_ticker);
        } else { // upload canceled, don't notify
            return null;
        }

        Intent userTracks = (new Intent(Actions.MY_PROFILE).putExtra("userBrowserTag", UserBrowser.Tab.tracks));
        PendingIntent contentIntent = PendingIntent.getActivity(this, 0, userTracks, PendingIntent.FLAG_UPDATE_CURRENT);
        Notification notification = new Notification(R.drawable.ic_notification_cloud, tickerText, System.currentTimeMillis());
        notification.flags = Notification.DEFAULT_LIGHTS | Notification.FLAG_AUTO_CANCEL;
        notification.setLatestEventInfo(this,title ,message , contentIntent);

        return notification;
    }

    private void acquireLocks() {
        if (Log.isLoggable(TAG, Log.DEBUG)) Log.d(TAG, "acquireLocks");
        acquireWakelock();
        acquireWifilock();
    }

    private void releaseLocks() {
        if (Log.isLoggable(TAG, Log.DEBUG)) Log.d(TAG, "releaseLocks");
        releaseWakelock();
        releaseWifilock();
    }

    private void acquireWifilock() { if (mWifiLock != null && !mWifiLock.isHeld()) mWifiLock.acquire(); }
    private void releaseWifilock() { if (mWifiLock != null && mWifiLock.isHeld()) mWifiLock.release(); }
    private void acquireWakelock() { if (mWakeLock != null && !mWakeLock.isHeld()) mWakeLock.acquire(); }
    private void releaseWakelock() { if (mWakeLock != null && mWakeLock.isHeld()) mWakeLock.release(); }

    public static IntentFilter getIntentFilter() {
        IntentFilter filter = new IntentFilter();
        for (String a : ALL_ACTIONS) {
            filter.addAction(a);
        }
        return filter;
    }

    /* package, for testing*/ Handler getServiceHandler() {
        return mServiceHandler;
    }

    /* package, for testing*/ Handler getUploadHandler() {
        return mUploadHandler;
    }

    /* package, for testing */ WifiManager.WifiLock getWifiLock() {
        return mWifiLock;
    }

    /* package, for testing */ PowerManager.WakeLock getWakeLock() {
        return mWakeLock;
    }

    private static Looper createLooper(String name) {
        HandlerThread thread = new HandlerThread(name);
        thread.start();
        return thread.getLooper();
    }
}
