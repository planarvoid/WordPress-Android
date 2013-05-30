package com.soundcloud.android.service.upload;

import static com.soundcloud.android.Consts.Notifications.UPLOADING_NOTIFY_ID;

import com.soundcloud.android.Actions;
import com.soundcloud.android.AndroidCloudAPI;
import com.soundcloud.android.Consts;
import com.soundcloud.android.R;
import com.soundcloud.android.dao.RecordingStorage;
import com.soundcloud.android.model.Recording;
import com.soundcloud.android.model.ScResource;
import com.soundcloud.android.model.Track;
import com.soundcloud.android.provider.Content;
import com.soundcloud.android.record.SoundRecorder;
import com.soundcloud.android.service.LocalBinder;
import com.soundcloud.android.service.record.SoundRecorderService;
import com.soundcloud.android.utils.IOUtils;
import com.soundcloud.android.utils.images.ImageUtils;
import org.jetbrains.annotations.Nullable;

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
import android.os.Process;
import android.support.v4.content.LocalBroadcastManager;
import android.text.TextUtils;
import android.text.format.DateFormat;
import android.text.format.DateUtils;
import android.util.Log;
import android.widget.RemoteViews;

import java.lang.ref.WeakReference;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class UploadService extends Service {
    /* package */ static final String TAG = UploadService.class.getSimpleName();

    public static final int UPLOAD_STAGE_PROCESSING     = 1;
    public static final int UPLOAD_STAGE_TRANSFERRING   = 2;

    public static final String EXTRA_RECORDING   = Recording.EXTRA;
    public static final String EXTRA_TRANSFERRED = "transferred";
    public static final String EXTRA_TOTAL       = "total";
    public static final String EXTRA_PROGRESS    = "progress";
    public static final String EXTRA_STAGE       = "stage";

    public static final String UPLOAD_CANCEL     = "com.soundcloud.android.service.upload.cancel";
    public static final String UPLOAD_SUCCESS    = "com.soundcloud.android.service.upload.success";

    public static final String TRANSFER_STARTED   = "com.soundcloud.android.service.upload.transfer.started";
    public static final String TRANSFER_PROGRESS  = "com.soundcloud.android.service.upload.transfer.progress";
    public static final String TRANSFER_ERROR     = "com.soundcloud.android.service.upload.transfer.error";
    public static final String TRANSFER_CANCELLED = "com.soundcloud.android.service.upload.transfer.cancelled";
    public static final String TRANSFER_SUCCESS   = "com.soundcloud.android.service.upload.transfer.success";

    public static final String PROCESSING_STARTED = "com.soundcloud.android.service.upload.processing.started";
    public static final String PROCESSING_SUCCESS = "com.soundcloud.android.service.upload.processing.success";
    public static final String PROCESSING_ERROR = "com.soundcloud.android.service.upload.processing.error";
    public static final String PROCESSING_CANCELED = "com.soundcloud.android.service.upload.processing.cancelled";
    public static final String PROCESSING_PROGRESS = "com.soundcloud.android.service.upload.processing.progress";

    public static final String RESIZE_STARTED    = "com.soundcloud.android.service.upload.resize.started";
    public static final String RESIZE_SUCCESS    = "com.soundcloud.android.service.upload.resize.success";
    public static final String RESIZE_ERROR      = "com.soundcloud.android.service.upload.resize.error";

    public static final String TRANSCODING_SUCCESS = "com.soundcloud.android.service.upload.transcoding.success";
    public static final String TRANSCODING_FAILED  = "com.soundcloud.android.service.upload.transcoding.failed";

    public static final String[] ALL_ACTIONS = {
        UPLOAD_SUCCESS,
        UPLOAD_CANCEL,

        TRANSFER_STARTED,
        TRANSFER_PROGRESS,
        TRANSFER_ERROR,
        TRANSFER_CANCELLED,
        TRANSFER_SUCCESS,

        PROCESSING_STARTED,
        PROCESSING_CANCELED,
        PROCESSING_SUCCESS,
        PROCESSING_ERROR,
        PROCESSING_PROGRESS,

        RESIZE_STARTED,
        RESIZE_SUCCESS,
        RESIZE_ERROR,

        TRANSCODING_FAILED,
        TRANSCODING_SUCCESS
    };

    private static class Upload {
        final Recording recording;
        Track track;
        Notification notification;

        public Upload(Recording r){
            recording = r;
        }

        @Override
        public String toString() {
            return "Upload{" +
                    "recording=" + recording +
                    ", playbackStream=" + recording.getPlaybackStream() +
                    '}';
        }
    }

    private final Map<Long, Upload> mUploads = new HashMap<Long, Upload>();

    private PowerManager.WakeLock mWakeLock;
    private WifiManager.WifiLock mWifiLock;

    private IntentHandler mIntentHandler;
    private UploadHandler mUploadHandler;
    private Handler mProcessingHandler;

    // notifications
    private NotificationManager nm;
    private LocalBroadcastManager mBroadcastManager;

    private RecordingStorage mRecordingStorage;

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "upload service started");

        mRecordingStorage = new RecordingStorage();
        mBroadcastManager = LocalBroadcastManager.getInstance(this);
        mIntentHandler = new IntentHandler(this, createLooper("UploadService", Process.THREAD_PRIORITY_DEFAULT));
        mUploadHandler = new UploadHandler(this, createLooper("Uploader", Process.THREAD_PRIORITY_DEFAULT));
        mProcessingHandler = new Handler(createLooper("Processing", Process.THREAD_PRIORITY_BACKGROUND));

        mBroadcastManager.registerReceiver(mReceiver, getIntentFilter());
        nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        mWakeLock = ((PowerManager) getSystemService(Context.POWER_SERVICE))
                .newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, TAG);
        mWifiLock = IOUtils.createHiPerfWifiLock(this, TAG);

        mUploadHandler.post(new StuckUploadCheck(this));
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "onDestroy()");

        mIntentHandler.getLooper().quit();
        mUploadHandler.getLooper().quit();
        mProcessingHandler.getLooper().quit();

        if (isUploading()) {
            Log.w(TAG, "Service being destroyed while still uploading.");
            for (Upload u : mUploads.values()) {
                cancel(u.recording);
            }
        }
        mBroadcastManager.unregisterReceiver(mReceiver);
        Log.d(TAG, "shutdown complete.");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        mIntentHandler.obtainMessage(0, startId, 0, intent).sendToTarget();
        return START_REDELIVER_INTENT;
    }


    @Override public IBinder onBind(Intent intent) {
        return mBinder;
    }

    private boolean isUploading() {
        return !mUploads.isEmpty() || mUploadHandler.hasMessages(0);
    }

    /**
     * Handles incoming upload request.
     * Note: success/failure signalling is handled via {@link LocalBroadcastManager}, which also handles
     * the (re-)queuing of tasks to correct queue.
     */
    private static final class UploadHandler extends Handler {
        private WeakReference<UploadService> mServiceRef;

        private UploadHandler(UploadService service, Looper looper) {
            super(looper);
            mServiceRef = new WeakReference<UploadService>(service);
        }

        @Override public void handleMessage(Message msg) {
            final UploadService service = mServiceRef.get();
            if (service == null) {
                return;
            }

            Upload upload = (Upload) msg.obj;

            Log.d(TAG, "handleMessage("+upload+")");

            if (upload.recording.needsResizing()) {
                post(new ImageResizer(service, upload.recording));
            } else if (upload.recording.needsProcessing()) {
                service.mProcessingHandler.post(new Processor(service, upload.recording));
            } else if (upload.recording.needsEncoding()) {
                service.mProcessingHandler.post(new Encoder(service, upload.recording));
            } else {
                // perform the actual upload
                post(new Uploader((AndroidCloudAPI) service.getApplication(), upload.recording));
            }
        }
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

            if (Log.isLoggable(TAG, Log.DEBUG)) Log.d(TAG, "Service received action " + action);
            if (RESIZE_STARTED.equals(action)) {
                acquireWakelock();

            } else if (RESIZE_SUCCESS.equals(action)) {
                releaseWakelock();
                queueUpload(recording);

            } else if (PROCESSING_STARTED.equals(action)) {
                acquireWakelock();
                showUploadingNotification(recording, PROCESSING_STARTED);

            } else if (PROCESSING_PROGRESS.equals(action)) {
                sendNotification(recording,
                        updateProcessingProgress(
                                recording,
                                R.string.uploader_event_processing_percent,
                                intent.getIntExtra(EXTRA_PROGRESS, 0)
                        )
                );

            } else if (PROCESSING_SUCCESS.equals(action)) {
                releaseWakelock();
                queueUpload(recording);

            } else if (TRANSFER_STARTED.equals(action)) {
                showUploadingNotification(recording, TRANSFER_STARTED);
                acquireLocks();

            } else if (TRANSFER_PROGRESS.equals(action)) {
                sendNotification(recording,
                        updateUploadingProgress(
                                recording,
                                R.string.uploader_event_uploading_percent,
                                intent.getIntExtra(EXTRA_PROGRESS, 0)
                        )
                );

            } else if (TRANSFER_SUCCESS.equals(action)) {
                Upload upload = mUploads.get(recording.id);
                if (upload == null) return;
                upload.track = intent.getParcelableExtra(Track.EXTRA);

                new Poller(createLooper("poller_" + upload.track.id, Process.THREAD_PRIORITY_BACKGROUND),
                            (AndroidCloudAPI) getApplication(),
                            upload.track.id,
                            Content.ME_SOUNDS.uri).start();

                mBroadcastManager.sendBroadcast(new Intent(UPLOAD_SUCCESS)
                        .putExtra(UploadService.EXTRA_RECORDING, recording));

                releaseWifilock();
                onUploadDone(recording);

            } else if (TRANSCODING_SUCCESS.equals(action) || TRANSCODING_FAILED.equals(action)) {
                releaseWakelock();
                onTranscodingDone(intent.<Track>getParcelableExtra(Track.EXTRA));
            }


            // error handling
            final boolean wasError = RESIZE_ERROR.equals(action)
                    || PROCESSING_CANCELED.equals(action)
                    || PROCESSING_ERROR.equals(action)
                    || TRANSFER_CANCELLED.equals(action)
                    || TRANSFER_ERROR.equals(action);
            if (wasError) {
                mRecordingStorage
                        .updateStatus(recording.setUploadFailed(PROCESSING_CANCELED.equals(action) || TRANSFER_CANCELLED.equals(action))); // for list state

                releaseLocks();
                mUploads.remove(recording.id);
                onUploadDone(recording);
            }
        }
    };

    private Notification transcodingFailedNotification(Track track) {
        String title = getString(R.string.cloud_uploader_notification_transcoding_error_title);
        String message = getString(R.string.cloud_uploader_notification_transcoding_error_message, track.title);
        String tickerText = getString(R.string.cloud_uploader_notification_transcoding_error_ticker);
        PendingIntent contentIntent = PendingIntent.getActivity(this, 0,
                new Intent(Actions.YOUR_SOUNDS),
                PendingIntent.FLAG_UPDATE_CURRENT);

        Notification notification = new Notification(R.drawable.ic_notification_cloud, tickerText, System.currentTimeMillis());
        notification.flags = Notification.DEFAULT_LIGHTS | Notification.FLAG_AUTO_CANCEL;
        notification.setLatestEventInfo(this, title, message , contentIntent);
        return notification;
    }

    private void onUploadDone(Recording recording) {
        // leave a note
        Notification n = notifyUploadCurrentUploadFinished(recording);
        if (n != null) {
            sendNotification(recording, n);
        } else {
            cancelNotification(recording);
        }

        if (!isUploading()) { // last one switch off the lights
            stopSelf();
            nm.cancel(UPLOADING_NOTIFY_ID);
        }
    }

    private void onTranscodingDone(Track track) {
        releaseLocks();

        if (!track.isFinished()) {
            sendNotification(track, transcodingFailedNotification(track));
        }

        Iterator<Upload> it = mUploads.values().iterator();
        while (it.hasNext()) {
            Upload u = it.next();
            if (track.equals(u.track)) it.remove();
        }

        if (!isUploading()) {
            stopSelf();
        }
    }

    private void sendNotification(ScResource r, Notification n) {
        // ugly way to help uniqueness
        nm.notify(getNotificationId(r), n);
    }

    private void cancelNotification(ScResource r) {
        nm.cancel(getNotificationId(r));
    }

    private int getNotificationId(ScResource r){
        return (int) (9990000 + r.id);
    }

    private Notification updateProcessingProgress(Recording r, int stringId, int progress) {
        final Notification n = getOngoingNotification(r);
        final int positiveProgress = Math.max(0, progress);
        final PendingIntent pendingIntent = PendingIntent.getActivity(this, 0,
                r.getMonitorIntentWithProgress(UPLOAD_STAGE_PROCESSING, positiveProgress), PendingIntent.FLAG_UPDATE_CURRENT);

        if (Consts.SdkSwitches.useCustomNotificationLayouts) {
            n.contentIntent = pendingIntent;
            n.contentView.setTextViewText(R.id.txt_processing, getString(stringId, positiveProgress));
            n.contentView.setProgressBar(R.id.progress_bar_processing, 100, positiveProgress, progress == -1); // just show indeterminate for 0 progress, looks better for quick uploads
        } else {
            n.setLatestEventInfo(this, r.getTitle(getResources()), getString(stringId, positiveProgress), pendingIntent);
        }
        return n;
    }

    private Notification updateUploadingProgress(Recording r, int stringId, int progress) {
        final Notification n = getOngoingNotification(r);
        final int positiveProgress = Math.max(0, progress);
        final PendingIntent pendingIntent = PendingIntent.getActivity(this, 0,
                r.getMonitorIntentWithProgress(UPLOAD_STAGE_TRANSFERRING, positiveProgress), PendingIntent.FLAG_UPDATE_CURRENT);

        if (Consts.SdkSwitches.useCustomNotificationLayouts) {
            n.contentIntent = pendingIntent;
            n.contentView.setTextViewText(R.id.txt_uploading, getString(stringId, positiveProgress));
            n.contentView.setProgressBar(R.id.progress_bar_uploading, 100, positiveProgress, progress == -1);
        } else {
            n.setLatestEventInfo(this, r.getTitle(getResources()), getString(stringId, positiveProgress), pendingIntent);
        }
        return n;
    }


    /* package */ void upload(Recording recording) {

        final SoundRecorder soundRecorder = SoundRecorder.getInstance(getApplicationContext());
        if (soundRecorder.isActive() && soundRecorder.getRecording().equals(recording)){
            soundRecorder.gotoIdleState();
        }

        if (!recording.isSaved()){
            recording = mRecordingStorage.create(recording).toBlockingObservable().last();
        }

        if (recording.isSaved()){
            recording.upload_status = Recording.Status.UPLOADING;
            mRecordingStorage.updateStatus(recording);
        } else {
            Log.w(TAG, "could not create " + recording);
        }
        queueUpload(recording);
    }

    /* package */  void cancel(Recording r) {

        Upload u = mUploads.get(r.id);
        if (u != null) mUploadHandler.removeMessages(0, u);
        if (mUploads.isEmpty()) {
            Log.d(TAG, "onCancel() called without any active uploads");
            mBroadcastManager.sendBroadcast(new Intent(TRANSFER_CANCELLED).putExtra(EXTRA_RECORDING, r)); // send this in case someone is cancelling a stuck upload
            stopSelf();
        } else {
            mBroadcastManager.sendBroadcast(new Intent(UploadService.UPLOAD_CANCEL).putExtra(EXTRA_RECORDING, r));
        }
    }


    private void queueUpload(Recording recording) {
        Upload upload = getUpload(recording);
        Message.obtain(mUploadHandler, 0, upload).sendToTarget();
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
            u.notification = SoundRecorderService.createOngoingNotification(PendingIntent.getActivity(this, 0,
                    recording.getMonitorIntent(),
                    PendingIntent.FLAG_UPDATE_CURRENT));
            u.notification.contentView = new RemoteViews(getPackageName(), R.layout.upload_status);
        }
        return u.notification;
    }


    private void showUploadingNotification(Recording recording, String action) {
        Notification n = getOngoingNotification(recording);

        if (Consts.SdkSwitches.useCustomNotificationLayouts){
            n.contentView.setTextViewText(R.id.time, getFormattedNotificationTimestamp(this, System.currentTimeMillis()));
            n.contentView.setTextViewText(R.id.message, TextUtils.isEmpty(recording.title) ? recording.sharingNote(getResources()) : recording.title);

            if (Consts.SdkSwitches.useRichNotifications && recording.hasArtwork()){
                Bitmap b = ImageUtils.getConfiguredBitmap(recording.artwork_path,
                        (int) getResources().getDimension(R.dimen.notification_image_width),
                        (int) getResources().getDimension(R.dimen.notification_image_height));
                if (b != null){
                    n.contentView.setImageViewBitmap(R.id.icon,b);
                }
            }
            if (PROCESSING_STARTED.equals(action)) {
                updateProcessingProgress(recording, R.string.uploader_event_processing_percent, -1);
                updateUploadingProgress(recording, R.string.uploader_event_not_yet_uploading, 0);
            } else if (TRANSFER_STARTED.equals(action)) {
                updateProcessingProgress(recording, R.string.uploader_event_processing_finished, 100);
                updateUploadingProgress(recording, R.string.uploader_event_uploading_percent, -1);
            }
        } else {
            if (PROCESSING_STARTED.equals(action)) {
                updateProcessingProgress(recording, R.string.uploader_event_processing_percent, -1);
            } else if (TRANSFER_STARTED.equals(action)) {
                updateUploadingProgress(recording, R.string.uploader_event_uploading_percent, -1);
            }
        }

        sendNotification(recording, n);
    }

    private @Nullable Notification notifyUploadCurrentUploadFinished(Recording recording) {
        final CharSequence title;
        final CharSequence message;
        final CharSequence tickerText;

        PendingIntent contentIntent;
        if (recording.isUploaded()) {
            title  = getString(R.string.cloud_uploader_notification_finished_title);
            message = getString(R.string.cloud_uploader_notification_finished_message, recording.title);
            tickerText = getString(R.string.cloud_uploader_notification_finished_ticker);
            contentIntent = PendingIntent.getActivity(this, 0,
                    new Intent(Actions.YOUR_SOUNDS),PendingIntent.FLAG_UPDATE_CURRENT);

        } else if (recording.isError()) {
            title = getString(R.string.cloud_uploader_notification_error_title);
            message = getString(R.string.cloud_uploader_notification_error_message, recording.title); // XXX could be null
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
        return mIntentHandler;
    }

    /* package, for testing*/ Handler getUploadHandler() {
        return mUploadHandler;
    }

    /* package, for testing*/ Handler getProcessingHandler() {
        return mProcessingHandler;
    }


    /* package, for testing */ WifiManager.WifiLock getWifiLock() {
        return mWifiLock;
    }

    /* package, for testing */ PowerManager.WakeLock getWakeLock() {
        return mWakeLock;
    }

    private static Looper createLooper(String name, int prio) {
        HandlerThread thread = new HandlerThread(name, prio);
        thread.start();
        return thread.getLooper();
    }

    private static CharSequence getFormattedNotificationTimestamp(Context context, long when) {
        final Date date = new Date(when);
        return DateUtils.isToday(when) ? DateFormat.getTimeFormat(context).format(date)
                : DateFormat.getDateFormat(context).format(date);
    }

    private static final class IntentHandler extends Handler {
        private WeakReference<UploadService> mServiceRef;

        public IntentHandler(UploadService service, Looper looper) {
            super(looper);
            mServiceRef = new WeakReference<UploadService>(service);
        }
        @Override
        public void handleMessage(Message msg) {
            final UploadService service = mServiceRef.get();
            final Intent intent = (Intent) msg.obj;
            final Recording r = intent.getParcelableExtra(EXTRA_RECORDING);
            if (service != null && r != null) {
                if (Actions.UPLOAD.equals(intent.getAction())) {
                    service.upload(r);
                } else if (Actions.UPLOAD_CANCEL.equals(intent.getAction())) {
                    service.cancel(r);
                }
            }
        }
    }
}
