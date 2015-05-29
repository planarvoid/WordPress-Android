package com.soundcloud.android.creators.upload;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.NotificationConstants;
import com.soundcloud.android.R;
import com.soundcloud.android.api.legacy.model.Recording;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.android.testsupport.fixtures.ModelFixtures;
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
public class UploadNotificationControllerTest {

    @Mock private NotificationManager notificationManager;
    @Mock private NotificationCompat.Builder notificationBuilder;
    @Mock private Notification notification;

    private UploadNotificationController notificationController;
    private Provider<NotificationCompat.Builder> notificationBuilderProvider = new Provider<NotificationCompat.Builder>() {
        @Override
        public NotificationCompat.Builder get() {
            return notificationBuilder;
        }
    };

    @Before
    public void setUp() throws Exception {
        when(notificationBuilder.build()).thenReturn(notification);

        notificationController = new UploadNotificationController(
                Robolectric.application,
                Robolectric.application.getResources(),
                notificationManager,
                notificationBuilderProvider);
    }

    @Test
    public void showProcessingNotificationCreatesConfiguredNotification() throws Exception {
        final Recording recording = ModelFixtures.create(Recording.class);
        notificationController.showProcessingNotification(recording);

        verify(notificationBuilder).setContentTitle(recording.getTitle());
        verify(notificationBuilder).setContentText(Robolectric.application.getString(R.string.uploader_event_processing));
        verify(notificationBuilder).setOngoing(true);
        verify(notificationBuilder).setAutoCancel(false);
        verify(notificationBuilder).setVisibility(NotificationCompat.VISIBILITY_PUBLIC);
        verify(notificationManager).notify(eq(NotificationConstants.UPLOADING_NOTIFY_ID), any(Notification.class));
    }

    @Test
    public void showTransferringNotificationCreatesConfiguredNotification() throws Exception {
        final Recording recording = ModelFixtures.create(Recording.class);
        notificationController.showTransferringNotification(recording, 50);

        verify(notificationBuilder).setContentTitle(recording.getTitle());
        verify(notificationBuilder).setContentText(Robolectric.application.getString(R.string.uploader_event_uploading_percent, 50));
        verify(notificationBuilder).setProgress(100, 50, false);
        verify(notificationBuilder).setOngoing(true);
        verify(notificationBuilder).setAutoCancel(false);
        verify(notificationBuilder).setVisibility(NotificationCompat.VISIBILITY_PUBLIC);
        verify(notificationManager).notify(eq(NotificationConstants.UPLOADING_NOTIFY_ID), any(Notification.class));
    }

    @Test
    public void showUploadFinishedCreatesConfiguredNotification() throws Exception {
        final Recording recording = ModelFixtures.create(Recording.class);
        notificationController.showUploadFinished(recording);

        verify(notificationBuilder).setContentTitle(Robolectric.application.getString(R.string.cloud_uploader_notification_finished_title));
        verify(notificationBuilder).setContentText(Robolectric.application.getString(R.string.cloud_uploader_notification_finished_message, recording.getTitle()));
        verify(notificationBuilder).setOngoing(false);
        verify(notificationBuilder).setAutoCancel(true);
        verify(notificationBuilder).setVisibility(NotificationCompat.VISIBILITY_PUBLIC);
        verify(notificationManager).notify(eq(NotificationConstants.UPLOADING_NOTIFY_ID), any(Notification.class));
    }

    @Test
    public void showUploadErrorCreatesConfiguredNotification() throws Exception {
        final Recording recording = ModelFixtures.create(Recording.class);
        notificationController.showUploadError(recording);

        verify(notificationBuilder).setContentTitle(Robolectric.application.getString(R.string.cloud_uploader_notification_error_title));
        verify(notificationBuilder).setContentText(Robolectric.application.getString(R.string.cloud_uploader_notification_error_message, recording.getTitle()));
        verify(notificationBuilder).setOngoing(false);
        verify(notificationBuilder).setAutoCancel(true);
        verify(notificationBuilder).setVisibility(NotificationCompat.VISIBILITY_PUBLIC);
        verify(notificationManager).notify(eq(NotificationConstants.UPLOADING_NOTIFY_ID), any(Notification.class));
    }

    @Test
    public void onUploadCancelledCancelsNotification() throws Exception {
        notificationController.onUploadCancelled();

        verify(notificationManager).cancel(eq(NotificationConstants.UPLOADING_NOTIFY_ID));
    }
}