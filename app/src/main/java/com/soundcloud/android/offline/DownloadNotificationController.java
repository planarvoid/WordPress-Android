package com.soundcloud.android.offline;

import com.soundcloud.android.Actions;
import com.soundcloud.android.NotificationConstants;
import com.soundcloud.android.R;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.support.v4.app.NotificationCompat;

import javax.inject.Inject;
import javax.inject.Provider;

class DownloadNotificationController {

    private final Context context;
    private final Resources resources;
    private final NotificationManager notificationManager;
    private final Provider<NotificationCompat.Builder> notificationBuilderProvider;

    private NotificationCompat.Builder progressNotification;
    private int completedDownloads;
    private int totalDownloads;

    @Inject
    public DownloadNotificationController(Context context, NotificationManager notificationManager,
                                          Provider<NotificationCompat.Builder> notificationBuilderProvider, Resources resources) {
        this.context = context;
        this.resources = resources;
        this.notificationManager = notificationManager;
        this.notificationBuilderProvider = notificationBuilderProvider;
    }

    public Notification onPendingRequests(int pending) {
        totalDownloads = completedDownloads + pending;
        progressNotification = notificationBuilderProvider.get();

        return updateProgressNotification();
    }

    public void onProgressUpdate() {
        completedDownloads++;
        notificationManager.notify(NotificationConstants.OFFLINE_NOTIFY_ID, updateProgressNotification());
    }

    public void onDownloadsFinished() {
        if (totalDownloads > 0) {
            notificationManager.notify(NotificationConstants.OFFLINE_NOTIFY_ID, buildCompletedNotification());
        }

        completedDownloads = 0;
        totalDownloads = 0;
    }

    public void onError() {
        if (totalDownloads > 0) {
            notificationManager.notify(NotificationConstants.OFFLINE_NOTIFY_ID, buildErrorNotification());
        }

        completedDownloads = 0;
        totalDownloads = 0;
    }

    private Notification buildCompletedNotification() {
        final NotificationCompat.Builder completedNotification = notificationBuilderProvider.get();

        setDefaultConfiguration(completedNotification);
        completedNotification.setOngoing(false);
        completedNotification.setAutoCancel(true);
        completedNotification.setContentTitle(resources.getString(R.string.offline_update_completed_title));
        completedNotification.setContentText(resources.getString(R.string.offline_update_completed_message));
        return completedNotification.build();
    }

    private Notification buildErrorNotification() {
        final NotificationCompat.Builder errorNotification = notificationBuilderProvider.get();

        setDefaultConfiguration(errorNotification);
        errorNotification.setOngoing(false);
        errorNotification.setAutoCancel(true);
        errorNotification.setContentTitle(resources.getString(R.string.offline_update_error_title));
        errorNotification.setContentText(resources.getString(R.string.offline_update_error_message));
        return errorNotification.build();
    }

    private Notification updateProgressNotification() {
        final int currentDownload = completedDownloads + 1;
        final String downloadDescription = resources.getQuantityString(R.plurals.downloading_track_of_tracks, totalDownloads, currentDownload, totalDownloads);

        setDefaultConfiguration(progressNotification);
        progressNotification.setOngoing(true);
        progressNotification.setContentTitle(resources.getString(R.string.offline_update_in_progress));
        progressNotification.setProgress(totalDownloads, completedDownloads, false);
        progressNotification.setContentText(downloadDescription);

        return progressNotification.build();
    }

    private void setDefaultConfiguration(NotificationCompat.Builder builder) {
        builder.setSmallIcon(R.drawable.ic_notification_cloud);
        builder.setContentIntent(getPendingIntent());
        builder.setVisibility(NotificationCompat.VISIBILITY_PUBLIC);
    }

    private PendingIntent getPendingIntent() {
        return PendingIntent.getActivity(context, 0, getIntent(),
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_ONE_SHOT);
    }

    private Intent getIntent() {
        return new Intent(Actions.STREAM)
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
    }
}
