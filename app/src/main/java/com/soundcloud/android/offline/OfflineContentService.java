package com.soundcloud.android.offline;

import static com.soundcloud.android.NotificationConstants.OFFLINE_NOTIFY_ID;
import static com.soundcloud.android.offline.DownloadRequest.TO_TRACK_URN;
import static com.soundcloud.android.rx.observers.DefaultSubscriber.fireAndForget;
import static com.soundcloud.java.collections.Lists.newArrayList;
import static com.soundcloud.java.collections.MoreCollections.transform;

import com.soundcloud.android.ApplicationModule;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.rx.RxUtils;
import com.soundcloud.android.rx.observers.DefaultSubscriber;
import com.soundcloud.android.utils.Log;
import org.jetbrains.annotations.Nullable;
import rx.Observable;
import rx.Scheduler;
import rx.Subscription;
import rx.functions.Func1;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.os.Message;
import android.support.annotation.VisibleForTesting;

import javax.inject.Inject;
import javax.inject.Named;
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
    @Inject @Named(ApplicationModule.LOW_PRIORITY) Scheduler scheduler;

    private DownloadHandler downloadHandler;
    private Subscription subscription = RxUtils.invalidSubscription();
    private boolean isStopping;

    private final Func1<List<Urn>, Observable<Collection<Urn>>> removeTracks = new Func1<List<Urn>, Observable<Collection<Urn>>>() {
        @Override
        public Observable<Collection<Urn>> call(List<Urn> urns) {
            return downloadOperations.removeOfflineTracks(urns);
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
                          DownloadQueue queue,
                          Scheduler scheduler) {
        this.downloadOperations = downloadOps;
        this.offlineContentOperations = offlineContentOperations;
        this.notificationController = notificationController;
        this.offlineContentScheduler = offlineContentScheduler;
        this.publisher = publisher;
        this.builder = builder;
        this.queue = queue;
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
        isStopping = ACTION_STOP.equals(intent.getAction());

        Log.d(TAG, " Starting offlineContentService for action: " + action);
        offlineContentScheduler.cancelPendingRetries();

        if (ACTION_START.equalsIgnoreCase(action)) {
            fireAndForget(offlineContentOperations.loadContentToDelete().flatMap(removeTracks));

            subscription.unsubscribe();
            subscription = offlineContentOperations
                    .loadOfflineContentUpdates()
                    .observeOn(scheduler)
                    .subscribe(new OfflineContentRequestsSubscriber());

        } else if (ACTION_STOP.equalsIgnoreCase(action)) {
            // ACTION_STOP is used to stop immediately without any states update.
            // It is currently only used when logging out.
            subscription.unsubscribe();
            if (downloadHandler.isDownloading()) {
                downloadHandler.cancel();
            }
            stop();
        }
        return START_NOT_STICKY;
    }

    @Override
    public void onSuccess(DownloadState state) {
        Log.d(TAG, "onSuccess> Download finished state = [" + state + "]");

        notificationController.onDownloadSuccess(state);
        publisher.publishDownloaded(state.request.getTrack());
        offlineContentOperations.setHasOfflineContent(true);

        downloadNextOrFinish(state);
    }

    @Override
    public void onError(DownloadState state) {
        Log.d(TAG, "onError> Download failed. state = [" + state + "]");

        if (state.isUnavailable()) {
            publisher.publishUnavailable(state.request.getTrack());
        } else {
            publisher.publishRequested(state.request.getTrack());
        }
        notificationController.onDownloadError(state);

        if (state.isConnectionError()) {
            Log.d(TAG, "onError> Connection error.");
            notificationController.onConnectionError(state);
            stopAndRetryLater();
        } else {
            Log.d(TAG, "onError> Download next.");
            downloadNextOrFinish(state);
        }
    }

    @Override
    public void onProgress(DownloadState state) {
        notificationController.onDownloadProgress(state);
    }

    @Override
    public void onCancel(DownloadState state) {
        Log.d(TAG, "onCancel> state = [" + state + "]");
        notificationController.onDownloadCancel(state);

        if (isStopping) {
            Log.d(TAG, "onCancel> Service is stopping.");
            stop();
        } else {
            Log.d(TAG, "onCancel> Download next.");
            downloadNextOrFinish(state);
        }
    }

    private void downloadNextOrFinish(@Nullable DownloadState result) {
        if (queue.isEmpty()) {
            Log.d(TAG, "downloadNextOrFinish> Download queue is empty. Stopping.");
            stopAndFinish(result);
        } else {
            final DownloadRequest request = queue.poll();
            Log.d(TAG, "downloadNextOrFinish> Downloading " + request);
            download(request);
        }
    }

    private void stopAndFinish(DownloadState result) {
        Log.d(TAG, "stopAndFinish> last result = [" + result + "]");
        stop();
        notificationController.onDownloadsFinished(result);
    }

    private void stopAndRetryLater() {
        Log.d(TAG, "stopAndRetryLater>");
        offlineContentScheduler.scheduleRetry();
        stop();
    }

    private void download(DownloadRequest request) {
        Log.d(TAG, "download> request = [" + request + "]");

        final Message message = downloadHandler.obtainMessage(DownloadHandler.ACTION_DOWNLOAD, request);
        downloadHandler.sendMessage(message);
        publisher.publishDownloading(request.getTrack());
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "onDestroy");
        subscription.unsubscribe();
        super.onDestroy();
    }

    private void stop() {
        Log.d(TAG, "Stopping the service");
        subscription.unsubscribe();
        downloadHandler.quit();

        stopForeground(false);
        stopSelf();
    }

    private class OfflineContentRequestsSubscriber extends DefaultSubscriber<OfflineContentUpdates> {
        @Override
        public void onNext(OfflineContentUpdates updates) {
            Log.d(OfflineContentService.TAG, "Received OfflineContentRequests: " + updates);

            publisher.publishEmptyCollections(updates.userExpectedOfflineContent());
            publisher.publishRemoved(updates.tracksToRemove());
            publisher.publishDownloaded(updates.tracksToRestore());
            publisher.publishUnavailable(updates.unavailableTracks());

            final List<DownloadRequest> requests = newArrayList(updates.tracksToDownload());
            final boolean noContentRequested = updates.userExpectedOfflineContent().isEmpty();

            if (downloadHandler.isDownloading()) {
                final DownloadRequest currentRequest = downloadHandler.getCurrentRequest();
                if (requests.contains(currentRequest)) {
                    Log.d(OfflineContentService.TAG, "Keep downloading." + currentRequest);
                    requests.remove(currentRequest);
                    setNewRequests(requests, noContentRequested);
                    publisher.publishDownloading(currentRequest.getTrack());
                } else {
                    Log.d(OfflineContentService.TAG, "Cancelling " + currentRequest);
                    setNewRequests(requests, noContentRequested);
                    // download cancelled event is sent in the callback.
                    downloadHandler.cancel();
                    publisher.publishRemoved(currentRequest.getTrack());
                }
            } else {
                setNewRequests(requests, noContentRequested);
                downloadNextOrFinish(null);
            }
        }
    }

    private void setNewRequests(List<DownloadRequest> requests, boolean muteNotification) {
        Log.d(OfflineContentService.TAG, "setNewRequests requests = [" + requests + "]");
        queue.set(requests);
        publisher.publishRequested(newArrayList(transform(requests, TO_TRACK_URN)));

        if (muteNotification || queue.isEmpty()) {
            notificationController.reset();
        } else {
            startForeground(OFFLINE_NOTIFY_ID, notificationController.onPendingRequests(queue));
        }
    }

}
