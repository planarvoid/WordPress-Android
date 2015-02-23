package com.soundcloud.android.offline;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;

import com.soundcloud.android.NotificationConstants;
import com.soundcloud.android.R;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.xtremelabs.robolectric.Robolectric;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;

import android.app.Notification;
import android.app.NotificationManager;
import android.support.v4.app.NotificationCompat;

import javax.inject.Provider;

@RunWith(SoundCloudTestRunner.class)
public class DownloadNotificationControllerTest {

    private static final String DOWNLOAD_IN_PROGRESS = getString(R.string.offline_update_in_progress);
    private static final String DOWNLOAD_COMPLETED = getString(R.string.offline_update_completed_title);

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
                Robolectric.application,
                notificationManager,
                notificationBuilderProvider,
                Robolectric.application.getResources());
    }

    @Test
    public void displayCompletedNotificationWhenCompleted() {
        notificationController.onPendingRequests(20);

        reset(notificationBuilder);
        notificationController.onDownloadsFinished();

        verify(notificationBuilder).setContentTitle(DOWNLOAD_COMPLETED);
        verify(notificationBuilder).setOngoing(false);
        verify(notificationBuilder).setVisibility(NotificationCompat.VISIBILITY_PUBLIC);
        verify(notificationManager).notify(eq(NotificationConstants.OFFLINE_NOTIFY_ID), any(Notification.class));
    }

    @Test
    public void doesNotShowCompletedNotificationIfNoTrackDownloaded() {
        notificationController.onPendingRequests(1);
        notificationController.onDownloadError();

        reset(notificationBuilder, notificationManager);
        notificationController.onDownloadsFinished();

        verify(notificationManager).cancel(NotificationConstants.OFFLINE_NOTIFY_ID);
    }

    @Test
    public void onCompletedDoesNotShowNotificationWhenNoPendingRequests() {
        notificationController.onDownloadsFinished();

        verify(notificationManager, never()).notify(eq(NotificationConstants.OFFLINE_NOTIFY_ID), any(Notification.class));
    }

    @Test
    public void onNewPendingRequestsCreatesNewProgressNotification() {
        notificationController.onPendingRequests(20);

        verify(notificationBuilder).setContentTitle(DOWNLOAD_IN_PROGRESS);
        verify(notificationBuilder).setOngoing(true);
        verify(notificationBuilder).setVisibility(NotificationCompat.VISIBILITY_PUBLIC);
        verify(notificationBuilder).setProgress(20, 0, false);
        verify(notificationBuilder).setContentText(getQuantifiedDownloadString(1, 20));
    }

    @Test
    public void onNewPendingRequestsOverridesNumberOfTotalDownloads() {
        notificationController.onPendingRequests(5);
        notificationController.onDownloadSuccess();

        reset(notificationBuilder);
        notificationController.onPendingRequests(10);

        verify(notificationBuilder).setContentTitle(DOWNLOAD_IN_PROGRESS);
        verify(notificationBuilder).setOngoing(true);
        verify(notificationBuilder).setVisibility(NotificationCompat.VISIBILITY_PUBLIC);
        verify(notificationBuilder).setProgress(11, 1, false);
        verify(notificationBuilder).setContentText(getQuantifiedDownloadString(2, 11));
    }

    @Test
    public void onDownloadSuccessModifiesNumberOfCompletedDownloads() {
        notificationController.onPendingRequests(20);
        reset(notificationBuilder);

        notificationController.onDownloadSuccess();

        verify(notificationBuilder).setContentTitle(DOWNLOAD_IN_PROGRESS);
        verify(notificationBuilder).setOngoing(true);
        verify(notificationBuilder).setVisibility(NotificationCompat.VISIBILITY_PUBLIC);
        verify(notificationBuilder).setProgress(20, 1, false);
        verify(notificationBuilder).setContentText(getQuantifiedDownloadString(2, 20));
        verify(notificationManager).notify(eq(NotificationConstants.OFFLINE_NOTIFY_ID), any(Notification.class));
    }

    @Test
    public void onDownloadErrorModifiesNumberOfCompletedDownloads() {
        notificationController.onPendingRequests(20);

        reset(notificationBuilder);
        notificationController.onDownloadError();

        verify(notificationBuilder).setContentTitle(DOWNLOAD_IN_PROGRESS);
        verify(notificationBuilder).setOngoing(true);
        verify(notificationBuilder).setVisibility(NotificationCompat.VISIBILITY_PUBLIC);
        verify(notificationBuilder).setProgress(20, 1, false);
        verify(notificationBuilder).setContentText(getQuantifiedDownloadString(2, 20));
        verify(notificationManager).notify(eq(NotificationConstants.OFFLINE_NOTIFY_ID), any(Notification.class));
    }

    @Test
    public void onProgressUpdateDisplaysCountBasedOnCurrentDownloadingTrack() {
        notificationController.onPendingRequests(2);
        reset(notificationBuilder);

        notificationController.onDownloadSuccess();

        verify(notificationBuilder).setContentTitle(DOWNLOAD_IN_PROGRESS);
        verify(notificationBuilder).setOngoing(true);
        verify(notificationBuilder).setVisibility(NotificationCompat.VISIBILITY_PUBLIC);
        verify(notificationBuilder).setProgress(2, 1, false);
        verify(notificationBuilder).setContentText(getQuantifiedDownloadString(2, 2));
        verify(notificationManager).notify(eq(NotificationConstants.OFFLINE_NOTIFY_ID), any(Notification.class));
    }

    @Test
    public void removesNotificationOnConnectionError() {
        notificationController.onConnectionError();

        verify(notificationManager).cancel(NotificationConstants.OFFLINE_NOTIFY_ID);
    }

    private String getQuantifiedDownloadString(int completed, int queueSize) {
        return Robolectric.application.getResources().getQuantityString(R.plurals.downloading_track_of_tracks, queueSize, completed, queueSize);
    }

    private static String getString(int resId) {
        return Robolectric.application.getString(resId);
    }

}
