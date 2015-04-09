package com.soundcloud.android.offline;

import static com.soundcloud.android.NotificationConstants.OFFLINE_NOTIFY_ID;
import static com.soundcloud.android.rx.observers.DefaultSubscriber.fireAndForget;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Predicate;
import com.google.common.collect.Collections2;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.events.CurrentDownloadEvent;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.rx.eventbus.EventBus;
import com.soundcloud.android.rx.observers.DefaultSubscriber;
import com.soundcloud.android.utils.Log;
import rx.Observable;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Func1;
import rx.subscriptions.Subscriptions;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.os.Message;

import javax.inject.Inject;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

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
    @Inject DownloadQueue queue;
    private DownloadHandler downloadHandler;

    private Subscription subscription = Subscriptions.empty();

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
                          DownloadQueue queue) {
        this.downloadOperations = downloadOps;
        this.offlineContentOperations = offlineContentOperations;
        this.notificationController = notificationController;
        this.eventBus = eventBus;
        this.offlineContentScheduler = offlineContentScheduler;
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
        Log.d(TAG, "Starting offlineContentService for action: " + action);

        offlineContentScheduler.cancelPendingRetries();

        if (ACTION_START_DOWNLOAD.equalsIgnoreCase(action)) {
            fireAndForget(offlineContentOperations.loadContentToDelete().flatMap(removeTracks));

            subscription.unsubscribe();
            subscription = offlineContentOperations
                    .loadOfflineContentUpdates()
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(new OfflineContentRequestsSubscriber());
        } else if (ACTION_STOP_DOWNLOAD.equalsIgnoreCase(action)) {
            stop();
        }
        return START_NOT_STICKY;
    }

    @Override
    public void onSuccess(DownloadResult result) {
        Log.d(TAG, "Download finished " + result);

        notificationController.onDownloadSuccess();
        notifyDownloaded(result);
        notifyRequestedPlaylists(result);

        downloadNextOrFinish();
    }

    @Override
    public void onError(DownloadResult result) {
        Log.d(TAG, "Download failed " + result);

        notificationController.onDownloadError();
        notifyTrackUnavailable(result);
        notifyRequestedPlaylists(result);
        notifyRelatedPlaylistsAsRequested(result);

        if (result.isConnectionError()) {
            stopAndRetryLater();
        } else {
            downloadNextOrFinish();
        }
    }

    private void notifyRequestedPlaylists(DownloadResult result) {
        final List<Urn> requested = queue.getRequested(result);
        final boolean likedTrackRequested = queue.isLikedTrackRequested();

        if (hasChanges(requested, likedTrackRequested)) {
            eventBus.publish(EventQueue.CURRENT_DOWNLOAD, CurrentDownloadEvent.downloadRequested(likedTrackRequested, requested));
        }
    }

    private void notifyRelatedPlaylistsAsRequested(DownloadResult result) {
        List<Urn> relatedPlaylists = result.getRequest().inPlaylists;
        if (!relatedPlaylists.isEmpty()) {
            eventBus.publish(EventQueue.CURRENT_DOWNLOAD, CurrentDownloadEvent.downloadRequested(false, relatedPlaylists));
        }
    }

    private void notifyDownloaded(DownloadResult result) {
        final List<Urn> completed = queue.getDownloaded(result);
        final boolean isLikedTrackCompleted = queue.isAllLikedTracksDownloaded(result);

        if (hasChanges(completed, isLikedTrackCompleted)) {
            eventBus.publish(EventQueue.CURRENT_DOWNLOAD, CurrentDownloadEvent.downloaded(isLikedTrackCompleted, completed));
        }
    }

    private void notifyTrackUnavailable(DownloadResult result) {
        eventBus.publish(EventQueue.CURRENT_DOWNLOAD, CurrentDownloadEvent.unavailable(false, Arrays.asList(result.getTrack())));
    }

    private boolean hasChanges(List<Urn> entitiesChangeList, boolean likedTracksChanged) {
        return !entitiesChangeList.isEmpty() || likedTracksChanged;
    }

    private void downloadNextOrFinish() {
        if (queue.isEmpty()) {
            stopAndFinish();
        } else if (downloadOperations.isValidNetwork()) {
            download(queue.poll());
        } else {
            stopAndRetryLater();
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
        eventBus.publish(EventQueue.CURRENT_DOWNLOAD, CurrentDownloadEvent.downloading(request));
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
        subscription.unsubscribe();
        downloadHandler.quit();

        stopForeground(false);
        stopSelf();
    }

    private class OfflineContentRequestsSubscriber extends DefaultSubscriber<OfflineContentRequests> {
        @Override
        public void onNext(OfflineContentRequests requests) {
            if (!requests.newRemovedTracks.isEmpty()) {
                // TODO : Cancel request if downloading a removed track.
                eventBus.publish(EventQueue.CURRENT_DOWNLOAD, CurrentDownloadEvent.downloadRemoved(requests.newRemovedTracks));
            }
            if (!requests.newRestoredRequests.isEmpty()) {
                eventBus.publish(EventQueue.CURRENT_DOWNLOAD, CurrentDownloadEvent.downloaded(requests.newRestoredRequests));
            }

            if (!queue.getRequests().isEmpty()) {
                eventBus.publish(EventQueue.CURRENT_DOWNLOAD, CurrentDownloadEvent.downloadRequestRemoved(queue.getRequests()));
            }
            queue.set(Collections2.filter(requests.allDownloadRequests, isNotCurrentDownloadFilter));
            if (!queue.getRequests().isEmpty()) {
                eventBus.publish(EventQueue.CURRENT_DOWNLOAD, CurrentDownloadEvent.downloadRequested(queue.getRequests()));
            }
            updateNotification();
            startDownloadIfNecessary();
        }

        private void startDownloadIfNecessary() {
            if (!downloadHandler.isDownloading()) {
                downloadNextOrFinish();
            }
        }

        private void updateNotification() {
            if (!queue.isEmpty()) {
                final int size = downloadHandler.isDownloading() ? queue.size() + 1 : queue.size();
                startForeground(OFFLINE_NOTIFY_ID, notificationController.onPendingRequests(size));
            }
        }
    }
}
