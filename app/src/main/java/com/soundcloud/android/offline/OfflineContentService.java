package com.soundcloud.android.offline;

import static com.soundcloud.android.NotificationConstants.OFFLINE_NOTIFY_ID;
import static com.soundcloud.android.offline.DownloadRequest.TO_TRACK_URN;
import static com.soundcloud.android.rx.observers.DefaultSubscriber.fireAndForget;
import static com.soundcloud.java.collections.Lists.newArrayList;
import static com.soundcloud.java.collections.MoreCollections.transform;

import com.soundcloud.android.ApplicationModule;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.analytics.performance.MetricKey;
import com.soundcloud.android.analytics.performance.MetricParams;
import com.soundcloud.android.analytics.performance.MetricType;
import com.soundcloud.android.analytics.performance.PerformanceMetric;
import com.soundcloud.android.analytics.performance.PerformanceMetricsEngine;
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
    @VisibleForTesting static final String EXTRA_SHOW_RESULT = "extra_show_result";

    @Inject DownloadOperations downloadOperations;
    @Inject OfflineContentOperations offlineContentOperations;
    @Inject DownloadNotificationController notificationController;
    @Inject OfflineContentScheduler offlineContentScheduler;
    @Inject OfflineStatePublisher publisher;
    @Inject DownloadQueue queue;
    @Inject DownloadHandler.Builder builder;
    @Inject @Named(ApplicationModule.LOW_PRIORITY) Scheduler scheduler;
    @Inject PerformanceMetricsEngine performanceMetricsEngine;

    private DownloadHandler downloadHandler;
    private Subscription subscription = RxUtils.invalidSubscription();

    private boolean isStopping;
    private boolean showResult;
    private long totalDownloadedDuration;

    private final Func1<List<Urn>, Observable<Collection<Urn>>> removeTracks = new Func1<List<Urn>, Observable<Collection<Urn>>>() {
        @Override
        public Observable<Collection<Urn>> call(List<Urn> urns) {
            return downloadOperations.removeOfflineTracks(urns);
        }
    };

    public static void start(Context context) {
        context.startService(createIntent(context, ACTION_START));
    }

    public static void startFromUserAction(Context context) {
        final Intent intent = createIntent(context, ACTION_START);
        intent.putExtra(EXTRA_SHOW_RESULT, true);
        context.startService(intent);
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
                          Scheduler scheduler,
                          PerformanceMetricsEngine performanceMetricsEngine) {
        this.downloadOperations = downloadOps;
        this.offlineContentOperations = offlineContentOperations;
        this.notificationController = notificationController;
        this.offlineContentScheduler = offlineContentScheduler;
        this.publisher = publisher;
        this.builder = builder;
        this.queue = queue;
        this.scheduler = scheduler;
        this.performanceMetricsEngine = performanceMetricsEngine;
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
        showResult = intent.getBooleanExtra(EXTRA_SHOW_RESULT, showResult);

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
            } else {
                stopAndFinish(null);
            }
        }
        return START_NOT_STICKY;
    }

    @Override
    public void onSuccess(DownloadState state) {
        Log.d(TAG, "onSuccess> Download finished state = [" + state + "]");

        notificationController.onDownloadSuccess(state);
        publisher.publishDownloaded(state.request.getUrn());
        offlineContentOperations.setHasOfflineContent(true);
        totalDownloadedDuration += state.request.getDuration();

        downloadNextOrFinish(state);
    }

    @Override
    public void onError(DownloadState state) {
        Log.d(TAG, "onError> Download failed. state = [" + state + "]");

        if (state.isUnavailable()) {
            publisher.publishUnavailable(state.request.getUrn());
        } else {
            publisher.publishRequested(state.request.getUrn());
        }
        notificationController.onDownloadError(state);

        if (state.isConnectivityError()) {
            Log.d(TAG, "onError> Connection error.");
            stopAndRetryLater(state);
        } else if (state.isNotEnoughMinimumSpace()) {
            Log.d(TAG, "onError> Not enough minimum space");
            stopAndFinish(state);
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

        if (isStopping) {
            Log.d(TAG, "onCancel> Service is stopping.");
            notificationController.reset();
            stopAndFinish(state);
        } else {
            Log.d(TAG, "onCancel> Download next.");
            notificationController.onDownloadCancel(state);
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

    private void stopAndFinish(@Nullable DownloadState result) {
        Log.d(TAG, "stopAndFinish> last result = [" + result + "]");
        stop();
        notificationController.onDownloadsFinished(result, showResult);
    }

    private void stopAndRetryLater(DownloadState state) {
        Log.d(TAG, "stopAndRetryLater>");
        offlineContentScheduler.scheduleRetryForConnectivityError();
        stop();
        notificationController.onConnectionError(state, showResult);
    }

    private void download(DownloadRequest request) {
        Log.d(TAG, "download> request = [" + request + "]");

        final Message message = downloadHandler.obtainMessage(DownloadHandler.ACTION_DOWNLOAD, request);
        downloadHandler.sendMessage(message);
        publisher.publishDownloading(request.getUrn());
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
        measureOfflineSync();
        subscription.unsubscribe();
        downloadHandler.quit();

        stopForeground(false);
        stopSelf();
    }

    private void measureOfflineSync() {
        if (totalDownloadedDuration > 0) {
            MetricParams params = new MetricParams().putLong(MetricKey.DOWNLOADED_DURATION, totalDownloadedDuration);
            performanceMetricsEngine.endMeasuring(PerformanceMetric.builder()
                                                                   .metricType(MetricType.OFFLINE_SYNC)
                                                                   .metricParams(params)
                                                                   .build());
        }
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
                if (requests.contains(currentRequest) && downloadOperations.isConnectionValid()) {
                    continueCurrentDownload(requests, noContentRequested, currentRequest);
                } else if (!downloadOperations.isConnectionValid()) {
                    cancelledByInvalidConnection(requests, noContentRequested, currentRequest);
                } else {
                    cancelledByUser(requests, noContentRequested, currentRequest);
                }
            } else {
                setNewRequests(requests, noContentRequested);
                downloadNextOrFinish(null);
                totalDownloadedDuration = 0;
                performanceMetricsEngine.startMeasuring(MetricType.OFFLINE_SYNC);
            }
        }
    }

    private void cancelledByUser(List<DownloadRequest> requests,
                                 boolean noContentRequested,
                                 DownloadRequest currentRequest) {
        Log.d(OfflineContentService.TAG, "Cancelling " + currentRequest);
        setNewRequests(requests, noContentRequested);
        downloadHandler.cancel();
        publisher.publishRemoved(currentRequest.getUrn());
    }

    private void cancelledByInvalidConnection(List<DownloadRequest> requests,
                                              boolean noContentRequested,
                                              DownloadRequest currentRequest) {
        Log.d(OfflineContentService.TAG, "Canceling, no valid connection " + currentRequest);
        setNewRequests(requests, noContentRequested);
        downloadHandler.cancel();
        publisher.publishRequested(currentRequest.getUrn());
    }

    private void continueCurrentDownload(List<DownloadRequest> requests,
                                         boolean noContentRequested,
                                         DownloadRequest currentRequest) {
        Log.d(OfflineContentService.TAG, "Keep downloading." + currentRequest);
        requests.remove(currentRequest);
        setNewRequests(requests, noContentRequested);
        publisher.publishDownloading(currentRequest.getUrn());
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
