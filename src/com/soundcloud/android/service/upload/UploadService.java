package com.soundcloud.android.service.upload;

import static com.soundcloud.android.Consts.Notifications.RECORD_NOTIFY_ID;
import static com.soundcloud.android.Consts.Notifications.UPLOADED_NOTIFY_ID;
import static com.soundcloud.android.Consts.Notifications.UPLOADING_NOTIFY_ID;

import com.soundcloud.android.Actions;
import com.soundcloud.android.R;
import com.soundcloud.android.activity.UploadMonitor;
import com.soundcloud.android.activity.UserBrowser;
import com.soundcloud.android.model.Recording;
import com.soundcloud.android.provider.SoundCloudDB;
import com.soundcloud.android.service.LocalBinder;
import com.soundcloud.android.service.record.CloudCreateService;
import com.soundcloud.api.CloudAPI;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
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
    private static final String TAG = UploadService.class.getSimpleName();

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

            RESIZE_STARTED,
            RESIZE_SUCCESS,
            RESIZE_ERROR
    };

    private final Map<Long, Recording> mUploads = new HashMap<Long, Recording>();

    private PowerManager.WakeLock mWakeLock;

    private Looper mServiceLooper;
    private ServiceHandler mServiceHandler;

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
//            stopSelf(msg.arg1);
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "upload service started");

        HandlerThread thread = new HandlerThread("UploadService");
        thread.start();

        mBroadcastManager = LocalBroadcastManager.getInstance(this);
        mServiceLooper = thread.getLooper();
        mServiceHandler = new ServiceHandler(mServiceLooper);
        mBroadcastManager.registerReceiver(mReceiver, getIntentFilter());
        nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        mWakeLock = ((PowerManager) getSystemService(Context.POWER_SERVICE))
                .newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, TAG);
    }

    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            final Recording recording = intent.getParcelableExtra(EXTRA_RECORDING);

            if (UPLOAD_STARTED.equals(action)) {
                acquireWakeLock();
                mUploads.put(recording.id, recording);
                showUploadingNotification(recording);

            } else if (UPLOAD_PROGRESS.equals(action)) {
                int progress = intent.getIntExtra(EXTRA_PROGRESS, 0);
                mUploadNotificationView.setProgressBar(R.id.progress_bar, 100, progress, false);
                mUploadNotificationView.setTextViewText(R.id.percentage, getString(R.string.cloud_uploader_event_uploading, progress));
                nm.notify(UPLOADING_NOTIFY_ID, mUploadNotification);

            } else if (UPLOAD_SUCCESS.equals(action) || UPLOAD_ERROR.equals(action)) {
                mUploads.remove(recording.id);
                notifyUploadCurrentUploadFinished(recording);
                releaseWakeLock();

                if (mUploads.isEmpty()) {
                    Log.d(TAG, "no more uploads, stopping service");
                    stopSelf();
                }
            } else if (RESIZE_STARTED.equals(action)) {
               Log.d(TAG, "resizing started");
                acquireWakeLock();

            } else if (RESIZE_SUCCESS.equals(action)) {
                onUpload(recording);
                releaseWakeLock();
            } else if (RESIZE_ERROR.equals(action)) {
                releaseWakeLock();
            }
        }
    };

    @Override
    public void onDestroy() {
        Log.d(TAG, "onDestroy()");

        mServiceLooper.quit();
        if (!mUploads.isEmpty()) {
            Log.w(TAG, "Service being destroyed while still uploading.");
            // cancel?
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


    private final IBinder mBinder = new LocalBinder<UploadService>() {
        public UploadService getService() {
            return UploadService.this;
        }
    };

    private void onHandleIntent(Intent intent) {
        if (Actions.UPLOAD.equals(intent.getAction())) {
            Recording r = intent.getParcelableExtra(EXTRA_RECORDING);
            if (r != null) {
                onUpload(r);
            }
        }
    }

    void onUpload(Recording recording) {
        // make sure recording is saved before uploading
        if (!recording.isSaved() &&
            (recording = SoundCloudDB.insertRecording(getContentResolver(), recording)) == null) {
            Log.w(TAG, "could not insert "+recording);

            return;
        }
        recording.status = Recording.Status.UPLOADING;
        recording.updateStatus(getContentResolver());

        if (recording.hasArtwork() && recording.resized_artwork_path == null) {
            mServiceHandler.post(new ImageResizer(this, recording));
        } else {
            mServiceHandler.post(new Uploader((CloudAPI) getApplication(), recording));
        }
    }

    public boolean isUploading() {
        return !mUploads.isEmpty();
    }

    public void cancelUploadById(long id) {
        nm.cancel(RECORD_NOTIFY_ID);
        sendBroadcast(new Intent(UPLOAD_CANCEL).putExtra("id", id));
    }


    public void cancelUpload() {
    }

    public Set<Long> getUploadLocalIds() {
        return mUploads.keySet();
    }

    public Recording getUploadById(long id) {
        return mUploads.get(id);
    }

    private Notification showUploadingNotification(Recording recording) {
        Intent monitor = new Intent(this, UploadMonitor.class).putExtra("upload_id", recording.id);

        mUploadNotificationView = new RemoteViews(getPackageName(), R.layout.create_service_status_upload);
        mUploadNotificationView.setTextViewText(R.id.message, "");
        mUploadNotificationView.setTextViewText(R.id.percentage, "0");
        mUploadNotificationView.setProgressBar(R.id.progress_bar, 100, 0, true);

        mUploadNotification = CloudCreateService.createOngoingNotification(
                getString(R.string.cloud_uploader_notification_ticker),
                PendingIntent.getActivity(this, 0, monitor, PendingIntent.FLAG_UPDATE_CURRENT));
        mUploadNotification.contentView = mUploadNotificationView;
        startForeground(UPLOADING_NOTIFY_ID, mUploadNotification);

        return mUploadNotification;
    }

    private void notifyUploadCurrentUploadFinished(Recording upload) {
        nm.cancel(UPLOADING_NOTIFY_ID);

        final CharSequence title;
        final CharSequence message;
        final CharSequence tickerText;

        if (upload.isSuccess()) {
            upload.status = Recording.Status.UPLOADED;
            title  = getString(R.string.cloud_uploader_notification_finished_title);
            message = getString(R.string.cloud_uploader_notification_finished_message, upload.title);
            tickerText = getString(R.string.cloud_uploader_notification_finished_ticker);
            upload.onUploaded();
        } else {
            upload.status = Recording.Status.NOT_YET_UPLOADED;
            title = getString(R.string.cloud_uploader_notification_error_title);
            message = getString(R.string.cloud_uploader_notification_error_message, upload.title);
            tickerText = getString(R.string.cloud_uploader_notification_error_ticker);
        }
        upload.updateStatus(getContentResolver());

        Intent userTracks = (new Intent(Actions.MY_PROFILE).putExtra("userBrowserTag", UserBrowser.Tab.tracks));
        PendingIntent contentIntent = PendingIntent.getActivity(this, 0, userTracks, PendingIntent.FLAG_UPDATE_CURRENT);
        Notification notification = new Notification(R.drawable.ic_status, tickerText, System.currentTimeMillis());
        notification.flags = Notification.DEFAULT_LIGHTS | Notification.FLAG_AUTO_CANCEL;
        notification.setLatestEventInfo(this,title ,message , contentIntent);
        nm.notify(UPLOADED_NOTIFY_ID, notification);
    }


    private void acquireWakeLock() {
        if (mWakeLock != null && !mWakeLock.isHeld()) {
            mWakeLock.acquire();
        }
    }

    private void releaseWakeLock() {
        if (mWakeLock != null && mWakeLock.isHeld()) {
            mWakeLock.release();
        }
    }

    public static IntentFilter getIntentFilter() {
        IntentFilter filter = new IntentFilter();
        for (String a : ALL_ACTIONS) {
            filter.addAction(a);
        }
        return filter;
    }

    /* package, for testing*/ Looper getServiceLooper() {
        return mServiceLooper;
    }
}
