package com.soundcloud.android.service.upload;

import static com.soundcloud.android.Consts.Notifications.UPLOADING_NOTIFY_ID;

import com.soundcloud.android.Actions;
import com.soundcloud.android.Consts;
import com.soundcloud.android.R;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.activity.UserBrowser;
import com.soundcloud.android.model.Recording;
import com.soundcloud.android.model.Track;
import com.soundcloud.android.provider.Content;
import com.soundcloud.android.provider.SoundCloudDB;
import com.soundcloud.android.service.LocalBinder;
import com.soundcloud.android.service.record.SoundRecorderService;
import com.soundcloud.android.utils.IOUtils;
import com.soundcloud.android.utils.ImageUtils;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.net.wifi.WifiManager;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.PowerManager;
import android.support.v4.content.LocalBroadcastManager;
import android.text.TextUtils;
import android.text.format.DateUtils;
import android.util.Log;
import android.widget.RemoteViews;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class UploadService extends Service {
    /* package */ static final String TAG = UploadService.class.getSimpleName();

    public static final String EXTRA_RECORDING   = "recording";
    public static final String EXTRA_TRANSFERRED = "transferred";
    public static final String EXTRA_TOTAL       = "total";
    public static final String EXTRA_PROGRESS    = "progress";

    public static final String UPLOAD_CANCEL     = "com.soundcloud.android.service.upload.cancel";
    public static final String UPLOAD_SUCCESS    = "com.soundcloud.android.service.upload.success";

    public static final String TRANSFER_STARTED   = "com.soundcloud.android.service.upload.transfer.started";
    public static final String TRANSFER_PROGRESS  = "com.soundcloud.android.service.upload.transfer.progress";
    public static final String TRANSFER_ERROR     = "com.soundcloud.android.service.upload.transfer.error";
    public static final String TRANSFER_CANCELLED = "com.soundcloud.android.service.upload.transfer.cancelled";
    public static final String TRANSFER_SUCCESS   = "com.soundcloud.android.service.upload.transfer.success";

    public static final String PROCESSING_STARTED = "com.soundcloud.android.service.upload.processing_started";
    public static final String PROCESSING_SUCCESS = "com.soundcloud.android.service.upload.processing_success";
    public static final String PROCESSING_ERROR = "com.soundcloud.android.service.upload.processing_error";
    public static final String PROCESSING_CANCELED = "com.soundcloud.android.service.upload.processing_cancelled";
    public static final String PROCESSING_PROGRESS = "com.soundcloud.android.service.upload.processing_progress";

    public static final String RESIZE_STARTED    = "com.soundcloud.android.service.upload.resize_started";
    public static final String RESIZE_SUCCESS    = "com.soundcloud.android.service.upload.resize_success";
    public static final String RESIZE_ERROR      = "com.soundcloud.android.service.upload.resize_error";

    public static final String[] ALL_ACTIONS = {
            UPLOAD_SUCCESS,
            UPLOAD_CANCEL,

            TRANSFER_STARTED,
            TRANSFER_PROGRESS,
            TRANSFER_ERROR,
            TRANSFER_CANCELLED,
            TRANSFER_SUCCESS,

            PROCESSING_STARTED,
            PROCESSING_SUCCESS,
            PROCESSING_ERROR,
            PROCESSING_PROGRESS,

            RESIZE_STARTED,
            RESIZE_SUCCESS,
            RESIZE_ERROR
    };

    private static class Upload {
        Recording recording;
        Notification notification;
        public Upload(Recording r){
            recording = r;
        }
    }

    private final Map<Long, Upload> mUploads = new HashMap<Long, Upload>();

    private PowerManager.WakeLock mWakeLock;
    private WifiManager.WifiLock mWifiLock;

    private ServiceHandler mServiceHandler;
    private UploadHandler mUploadHandler;
    private ProcessingHandler mProcessingHandler;

    // notifications
    private NotificationManager nm;
    private LocalBroadcastManager mBroadcastManager;

    private final class ServiceHandler extends Handler {
        public ServiceHandler(Looper looper) {
            super(looper);
        }
        @Override
        public void handleMessage(Message msg) {
            onHandleIntent((Intent) msg.obj);
        }
    }

    private final class UploadHandler extends Handler {
        public UploadHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {
            Recording recording = ((Upload) msg.obj).recording;
            if (recording.hasArtwork() && recording.resized_artwork_path == null) {
                post(new ImageResizer(UploadService.this, recording));
            } else {
                if (!recording.getEncodedFile().exists()) {
                    mProcessingHandler.post(new Encoder(UploadService.this, recording));
                } else {
                    post(new Uploader((SoundCloudApplication) getApplication(), recording));
                }
            }
        }
    }

    private final class ProcessingHandler extends Handler {
        public ProcessingHandler(Looper looper) {
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
        mProcessingHandler = new ProcessingHandler(createLooper("Encoder"));

        mBroadcastManager.registerReceiver(mReceiver, getIntentFilter());
        nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        mWakeLock = ((PowerManager) getSystemService(Context.POWER_SERVICE))
                .newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, TAG);
        mWifiLock = IOUtils.createHiPerfWifiLock(this, TAG);
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "onDestroy()");

        // ensure notification is gone
        nm.cancel(UPLOADING_NOTIFY_ID);

        mServiceHandler.getLooper().quit();
        mUploadHandler.getLooper().quit();

        if (isUploading()) {
            Log.w(TAG, "Service being destroyed while still uploading.");
            for (Upload u : mUploads.values()) {
                onCancel(u);
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
        return !mUploads.isEmpty() || mUploadHandler.hasMessages(0);
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

            if (RESIZE_STARTED.equals(action)) {
                acquireWakelock();
                // XXX notify

            } else if (RESIZE_SUCCESS.equals(action)) {
                releaseWakelock();
                queueUpload(mUploads.get(recording.id));

            } else if (RESIZE_ERROR.equals(action)) {
                releaseWakelock();

            } else if (PROCESSING_STARTED.equals(action)) {
                showUploadingNotification(recording, PROCESSING_STARTED);

            } else if (PROCESSING_PROGRESS.equals(action)) {
                sendNotification(recording,
                        updateProcessingProgress(
                                getOngoingNotification(recording),
                                R.string.uploader_event_processing,
                                intent.getIntExtra(EXTRA_PROGRESS, 0)
                        )
                );

            } else if (PROCESSING_SUCCESS.equals(action)) {
                queueUpload(mUploads.get(recording.id));

            } else if (PROCESSING_ERROR.equals(action)) {
                uploadDone(recording);

            } else if (TRANSFER_STARTED.equals(action)) {
                showUploadingNotification(recording, TRANSFER_STARTED);
                acquireLocks();

            } else if (TRANSFER_PROGRESS.equals(action)) {
                sendNotification(recording,
                        updateUploadingProgress(
                                getOngoingNotification(recording),
                                R.string.uploader_event_uploading,
                                intent.getIntExtra(EXTRA_PROGRESS, 0)
                        )
                );

            } else if (TRANSFER_SUCCESS.equals(action) ||
                    TRANSFER_ERROR.equals(action) ||
                    TRANSFER_CANCELLED.equals(action)) {

                if (TRANSFER_SUCCESS.equals(action)) {
                    final long track_id = recording.track_id;
                    if (track_id != Track.NOT_SET) {
                        new Poller(createLooper("poller_" + track_id),
                                (SoundCloudApplication) getApplication(),
                                track_id,
                                Content.ME_TRACKS.uri).start();
                    }

                    mBroadcastManager.sendBroadcast(new Intent(UPLOAD_SUCCESS)
                            .putExtra(UploadService.EXTRA_RECORDING, recording));
                }

                // XXX retry on temp. error?
                uploadDone(recording);

            }
        }
    };

    private void uploadDone(Recording recording) {

        mUploads.remove(recording.id);
        releaseLocks();

        // leave a note
        Notification n = notifyUploadCurrentUploadFinished(recording);
        if (n != null) {
            sendNotification(recording,n);
        }

        if (!isUploading()) { // last one switch off the lights
            stopSelf();
        }
    }

    private void sendNotification(Recording r, Notification n){
        // ugly way to help uniqueness
        nm.notify((int) (9990000 + r.id), n);
    }

    private Notification updateProcessingProgress(Notification n, int stringId, int progress) {
        final int positiveProgress = Math.max(0, progress);
        n.contentView.setTextViewText(R.id.txt_processing, getString(stringId, positiveProgress));
        n.contentView.setProgressBar(R.id.progress_bar_processing, 100, positiveProgress, progress == -1); // just show indeterminate for 0 progress, looks better for quick uploads
        return n;
    }

    private Notification updateUploadingProgress(Notification n, int stringId, int progress) {
        final int positiveProgress = Math.max(0, progress);
        n.contentView.setTextViewText(R.id.txt_uploading, getString(stringId, positiveProgress));
        n.contentView.setProgressBar(R.id.progress_bar_uploading, 100, positiveProgress, progress == -1);
        return n;
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
            recording.upload_status = Recording.Status.UPLOADING;
            recording.updateStatus(getContentResolver());
            queueUpload(getUpload(recording));
        }
    }

    private void queueUpload(Upload upload) {
        mUploads.put(upload.recording.id, upload);
        Message.obtain(mUploadHandler, 0, upload).sendToTarget();
    }

    /* package */  void onCancel(Recording r) {
        onCancel(getUpload(r));
    }

    /* package */  void onCancel(Upload u) {
        mUploadHandler.removeMessages(0, u);

        if (mUploads.isEmpty()) {
            Log.d(TAG, "onCancel() called without any active uploads");
            stopSelf();
        }  else {
            mBroadcastManager.sendBroadcast(new Intent(UploadService.UPLOAD_CANCEL).putExtra(EXTRA_RECORDING, u.recording));
        }
    }

    private Upload getUpload(Recording r){
        if (!mUploads.containsKey(r.id)){
            mUploads.put(r.id, new Upload(r));
        }
        return mUploads.get(r.id);
    }

    private Notification getOngoingNotification(Recording recording){
        final Upload u = getUpload(recording);
        if (u.notification == null) {
            u.notification = SoundRecorderService.createOngoingNotification(PendingIntent.getActivity(this, 0, recording.getMonitorIntent(), PendingIntent.FLAG_UPDATE_CURRENT));
            u.notification.contentView = new RemoteViews(getPackageName(), R.layout.upload_status);
        }
        return u.notification;
    }


    private void showUploadingNotification(Recording recording, String action) {
        Notification n = getOngoingNotification(recording);
        n.contentView.setTextViewText(R.id.time, getFormattedNotificationTimestamp(this, System.currentTimeMillis()));
        n.contentView.setTextViewText(R.id.message, TextUtils.isEmpty(recording.title) ? recording.sharingNote(getResources()) : recording.title);

        if (PROCESSING_STARTED.equals(action)) {
            updateProcessingProgress(n, R.string.uploader_event_processing_percent, -1);
            updateProcessingProgress(n, R.string.uploader_event_not_yet_uploading, 0);
        } else if (TRANSFER_STARTED.equals(action)) {
            updateProcessingProgress(n, R.string.uploader_event_processing_finished, 100);
            updateUploadingProgress(n, R.string.uploader_event_uploading_percent, -1);
        }

        if (Consts.SdkSwitches.useRichNotifications && recording.hasArtwork()){
            Bitmap b = ImageUtils.getConfiguredBitmap(recording.artwork_path,
                    (int) getResources().getDimension(R.dimen.notification_image_width),
                    (int) getResources().getDimension(R.dimen.notification_image_height));
            if (b != null){
                n.contentView.setImageViewBitmap(R.id.icon,b);
            }
        }

        sendNotification(recording,n);
    }

    private Notification notifyUploadCurrentUploadFinished(Recording recording) {
        final CharSequence title;
        final CharSequence message;
        final CharSequence tickerText;

        PendingIntent contentIntent;
        if (recording.isUploaded()) {
            title  = getString(R.string.cloud_uploader_notification_finished_title);
            message = getString(R.string.cloud_uploader_notification_finished_message, recording.title);
            tickerText = getString(R.string.cloud_uploader_notification_finished_ticker);
            contentIntent = PendingIntent.getActivity(this, 0,
                    new Intent(Actions.MY_PROFILE).putExtra("userBrowserTag", UserBrowser.Tab.tracks),
                    PendingIntent.FLAG_UPDATE_CURRENT);

        } else if (!recording.isCanceled()) {
            title = getString(R.string.cloud_uploader_notification_error_title);
            message = getString(R.string.cloud_uploader_notification_error_message, recording.title);
            tickerText = getString(R.string.cloud_uploader_notification_error_ticker);
            contentIntent = PendingIntent.getActivity(this, 0, recording.getMonitorIntent(), PendingIntent.FLAG_UPDATE_CURRENT);

        } else { // upload canceled, don't notify
            return null;
        }

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

    static Looper createLooper(String name) {
        HandlerThread thread = new HandlerThread(name);
        thread.start();
        return thread.getLooper();
    }

    private static CharSequence getFormattedNotificationTimestamp(Context context, long when) {
        final Date date = new Date(when);
        return DateUtils.isToday(when) ? android.text.format.DateFormat.getTimeFormat(context).format(date)
                : android.text.format.DateFormat.getDateFormat(context).format(date);
    }
}
