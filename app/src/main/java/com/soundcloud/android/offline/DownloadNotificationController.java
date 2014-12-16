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

    public Notification create() {
        progressNotification = notificationBuilderProvider.get();

        setDefaultConfiguration(progressNotification);
        progressNotification.setOngoing(true);
        progressNotification.setContentTitle(resources.getString(R.string.downloads_started));
        return progressNotification.build();
    }

    public void onNewPendingRequests(int pending) {
        this.totalDownloads = completedDownloads + pending;
        update();
    }

    public void onProgressUpdate() {
        this.completedDownloads++;
        update();
    }

    public void onCompleted() {
        completedDownloads = 0;
        totalDownloads = 0;

        final NotificationCompat.Builder completedNotification = notificationBuilderProvider.get();

        setDefaultConfiguration(completedNotification);
        completedNotification.setOngoing(false);
        completedNotification.setAutoCancel(true);
        completedNotification.setContentTitle(resources.getString(R.string.downloads_completed));
        notificationManager.notify(NotificationConstants.OFFLINE_NOTIFY_ID, completedNotification.build());
    }

    private void update() {
        final String downloadDescription = resources.getQuantityString(R.plurals.downloading_track_of_tracks, totalDownloads, completedDownloads, totalDownloads);

        setDefaultConfiguration(progressNotification);
        progressNotification.setOngoing(true);
        progressNotification.setContentTitle(resources.getString(R.string.downloads_in_progress));
        progressNotification.setProgress(totalDownloads, completedDownloads, false);
        progressNotification.setContentText(downloadDescription);
        notificationManager.notify(NotificationConstants.OFFLINE_NOTIFY_ID, progressNotification.build());
    }

    private void setDefaultConfiguration(NotificationCompat.Builder builder) {
        builder.setSmallIcon(R.drawable.ic_notification_cloud);
        builder.setContentIntent(getPendingIntent());
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
