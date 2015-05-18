package com.soundcloud.android.creators.upload;

import com.soundcloud.android.Actions;
import com.soundcloud.android.NotificationConstants;
import com.soundcloud.android.R;
import com.soundcloud.android.api.legacy.model.Recording;
import com.soundcloud.android.utils.images.ImageUtils;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.support.v4.app.NotificationCompat;
import android.text.TextUtils;

import javax.inject.Inject;
import javax.inject.Provider;

public class UploadNotificationController {

    private final Context context;
    private final Resources resources;
    private final NotificationManager notificationManager;
    private final NotificationCompat.Builder progressNotification;
    private final NotificationCompat.Builder finishedNotification;

    @Inject
    public UploadNotificationController(Context context, Resources resources, NotificationManager notificationManager,
                                        Provider<NotificationCompat.Builder> notificationBuilderProvider) {
        this.context = context;
        this.resources = resources;
        this.notificationManager = notificationManager;
        this.progressNotification = notificationBuilderProvider.get();
        this.finishedNotification = notificationBuilderProvider.get();
    }

    public void showProcessingNotification(Recording recording) {
        sendNotification(createProcessingNotification(recording));
    }

    private Notification createProcessingNotification(Recording recording) {
        setProgressOptions(progressNotification, recording);
        progressNotification.setContentIntent(getPendingProcessingIntent(recording));
        progressNotification.setContentText(resources.getString(R.string.uploader_event_processing));
        progressNotification.setProgress(100, 0, true);
        return progressNotification.build();
    }

    private PendingIntent getPendingProcessingIntent(Recording recording) {
        final Intent monitorIntentWithProgress = getMonitorIntent(recording).putExtra(UploadService.EXTRA_STAGE, UploadService.UPLOAD_STAGE_PROCESSING);
        return PendingIntent.getActivity(context, 0, monitorIntentWithProgress, PendingIntent.FLAG_UPDATE_CURRENT);
    }

    public void showTransferringNotification(Recording recording, int progress) {
        sendNotification(createTransferringNotification(recording, progress));
    }

    private Notification createTransferringNotification(Recording recording, int progress) {
        setProgressOptions(progressNotification, recording);
        progressNotification.setContentIntent(getTransferringPendingIntent(recording, progress));
        progressNotification.setContentText(resources.getString(R.string.uploader_event_uploading_percent, progress));
        progressNotification.setProgress(100, progress, false);
        return progressNotification.build();
    }

    private PendingIntent getTransferringPendingIntent(Recording recording, int progress) {
        final Intent monitorIntentWithProgress = getMonitorIntent(recording)
                .putExtra(UploadService.EXTRA_STAGE, UploadService.UPLOAD_STAGE_TRANSFERRING)
                .putExtra(UploadService.EXTRA_PROGRESS, progress);
        return PendingIntent.getActivity(context, 0, monitorIntentWithProgress, PendingIntent.FLAG_UPDATE_CURRENT);
    }

    public void showUploadFinished(Recording recording) {
        sendNotification(createUploadFinishedNotification(recording));
    }

    public void showUploadError(Recording recording) {
        sendNotification(createUploadErrorNotification(recording));
    }

    private Notification createUploadFinishedNotification(Recording recording) {
        setDoneOptions(recording);
        finishedNotification.setContentTitle(resources.getString(R.string.cloud_uploader_notification_finished_title));
        finishedNotification.setContentText(resources.getString(R.string.cloud_uploader_notification_finished_message, recording.title));
        finishedNotification.setTicker(resources.getString(R.string.cloud_uploader_notification_finished_ticker));
        finishedNotification.setContentIntent(PendingIntent.getActivity(context, 0, new Intent(Actions.YOUR_SOUNDS), PendingIntent.FLAG_UPDATE_CURRENT));
        return finishedNotification.build();
    }

    private Notification createUploadErrorNotification(Recording recording) {
        setDoneOptions(recording);
        finishedNotification.setContentTitle(resources.getString(R.string.cloud_uploader_notification_error_title));
        finishedNotification.setContentText(resources.getString(R.string.cloud_uploader_notification_error_message, recording.title));
        finishedNotification.setTicker(resources.getString(R.string.cloud_uploader_notification_error_ticker));
        finishedNotification.setContentIntent(PendingIntent.getActivity(context, 0, getMonitorIntent(recording), PendingIntent.FLAG_UPDATE_CURRENT));
        return finishedNotification.build();
    }

    public void onUploadCancelled() {
        notificationManager.cancel(NotificationConstants.UPLOADING_NOTIFY_ID);
    }

    private void sendNotification(Notification notification) {
        // ugly way to help uniqueness
        notificationManager.notify(NotificationConstants.UPLOADING_NOTIFY_ID, notification);
    }

    private void setProgressOptions(NotificationCompat.Builder notificationBuilder, Recording recording) {
        setDefaultOptions(notificationBuilder, recording);
        notificationBuilder.setAutoCancel(false);
        notificationBuilder.setOngoing(true);
    }

    private void setDoneOptions(Recording recording) {
        setDefaultOptions(finishedNotification, recording);
        finishedNotification.setOngoing(false);
        finishedNotification.setAutoCancel(true);
    }

    private void setDefaultOptions(NotificationCompat.Builder notificationBuilder, Recording recording) {
        notificationBuilder.setContentTitle(TextUtils.isEmpty(recording.title) ? recording.sharingNote(resources) : recording.title);
        notificationBuilder.setSmallIcon(R.drawable.ic_notification_cloud);
        notificationBuilder.setVisibility(NotificationCompat.VISIBILITY_PUBLIC);
        configureIcon(notificationBuilder, recording);
    }

    private void configureIcon(NotificationCompat.Builder notificationBuilder, Recording recording) {
        if (recording.hasArtwork()) {
            Bitmap bitmap = ImageUtils.getConfiguredBitmap(recording.getArtwork(),
                    (int) resources.getDimension(R.dimen.notification_image_width),
                    (int) resources.getDimension(R.dimen.notification_image_height));
            if (bitmap != null) {
                notificationBuilder.setLargeIcon(bitmap);
            }
        }
    }

    public Intent getMonitorIntent(Recording recording) {
        return new Intent(Actions.UPLOAD_MONITOR).putExtra(UploadService.EXTRA_RECORDING, recording);
    }
}
