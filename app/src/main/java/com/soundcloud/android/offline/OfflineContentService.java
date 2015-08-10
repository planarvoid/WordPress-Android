package com.soundcloud.android.offline;

import static com.soundcloud.android.NotificationConstants.OFFLINE_NOTIFY_ID;
import static com.soundcloud.android.rx.observers.DefaultSubscriber.fireAndForget;

import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.rx.RxUtils;
import com.soundcloud.android.rx.observers.DefaultSubscriber;
import com.soundcloud.android.utils.Log;
import com.soundcloud.java.collections.MoreCollections;
import com.soundcloud.java.functions.Predicate;
import org.jetbrains.annotations.Nullable;
import rx.Observable;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Func1;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.os.Message;
import android.support.annotation.VisibleForTesting;

import javax.inject.Inject;
import java.util.Collection;
import java.util.List;

public class OfflineContentService extends Service implements DownloadHandler.Listener {

    public static final String TAG = "OfflineContent";
    @VisibleForTesting static final String ACTION_START = "action_start_download";
    @VisibleForTesting static final String ACTION_STOP = "action_stop_download";

    @Inject DownloadOperations downloadOperations;
    @Inject OfflineContentOperations offlineContentOperations;
    @Inject DownloadNotificationController notificationController;
    @Inject OfflineContentScheduler offlineContentScheduler;
    @Inject OfflineStatePublisher publisher;

    @Inject DownloadQueue queue;
    @Inject DownloadHandler.Builder builder;

    private DownloadHandler downloadHandler;
    private Subscription subscription = RxUtils.invalidSubscription();
    private boolean isStopping;

    private final Func1<List<Urn>, Observable<Collection<Urn>>> removeTracks = new Func1<List<Urn>, Observable<Collection<Urn>>>() {
        @Override
        public Observable<Collection<Urn>> call(List<Urn> urns) {
            // TODO : offline content operation responsibility (?)
            return downloadOperations.removeOfflineTracks(urns);
        }
    };

    private final Predicate<DownloadRequest> isNotCurrentDownloadFilter = new Predicate<DownloadRequest>() {
        @Override
        public boolean apply(DownloadRequest request) {
            return !downloadHandler.isCurrentRequest(request);
        }
    };

    public static void start(Context context) {
        context.startService(createIntent(context, ACTION_START));
    }

    public static void stop(Context context) {
        context.startService(createIntent(context, ACTION_STOP));
    }

    private static Intent createIntent(Context context, String action) {
        final Intent intent = new Intent(context, OfflineContentService.class);
        intent.setAction(action);
        return intent;
    }

    public OfflineContentService() {
        SoundCloudApplication.getObjectGraph().inject(this);
    }

    @VisibleForTesting
    OfflineContentService(DownloadOperations downloadOps,
                          OfflineContentOperations offlineContentOperations,
                          DownloadNotificationController notificationController,
                          OfflineContentScheduler offlineContentScheduler,
                          DownloadHandler.Builder builder,
                          OfflineStatePublisher publisher,
                          DownloadQueue queue) {
        this.downloadOperations = downloadOps;
        this.offlineContentOperations = offlineContentOperations;
        this.notificationController = notificationController;
        this.offlineContentScheduler = offlineContentScheduler;
        this.publisher = publisher;
        this.builder = builder;
        this.queue = queue;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        downloadHandler = builder.create(this);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        final String action = intent.getAction();
        isStopping = ACTION_STOP.equals(intent.getAction());

        Log.d(TAG, " Starting offlineContentService for action: " + action);
        offlineContentScheduler.cancelPendingRetries();

        if (ACTION_START.equalsIgnoreCase(action)) {
            fireAndForget(offlineContentOperations.loadContentToDelete().flatMap(removeTracks));

            subscription.unsubscribe();
            subscription = offlineContentOperations
                    .loadOfflineContentUpdates()
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(new OfflineContentRequestsSubscriber());

        } else if (ACTION_STOP.equalsIgnoreCase(action)) {
            if (downloadHandler.isDownloading()) {
                downloadHandler.cancel();
            } else {
                stop();
            }
        }
        return START_NOT_STICKY;
    }

    @Override
    public void onSuccess(DownloadState state) {
        Log.d(TAG, "Download finished " + state);

        notificationController.onDownloadSuccess(state);
        publisher.publishDownloadSuccessfulEvents(queue, state);
        downloadNextOrFinish(state);
    }

    @Override
    public void onError(DownloadState state) {
        Log.d(TAG, "Download failed " + state);

        notificationController.onDownloadError(state);
        publisher.publishDownloadErrorEvents(queue, state);

        if (state.isConnectionError()) {
            stopAndRetryLater();
            notificationController.onConnectionError(state);
        } else {
            downloadNextOrFinish(state);
        }
    }

    @Override
    public void onProgress(DownloadState state) {
        notificationController.onDownloadProgress(state);
    }


    @Override
    public void onCancel(DownloadState state) {
        Log.d(TAG, "Download cancelled " + state);

        notificationController.onDownloadCancel(state);
        publisher.publishDownloadCancelEvents(queue, state);

        if (isStopping) {
            stop();
        } else {
            Log.d(TAG, "Single track download cancelled.. work continues");
            downloadNextOrFinish(state);
        }
    }

    private void downloadNextOrFinish(@Nullable DownloadState result) {
        if (queue.isEmpty()) {
            stopAndFinish(result);
        } else {
            download(queue.poll());
        }
    }

    private void stopAndFinish(DownloadState result) {
        stop();
        notificationController.onDownloadsFinished(result);
    }

    private void stopAndRetryLater() {
        offlineContentScheduler.scheduleRetry();
        stop();
    }

    private void download(DownloadRequest request) {
        Log.d(TAG, "Download started " + request);

        final Message message = downloadHandler.obtainMessage(DownloadHandler.ACTION_DOWNLOAD, request);
        downloadHandler.sendMessage(message);
        publisher.publishDownloading(request);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        subscription.unsubscribe();
        super.onDestroy();
    }

    private void stop() {
        Log.d(TAG, "Stopping the service");
        publisher.publishDone();
        subscription.unsubscribe();
        downloadHandler.quit();

        stopForeground(false);
        stopSelf();
    }

    private class OfflineContentRequestsSubscriber extends DefaultSubscriber<OfflineContentUpdates> {
        @Override
        public void onNext(OfflineContentUpdates requests) {
            Log.d(OfflineContentService.TAG, "Received OfflineContentRequests: " + requests);
            publisher.publishNotDownloadableStateChanges(queue, requests, downloadHandler.getCurrentTrack());

            queue.set(MoreCollections.filter(requests.allDownloadRequests, isNotCurrentDownloadFilter));
            publisher.publishDownloadsRequested(queue);

            updateNotification();

            if (isRemovedTrackCurrentlyBeingDownloaded(requests)) {
                Log.d(OfflineContentService.TAG, "About to cancel download!");
                // download cancelled event is sent in the callback.
                downloadHandler.cancel();
            } else {
                startDownloadIfNecessary();
            }
        }
    }

    private boolean isRemovedTrackCurrentlyBeingDownloaded(OfflineContentUpdates requests) {
        return requests.newRemovedTracks.contains(downloadHandler.getCurrentTrack());
    }

    private void startDownloadIfNecessary() {
        if (!downloadHandler.isDownloading()) {
            downloadNextOrFinish(null);
        } else {
            publisher.publishDownloading(downloadHandler.getCurrentRequest());
        }
    }

    private void updateNotification() {
        if (!queue.isEmpty()) {
            startForeground(OFFLINE_NOTIFY_ID, notificationController.onPendingRequests(queue));
        }
    }
}
