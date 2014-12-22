package com.soundcloud.android.offline;

import static com.soundcloud.android.NotificationConstants.OFFLINE_NOTIFY_ID;

import com.google.common.annotations.VisibleForTesting;
import com.soundcloud.android.SoundCloudApplication;
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
import javax.inject.Singleton;
import java.util.List;

@Singleton
public class OfflineContentService extends Service {

    protected static final String TAG = "OfflineContent";
    protected static final String ACTION_DOWNLOAD_TRACKS = "action_download_tracks";

    @Inject DownloadOperations downloadOperations;
    @Inject DownloadNotificationController notificationController;

    private Subscription subscription = Subscriptions.empty();

    private final Action1<List<DownloadRequest>> updateNotification = new Action1<List<DownloadRequest>>() {
        @Override
        public void call(List<DownloadRequest> downloadRequests) {
            notificationController.onNewPendingRequests(downloadRequests.size());
        }
    };

    private final Func1<List<DownloadRequest>, Observable<DownloadResult>> toDownloadResult =
            new Func1<List<DownloadRequest>, Observable<DownloadResult>>() {
                @Override
                public Observable<DownloadResult> call(List<DownloadRequest> downloadRequests) {
                    return downloadOperations.processDownloadRequests(downloadRequests);
                }
            };

    public static void syncOfflineContent(Context context) {
        context.startService(getDownloadIntent(context));
    }

    private static Intent getDownloadIntent(Context context) {
        final Intent intent = new Intent(context, OfflineContentService.class);
        intent.setAction(ACTION_DOWNLOAD_TRACKS);
        return intent;
    }

    public OfflineContentService() {
        SoundCloudApplication.getObjectGraph().inject(this);
    }

    @VisibleForTesting
    OfflineContentService(DownloadOperations downloadOps, DownloadNotificationController notificationController) {
        this.downloadOperations = downloadOps;
        this.notificationController = notificationController;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        final String action = intent.getAction();
        Log.d(TAG, "Starting offlineContentService for action: " + action);

        if (ACTION_DOWNLOAD_TRACKS.equalsIgnoreCase(action)) {

            startForeground(OFFLINE_NOTIFY_ID, notificationController.create());
            subscription.unsubscribe();

            subscription = downloadOperations
                    .pendingDownloads()
                    .doOnNext(updateNotification)
                    .flatMap(toDownloadResult)
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(new DownloadResultSubscriber());
        }
        return START_NOT_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void stop() {
        Log.d(TAG, "Stopping the service");
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
        }
    }
}
