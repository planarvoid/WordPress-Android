package com.soundcloud.android.offline;

import static com.soundcloud.android.offline.DownloadOperations.ConnectionState;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;

import com.soundcloud.android.NotificationConstants;
import com.soundcloud.android.R;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.android.testsupport.fixtures.ModelFixtures;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;

import android.app.Notification;
import android.app.NotificationManager;
import android.support.v4.app.NotificationCompat;

import javax.inject.Provider;
import java.util.ArrayList;
import java.util.Collection;

public class DownloadNotificationControllerTest extends AndroidUnitTest{

    private static final String DOWNLOAD_IN_PROGRESS = getString(R.string.offline_update_in_progress);
    private static final String DOWNLOAD_COMPLETED = getString(R.string.offline_update_completed_title);
    private static final String DOWNLOAD_PAUSED = getString(R.string.offline_update_paused);
    private static final int TRACK_DURATION = 1234;
    private static final long TRACK_DURATION_IN_BYTES = SecureFileStorage.calculateFileSizeInBytes(TRACK_DURATION);

    private final DownloadRequest downloadRequest = ModelFixtures.downloadRequestFromLikes(Urn.forTrack(123L));
    private final DownloadState successfulDownloadState = DownloadState.success(downloadRequest);
    private final DownloadState failedDownloadState = DownloadState.error(downloadRequest);
    private final DownloadState storageLimitResult = DownloadState.notEnoughSpace(downloadRequest);

    @Mock private NotificationManager notificationManager;
    @Mock private NotificationCompat.Builder notificationBuilder;

    private DownloadNotificationController notificationController;
    private Provider<NotificationCompat.Builder> notificationBuilderProvider = new Provider<NotificationCompat.Builder>() {
        @Override
        public NotificationCompat.Builder get() {
            return notificationBuilder;
        }
    };

    @Before
    public void setUp() throws Exception {
        notificationController = new DownloadNotificationController(
                context(),
                notificationManager,
                notificationBuilderProvider,
                resources());
    }

    @Test
    public void onDownloadsFinishedDisplayCompletedNotification() {
        notificationController.onPendingRequests(createQueue(20));

        reset(notificationBuilder);
        notificationController.onDownloadsFinished(successfulDownloadState);

        verify(notificationBuilder).setContentTitle(DOWNLOAD_COMPLETED);
        verify(notificationBuilder).setOngoing(false);
        verify(notificationBuilder).setVisibility(NotificationCompat.VISIBILITY_PUBLIC);
        verify(notificationManager).notify(eq(NotificationConstants.OFFLINE_NOTIFY_ID), any(Notification.class));
    }

    @Test
    public void onDownloadsFinishedDoesNotShowNotificationIfNoTrackDownloaded() {
        notificationController.onPendingRequests(createQueue(1));
        notificationController.onDownloadError(failedDownloadState);

        reset(notificationBuilder, notificationManager);
        notificationController.onDownloadsFinished(null);

        verify(notificationManager).cancel(NotificationConstants.OFFLINE_NOTIFY_ID);
    }

    @Test
    public void onDownloadsFinishedDoesNotShowNotificationWhenNoPendingRequests() {
        notificationController.onDownloadsFinished(successfulDownloadState);

        verify(notificationManager, never()).notify(eq(NotificationConstants.OFFLINE_NOTIFY_ID), any(Notification.class));
    }

    @Test
    public void onDownloadsFinishedShowsStorageLimitReachedNotification() {
        notificationController.onPendingRequests(createQueue(1));
        notificationController.onDownloadError(storageLimitResult);

        reset(notificationBuilder, notificationManager);
        notificationController.onDownloadsFinished(storageLimitResult);

        verify(notificationBuilder).setContentTitle(getString(R.string.offline_update_storage_limit_reached_title));
        verify(notificationBuilder).setContentText(getString(R.string.offline_update_storage_limit_reached_message));
    }

    @Test
    public void onNewPendingRequestsCreatesNewProgressNotification() {
        notificationController.onPendingRequests(createQueue(20));

        verify(notificationBuilder).setContentTitle(DOWNLOAD_IN_PROGRESS);
        verify(notificationBuilder).setOngoing(true);
        verify(notificationBuilder).setVisibility(NotificationCompat.VISIBILITY_PUBLIC);
        verify(notificationBuilder).setProgress(1000, 0, false);
        verify(notificationBuilder).setContentText(getQuantifiedDownloadString(1, 20));
    }

    @Test
    public void onNewPendingRequestsOverridesNumberOfTotalDownloads() {
        notificationController.onPendingRequests(createQueue(5));
        notificationController.onDownloadSuccess(successfulDownloadState);

        reset(notificationBuilder);
        notificationController.onPendingRequests(createQueue(10));

        verify(notificationBuilder).setContentTitle(DOWNLOAD_IN_PROGRESS);
        verify(notificationBuilder).setOngoing(true);
        verify(notificationBuilder).setVisibility(NotificationCompat.VISIBILITY_PUBLIC);
        verify(notificationBuilder).setProgress(1000, 90, false);
        verify(notificationBuilder).setContentText(getQuantifiedDownloadString(2, 11));
    }

    @Test
    public void onDownloadSuccessModifiesNumberOfCompletedDownloads() {
        notificationController.onPendingRequests(createQueue(3));
        reset(notificationBuilder);

        notificationController.onDownloadSuccess(successfulDownloadState);

        verify(notificationBuilder).setContentTitle(DOWNLOAD_IN_PROGRESS);
        verify(notificationBuilder).setOngoing(true);
        verify(notificationBuilder).setVisibility(NotificationCompat.VISIBILITY_PUBLIC);
        verify(notificationBuilder).setProgress(1000, 333, false);
        verify(notificationBuilder).setContentText(getQuantifiedDownloadString(2, 3));
        verify(notificationManager).notify(eq(NotificationConstants.OFFLINE_NOTIFY_ID), any(Notification.class));
    }

    @Test
    public void currentDownloadDisplayIsLimitedByTotalDownloadsWhenLastItemCompletes() {
        notificationController.onPendingRequests(createQueue(1));
        reset(notificationBuilder);

        notificationController.onDownloadSuccess(successfulDownloadState);

        verify(notificationBuilder).setContentTitle(DOWNLOAD_IN_PROGRESS);
        verify(notificationBuilder).setContentText(getQuantifiedDownloadString(1, 1));
    }

    @Test
    public void onDownloadProgressUpdatesTotalProgress() {
        notificationController.onPendingRequests(createQueue(3));
        reset(notificationBuilder);

        notificationController.onDownloadSuccess(successfulDownloadState);
        Mockito.reset(notificationBuilder, notificationManager);

        notificationController.onDownloadProgress(DownloadState.inProgress(downloadRequest, TRACK_DURATION_IN_BYTES / 2));

        verify(notificationBuilder).setContentTitle(DOWNLOAD_IN_PROGRESS);
        verify(notificationBuilder).setOngoing(true);
        verify(notificationBuilder).setVisibility(NotificationCompat.VISIBILITY_PUBLIC);
        verify(notificationBuilder).setProgress(1000, 500, false);
        verify(notificationBuilder).setContentText(getQuantifiedDownloadString(2, 3));
        verify(notificationManager).notify(eq(NotificationConstants.OFFLINE_NOTIFY_ID), any(Notification.class));
    }

    @Test
    public void onDownloadProgressDoesNotUpdateProgressTwiceWithSameProgress() {
        notificationController.onPendingRequests(createQueue(3));
        reset(notificationBuilder);

        notificationController.onDownloadSuccess(successfulDownloadState);
        Mockito.reset(notificationBuilder, notificationManager);

        notificationController.onDownloadProgress(DownloadState.inProgress(downloadRequest, TRACK_DURATION_IN_BYTES / 2));
        notificationController.onDownloadProgress(DownloadState.inProgress(downloadRequest, TRACK_DURATION_IN_BYTES / 2));

        verify(notificationBuilder).setContentTitle(DOWNLOAD_IN_PROGRESS);
    }

    @Test
    public void onDownloadErrorModifiesNumberOfCompletedDownloads() {
        notificationController.onPendingRequests(createQueue(3));

        reset(notificationBuilder);
        notificationController.onDownloadError(failedDownloadState);

        verify(notificationBuilder).setContentTitle(DOWNLOAD_IN_PROGRESS);
        verify(notificationBuilder).setOngoing(true);
        verify(notificationBuilder).setVisibility(NotificationCompat.VISIBILITY_PUBLIC);
        verify(notificationBuilder).setProgress(1000, 333, false);
        verify(notificationBuilder).setContentText(getQuantifiedDownloadString(2, 3));
        verify(notificationManager).notify(eq(NotificationConstants.OFFLINE_NOTIFY_ID), any(Notification.class));
    }

    @Test
    public void onProgressUpdateProgressBasedOnCurrentDownloadingTrack() {
        notificationController.onPendingRequests(createQueue(3));
        reset(notificationBuilder);

        notificationController.onDownloadSuccess(successfulDownloadState);

        verify(notificationBuilder).setContentTitle(DOWNLOAD_IN_PROGRESS);
        verify(notificationBuilder).setOngoing(true);
        verify(notificationBuilder).setVisibility(NotificationCompat.VISIBILITY_PUBLIC);
        verify(notificationBuilder).setProgress(DownloadNotificationController.PROGRESS_MAX, 333 , false);
        verify(notificationBuilder).setContentText(getQuantifiedDownloadString(2, 3));
        verify(notificationManager).notify(eq(NotificationConstants.OFFLINE_NOTIFY_ID), any(Notification.class));
    }

    @Test
    public void onConnectionErrorWithDisconnectedShowsNoNetworkNotification() {
        reset(notificationBuilder);

        DownloadState result = DownloadState.connectionError(downloadRequest, ConnectionState.DISCONNECTED);
        notificationController.onConnectionError(result);

        verify(notificationBuilder).setContentTitle(DOWNLOAD_PAUSED);
        verify(notificationBuilder).setOngoing(false);
        verify(notificationBuilder).setVisibility(NotificationCompat.VISIBILITY_PUBLIC);
        verify(notificationBuilder).setContentText(getString(R.string.no_network_connection));
        verify(notificationManager).notify(eq(NotificationConstants.OFFLINE_NOTIFY_ID), any(Notification.class));
    }

    @Test
    public void onConnectionNetworkWithWifiOnlyRestrictionShowsWifiOnlyNotification() {
        reset(notificationBuilder);

        DownloadState result = DownloadState.connectionError(downloadRequest, ConnectionState.NOT_ALLOWED);
        notificationController.onConnectionError(result);

        verify(notificationBuilder).setContentTitle(DOWNLOAD_PAUSED);
        verify(notificationBuilder).setOngoing(false);
        verify(notificationBuilder).setVisibility(NotificationCompat.VISIBILITY_PUBLIC);
        verify(notificationBuilder).setContentText(getString(R.string.no_wifi_connection));
        verify(notificationManager).notify(eq(NotificationConstants.OFFLINE_NOTIFY_ID), any(Notification.class));
    }

    private String getQuantifiedDownloadString(int completed, int queueSize) {
        return resources().getQuantityString(R.plurals.downloading_track_of_tracks, queueSize, completed, queueSize);
    }

    private static String getString(int resId) {
        return resources().getString(resId);
    }

    private DownloadQueue createQueue(int size) {
        final DownloadQueue downloadQueue = new DownloadQueue();
        final Collection<DownloadRequest> requests = new ArrayList<>();
        for (int i = 0; i < size; i++){
            requests.add(ModelFixtures.downloadRequestFromLikes(Urn.forTrack(i)));
        }
        downloadQueue.set(requests);
        return downloadQueue;
    }

}
