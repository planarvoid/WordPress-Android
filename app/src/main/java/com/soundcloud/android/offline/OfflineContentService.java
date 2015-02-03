package com.soundcloud.android.offline;

import static com.soundcloud.android.NotificationConstants.OFFLINE_NOTIFY_ID;

import com.google.common.annotations.VisibleForTesting;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.OfflineSyncEvent;
import com.soundcloud.android.rx.eventbus.EventBus;
import com.soundcloud.android.rx.observers.DefaultSubscriber;
import com.soundcloud.android.utils.Log;
import rx.Observable;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;
import rx.functions.Func1;
import rx.subscriptions.Subscriptions;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;

import javax.inject.Inject;
import java.util.List;

public class OfflineContentService extends Service {

    static final String TAG = "OfflineContent";
    @VisibleForTesting static final String ACTION_START_DOWNLOAD = "action_start_download";
    @VisibleForTesting static final String ACTION_STOP_DOWNLOAD = "action_stop_download";

    @Inject DownloadOperations downloadOperations;
    @Inject DownloadNotificationController notificationController;
    @Inject EventBus eventBus;
    @Inject OfflineContentScheduler offlineContentScheduler;

    private Subscription subscription = Subscriptions.empty();

    private final Action1<List<DownloadRequest>> updateNotification = new Action1<List<DownloadRequest>>() {
        @Override
        public void call(List<DownloadRequest> downloadRequests) {
            startForeground(OFFLINE_NOTIFY_ID, notificationController.onNewPendingRequests(downloadRequests.size()));
        }
    };

    private final Func1<List<DownloadRequest>, Observable<DownloadResult>> toDownloadResult =
            new Func1<List<DownloadRequest>, Observable<DownloadResult>>() {
                @Override
                public Observable<DownloadResult> call(List<DownloadRequest> downloadRequests) {
                    return downloadOperations.processDownloadRequests(downloadRequests);
                }
            };

    public static void startSyncing(Context context) {
        context.startService(createIntent(context, ACTION_START_DOWNLOAD));
    }

    public static void stopSyncing(Context context) {
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
                          DownloadNotificationController notificationController,
                          EventBus eventBus, OfflineContentScheduler offlineContentScheduler) {
        this.downloadOperations = downloadOps;
        this.notificationController = notificationController;
        this.eventBus = eventBus;
        this.offlineContentScheduler = offlineContentScheduler;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        final String action = intent.getAction();
        Log.d(TAG, "Starting offlineContentService for action: " + action);

        if (ACTION_START_DOWNLOAD.equalsIgnoreCase(action)) {

            offlineContentScheduler.cancelPendingRetries();

            eventBus.publish(EventQueue.OFFLINE_SYNC, OfflineSyncEvent.start());
            subscription.unsubscribe();
            subscription = downloadOperations
                    .pendingDownloads()
                    .doOnNext(updateNotification)
                    .flatMap(toDownloadResult)
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(new DownloadResultSubscriber());
        } else if (ACTION_STOP_DOWNLOAD.equalsIgnoreCase(action)) {
            stop();
        }
        return START_NOT_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void stop() {
        Log.d(TAG, "Stopping the service");
        eventBus.publish(EventQueue.OFFLINE_SYNC, OfflineSyncEvent.stop());
        stopForeground(true);
        subscription.unsubscribe();
        stopSelf();
    }

    @Override
    public void onDestroy() {
        subscription.unsubscribe();
        super.onDestroy();
    }

    private final class DownloadResultSubscriber extends DefaultSubscriber<DownloadResult> {
        @Override
        public void onCompleted() {
            stop();
            notificationController.onCompleted();
        }

        @Override
        public void onNext(DownloadResult result) {
            Log.d(TAG, "Downloaded track: " + result);
            notificationController.onProgressUpdate();
        }

        @Override
        public void onError(Throwable throwable) {
            //TODO: error handling and notifications
            Log.e(TAG, "something bad happened", throwable);
            offlineContentScheduler.scheduleRetry();
            notificationController.onError();
        }
    }
}
