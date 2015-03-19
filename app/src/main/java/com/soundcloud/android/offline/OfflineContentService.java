package com.soundcloud.android.offline;

import static com.soundcloud.android.NotificationConstants.OFFLINE_NOTIFY_ID;
import static com.soundcloud.android.rx.observers.DefaultSubscriber.fireAndForget;
import static com.soundcloud.android.utils.CollectionUtils.subtract;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Function;
import com.google.common.collect.Collections2;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.events.CurrentDownloadEvent;
import com.soundcloud.android.events.EntityStateChangedEvent;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.rx.eventbus.EventBus;
import com.soundcloud.android.rx.observers.DefaultSubscriber;
import com.soundcloud.android.utils.Log;
import rx.Scheduler;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.subscriptions.Subscriptions;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.os.Message;

import javax.inject.Inject;
import javax.inject.Named;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

public class OfflineContentService extends Service implements DownloadHandler.Listener {

    static final String TAG = "OfflineContent";
    @VisibleForTesting static final String ACTION_START_DOWNLOAD = "action_start_download";
    @VisibleForTesting static final String ACTION_STOP_DOWNLOAD = "action_stop_download";

    @Inject DownloadOperations downloadOperations;
    @Inject OfflineContentOperations offlineContentOperations;
    @Inject DownloadNotificationController notificationController;
    @Inject EventBus eventBus;
    @Inject OfflineContentScheduler offlineContentScheduler;
    @Inject DownloadHandler.Builder builder;
    @Inject @Named("Storage") Scheduler scheduler;

    private final Queue<DownloadRequest> requestsQueue = new LinkedList<>();
    private DownloadHandler downloadHandler;

    private Subscription loadRequestsSubscription = Subscriptions.empty();

    public static void start(Context context) {
        context.startService(createIntent(context, ACTION_START_DOWNLOAD));
    }

    public static void stop(Context context) {
        context.startService(createIntent(context, ACTION_STOP_DOWNLOAD));
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
                          EventBus eventBus,
                          OfflineContentScheduler offlineContentScheduler,
                          DownloadHandler.Builder builder,
                          Scheduler scheduler) {
        this.downloadOperations = downloadOps;
        this.offlineContentOperations = offlineContentOperations;
        this.notificationController = notificationController;
        this.eventBus = eventBus;
        this.offlineContentScheduler = offlineContentScheduler;
        this.builder = builder;
        this.scheduler = scheduler;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        downloadHandler = builder.create(this);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        final String action = intent.getAction();
        Log.d(TAG, "Starting offlineContentService for action: " + action);

        offlineContentScheduler.cancelPendingRetries();
        if (ACTION_START_DOWNLOAD.equalsIgnoreCase(action)) {
            loadRequestsSubscription.unsubscribe();

            fireAndForget(downloadOperations
                    .deletePendingRemovals()
                    .subscribeOn(scheduler));

            loadRequestsSubscription = offlineContentOperations
                    .loadDownloadRequests()
                    .subscribeOn(scheduler)
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(new DownloadSubscriber());

        } else if (ACTION_STOP_DOWNLOAD.equalsIgnoreCase(action)) {
            stop();
        }
        return START_NOT_STICKY;
    }

    @Override
    public void onSuccess(DownloadResult result) {
        Log.d(TAG, "Download finished " + result);

        notificationController.onDownloadSuccess();
        eventBus.publish(EventQueue.ENTITY_STATE_CHANGED, EntityStateChangedEvent.downloadFinished(result.getUrn()));
        eventBus.publish(EventQueue.CURRENT_DOWNLOAD, CurrentDownloadEvent.stop(result.getUrn()));

        downloadNextOrFinish();
    }

    @Override
    public void onError(DownloadResult result) {
        Log.d(TAG, "Download failed " + result);

        notificationController.onDownloadError();
        eventBus.publish(EventQueue.ENTITY_STATE_CHANGED, EntityStateChangedEvent.downloadFailed(result.getUrn()));
        eventBus.publish(EventQueue.CURRENT_DOWNLOAD, CurrentDownloadEvent.stop(result.getUrn()));

        if (result.isFailure()) {
            stopAndRetryLater();
        } else {
            downloadNextOrFinish();
        }
    }

    private void downloadNextOrFinish() {
        if (requestsQueue.isEmpty()) {
            stopAndFinish();
        } else {
            if (downloadOperations.isValidNetwork()) {
                download(requestsQueue.poll());
            } else {
                stopAndRetryLater();
            }
        }
    }

    private void stopAndFinish() {
        stop();
        notificationController.onDownloadsFinished();
    }

    private void stopAndRetryLater() {
        offlineContentScheduler.scheduleRetry();
        stop();
        notificationController.onConnectionError();
    }

    private void download(DownloadRequest request) {
        Log.d(TAG, "Download started " + request);

        final Message message = downloadHandler.obtainMessage(DownloadHandler.ACTION_DOWNLOAD, request);
        downloadHandler.sendMessage(message);
        eventBus.publish(EventQueue.CURRENT_DOWNLOAD, CurrentDownloadEvent.start(request.urn));
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        loadRequestsSubscription.unsubscribe();
        super.onDestroy();
    }

    private final class DownloadSubscriber extends DefaultSubscriber<List<DownloadRequest>> {
        @Override
        public void onNext(List<DownloadRequest> requests) {
            if (downloadHandler.isDownloading()) {
                requests.remove(downloadHandler.getCurrent());
            }
            updateRequestQueue(requests);

            if (!downloadHandler.isDownloading()) {
                downloadNextOrFinish();
            }
        }
    }

    private void updateRequestQueue(List<DownloadRequest> pendingRequests) {
        Log.d(TAG, "Updating download queue with " + pendingRequests.size() + " pendingRequests.");

        publishEntityChangedEventForNewRequests(pendingRequests);
        publishEntityChangedEventForRemovedRequests(pendingRequests);

        requestsQueue.clear();
        requestsQueue.addAll(pendingRequests);

        if (!requestsQueue.isEmpty()) {
            final int size = downloadHandler.isDownloading() ? requestsQueue.size() + 1 : requestsQueue.size();
            startForeground(OFFLINE_NOTIFY_ID, notificationController.onPendingRequests(size));
        }
    }

    private void publishEntityChangedEventForRemovedRequests(List<DownloadRequest> pendingRequests) {
        final Collection<DownloadRequest> removedRequests = subtract(requestsQueue, pendingRequests);
        if (!removedRequests.isEmpty()) {
            eventBus.publish(EventQueue.ENTITY_STATE_CHANGED, EntityStateChangedEvent.downloadRemoved(toUrns(removedRequests)));
        }
    }

    private void publishEntityChangedEventForNewRequests(List<DownloadRequest> pendingRequests) {
        final Collection<DownloadRequest> newRequests = subtract(pendingRequests, requestsQueue);
        if (!newRequests.isEmpty()) {
            eventBus.publish(EventQueue.ENTITY_STATE_CHANGED, EntityStateChangedEvent.downloadRequested(toUrns(newRequests)));
        }
    }

    private Collection<Urn> toUrns(Collection<DownloadRequest> requests) {
        return Collections2.transform(requests, new Function<DownloadRequest, Urn>() {
            @Override
            public Urn apply(DownloadRequest downloadRequest) {
                return downloadRequest.urn;
            }
        });
    }

    private void stop() {
        Log.d(TAG, "Stopping the service");
        loadRequestsSubscription.unsubscribe();
        downloadHandler.quit();

        stopForeground(false);
        stopSelf();
    }

}
