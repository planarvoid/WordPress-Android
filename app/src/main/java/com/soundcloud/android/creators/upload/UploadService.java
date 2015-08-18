package com.soundcloud.android.creators.upload;

import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.api.ApiClient;
import com.soundcloud.android.api.legacy.model.PublicApiTrack;
import com.soundcloud.android.api.legacy.model.Recording;
import com.soundcloud.android.commands.StoreTracksCommand;
import com.soundcloud.android.creators.record.SoundRecorder;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.UploadEvent;
import com.soundcloud.android.rx.observers.DefaultSubscriber;
import com.soundcloud.android.service.LocalBinder;
import com.soundcloud.android.sync.posts.StorePostsCommand;
import com.soundcloud.android.utils.IOUtils;
import com.soundcloud.rx.eventbus.EventBus;
import rx.Subscription;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.net.wifi.WifiManager;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.PowerManager;
import android.os.Process;
import android.support.annotation.VisibleForTesting;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import javax.inject.Inject;
import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.Map;

public class UploadService extends Service {
    public static final int UPLOAD_STAGE_PROCESSING = 1;
    public static final int UPLOAD_STAGE_TRANSFERRING = 2;

    public static final String EXTRA_RECORDING = Recording.EXTRA;
    public static final String EXTRA_PROGRESS = "progress";
    public static final String EXTRA_STAGE = "stage";

    static final String TAG = UploadService.class.getSimpleName();

    private final Map<Long, Upload> uploads = new HashMap<>();
    private final IBinder binder = new LocalBinder<UploadService>() {
        public UploadService getService() {
            return UploadService.this;
        }
    };
    private PowerManager.WakeLock wakeLock;
    private WifiManager.WifiLock wifiLock;
    private UploadHandler uploadHandler;
    private Handler processingHandler;

    @Inject UploadNotificationController notificationController;
    @Inject StoreTracksCommand storeTracksCommand;
    @Inject StorePostsCommand storePostsCommand;
    @Inject ApiClient apiClient;
    @Inject EventBus eventBus;
    private Subscription subscription;

    public UploadService() {
        SoundCloudApplication.getObjectGraph().inject(this);
    }

    @VisibleForTesting
    public UploadService(StorePostsCommand storePostsCommand, EventBus eventBus) {
        this.storePostsCommand = storePostsCommand;
        this.eventBus = eventBus;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "upload service started");
        uploadHandler = new UploadHandler(this, createLooper("Uploader", Process.THREAD_PRIORITY_DEFAULT),
                apiClient, storeTracksCommand, storePostsCommand, eventBus);
        processingHandler = new Handler(createLooper("Processing", Process.THREAD_PRIORITY_BACKGROUND));

        wakeLock = ((PowerManager) getSystemService(Context.POWER_SERVICE))
                .newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, TAG);
        wifiLock = IOUtils.createHiPerfWifiLock(this, TAG);

        subscription = eventBus.subscribe(EventQueue.UPLOAD, new EventSubscriber());
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "onDestroy()");

        uploadHandler.getLooper().quit();
        processingHandler.getLooper().quit();
        subscription.unsubscribe();

        if (isUploading()) {
            Log.w(TAG, "Service being destroyed while still uploading.");
            for (Upload u : uploads.values()) {
                cancel(u.recording);
            }
        }

        Log.d(TAG, "shutdown complete.");
    }

    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    public boolean isUploading() {
        return !uploads.isEmpty() || uploadHandler.hasMessages(0);
    }

    private void onUploadDone(Recording recording) {
        if (recording.isUploaded()) {
            notificationController.showUploadFinished(recording);
        } else if (recording.isError()) {
            notificationController.showUploadError(recording);
        } else {
            notificationController.onUploadCancelled();
        }

        uploads.remove(recording.getId());

        if (!isUploading()) { // last one switch off the lights
            stopSelf();
        }
    }

    private void queueUpload(Recording recording) {
        notificationController.showProcessingNotification(recording);

        Upload upload = getUpload(recording);
        Message.obtain(uploadHandler, 0, upload).sendToTarget();
    }

    private Upload getUpload(Recording r) {
        if (!uploads.containsKey(r.getId())) {
            uploads.put(r.getId(), new Upload(r));
        }
        return uploads.get(r.getId());
    }

    private void acquireLocks() {
        if (Log.isLoggable(TAG, Log.DEBUG)) {
            Log.d(TAG, "acquireLocks");
        }
        acquireWakelock();
        acquireWifilock();
    }

    private void releaseLocks() {
        if (Log.isLoggable(TAG, Log.DEBUG)) {
            Log.d(TAG, "releaseLocks");
        }
        releaseWakelock();
        releaseWifilock();
    }

    private void acquireWifilock() {
        if (wifiLock != null && !wifiLock.isHeld()) {
            wifiLock.acquire();
        }
    }

    private void releaseWifilock() {
        if (wifiLock != null && wifiLock.isHeld()) {
            wifiLock.release();
        }
    }

    private void acquireWakelock() {
        if (wakeLock != null && !wakeLock.isHeld()) {
            wakeLock.acquire();
        }
    }

    private void releaseWakelock() {
        if (wakeLock != null && wakeLock.isHeld()) {
            wakeLock.release();
        }
    }

    private static Looper createLooper(String name, int prio) {
        HandlerThread thread = new HandlerThread(name, prio);
        thread.start();
        return thread.getLooper();
    }

    public void upload(Recording recording) {
        final SoundRecorder soundRecorder = SoundRecorder.getInstance(getApplicationContext());
        if (soundRecorder.isActive() && soundRecorder.getRecording().equals(recording)) {
            soundRecorder.gotoIdleState();
        }

        recording.setUploading();
        queueUpload(recording);
    }

    void cancel(Recording recording) {
        Upload u = uploads.get(recording.getId());
        if (u != null) {
            uploadHandler.removeMessages(0, u);
        }

        eventBus.publish(EventQueue.UPLOAD, UploadEvent.cancelled(recording));

        if (uploads.isEmpty()) {
            Log.d(TAG, "onCancel() called without any active uploads");
            stopSelf();
        }
    }

    @VisibleForTesting
    /* package */ Handler getUploadHandler() {
        return uploadHandler;
    }

    @VisibleForTesting
    /* package */ Handler getProcessingHandler() {
        return processingHandler;
    }

    @VisibleForTesting
    /* package */ WifiManager.WifiLock getWifiLock() {
        return wifiLock;
    }

    @VisibleForTesting
    /* package */ PowerManager.WakeLock getWakeLock() {
        return wakeLock;
    }

    private static class Upload {
        final Recording recording;
        PublicApiTrack track;

        public Upload(Recording r) {
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

    /**
     * Handles incoming upload request.
     * Note: success/failure signalling is handled via {@link LocalBroadcastManager}, which also handles
     * the (re-)queuing of tasks to correct queue.
     */
    private static final class UploadHandler extends Handler {
        private final WeakReference<UploadService> serviceRef;
        private final ApiClient apiClient;
        private final EventBus eventBus;
        private final StoreTracksCommand storeTracksCommand;
        private final StorePostsCommand storePostsCommand;

        private UploadHandler(UploadService service, Looper looper, ApiClient apiClient, StoreTracksCommand storeTracksCommand,
                              StorePostsCommand storePostsCommand, EventBus eventBus) {
            super(looper);
            this.apiClient = apiClient;
            this.storeTracksCommand = storeTracksCommand;
            this.storePostsCommand = storePostsCommand;
            this.eventBus = eventBus;
            serviceRef = new WeakReference<>(service);
        }

        @Override
        public void handleMessage(Message msg) {
            final UploadService service = serviceRef.get();
            if (service == null) {
                return;
            }

            Upload upload = (Upload) msg.obj;

            Log.d(TAG, "handleMessage(" + upload + ")");

            if (upload.recording.needsResizing()) {
                service.processingHandler.post(new ImageResizer(upload.recording));
            } else if (upload.recording.needsProcessing()) {
                service.processingHandler.post(new Processor(upload.recording));
            } else if (upload.recording.needsEncoding()) {
                service.processingHandler.post(new Encoder(upload.recording, eventBus));
            } else {
                // perform the actual upload
                post(new Uploader(service, apiClient, upload.recording, storeTracksCommand, storePostsCommand, eventBus));
            }
        }
    }

    private final class EventSubscriber extends DefaultSubscriber<UploadEvent> {
        @Override
        public void onNext(UploadEvent event) {
            Recording recording = event.getRecording();

            if (event.isStarted()) {
                queueUpload(recording);
            } else if (event.isError() || event.isCancelled()) {
                recording.setUploadFailed(event.isCancelled());
                releaseLocks();
                uploads.remove(recording.getId());
                onUploadDone(recording);
            } else if (event.isResizeStarted() || event.isProcessingStarted()) {
                acquireWakelock();
            } else if (event.isResizeSuccess() || event.isProcessingSuccess()) {
                releaseWakelock();
                queueUpload(recording);
            } else if (event.isTransferStarted()) {
                notificationController.showTransferringNotification(recording, 0);
                acquireLocks();
            } else if (event.isTransferProgress()) {
                notificationController.showTransferringNotification(recording, event.getProgress());
            } else if (event.isTransferSuccess()) {
                Upload upload = uploads.get(recording.getId());

                if (upload != null) {
                    upload.track = event.getTrack();
                    SoundRecorder.getInstance(getApplicationContext()).reset(true);
                    releaseLocks();
                    onUploadDone(recording);
                    eventBus.publish(EventQueue.UPLOAD, UploadEvent.success(recording));
                }
            }
        }
    }
}
