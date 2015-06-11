package com.soundcloud.android.offline;

import static com.soundcloud.android.offline.DownloadOperations.ConnectionState;

import com.soundcloud.android.Actions;
import com.soundcloud.android.NotificationConstants;
import com.soundcloud.android.R;
import com.soundcloud.android.analytics.Screen;
import com.soundcloud.android.main.MainActivity;
import com.soundcloud.android.playlists.PlaylistDetailActivity;
import com.soundcloud.android.settings.OfflineSettingsActivity;
import org.jetbrains.annotations.Nullable;

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
    private int completed;
    private int errors;
    private int storageErrors;
    private int totalDownloads;

    @Inject
    public DownloadNotificationController(Context context, NotificationManager notificationManager,
                                          Provider<NotificationCompat.Builder> notificationBuilderProvider,
                                          Resources resources) {
        this.context = context;
        this.resources = resources;
        this.notificationManager = notificationManager;
        this.notificationBuilderProvider = notificationBuilderProvider;
    }

    public Notification onPendingRequests(int pending, DownloadRequest firstRequest) {
        totalDownloads = completed + pending + errors + storageErrors;
        progressNotification = notificationBuilderProvider.get();

        return updateProgressNotification(firstRequest);
    }

    public void onDownloadSuccess(DownloadResult lastDownload) {
        completed++;
        notificationManager.notify(NotificationConstants.OFFLINE_NOTIFY_ID,
                updateProgressNotification(lastDownload.request));
    }

    public void onDownloadError(DownloadResult lastDownload) {
        if (lastDownload.isNotEnoughSpace()) {
            storageErrors++;
        } else {
            errors++;
        }
        notificationManager.notify(NotificationConstants.OFFLINE_NOTIFY_ID,
                updateProgressNotification(lastDownload.request));
    }

    public void onDownloadCancel(DownloadResult cancelled) {
        if (completed > 0) {
            completed--;
        }

        if (totalDownloads > 0) {
            totalDownloads--;
            notificationManager.notify(NotificationConstants.OFFLINE_NOTIFY_ID,
                    updateProgressNotification(cancelled.request));
        }
    }

    public void onDownloadsFinished(@Nullable DownloadResult lastDownload) {
        if (storageErrors > 0) {
            notificationManager.notify(NotificationConstants.OFFLINE_NOTIFY_ID,
                    completedWithStorageErrorsNotification());
        } else if (lastDownload != null && totalDownloads != errors) {
            notificationManager.notify(NotificationConstants.OFFLINE_NOTIFY_ID,
                    completedNotification(lastDownload.request));
        } else {
            notificationManager.cancel(NotificationConstants.OFFLINE_NOTIFY_ID);
        }

        completed = 0;
        totalDownloads = 0;
        storageErrors = errors = 0;
    }

    public void onConnectionError(DownloadResult lastDownload) {
        final NotificationCompat.Builder notification = buildBaseCompletedNotification();

        notification.setContentIntent(getPendingIntent(lastDownload.request));
        notification.setContentTitle(resources.getString(R.string.offline_update_paused));
        notification.setContentText(
                resources.getString(lastDownload.connectionState == ConnectionState.DISCONNECTED ?
                        R.string.no_network_connection : R.string.no_wifi_connection));

        notificationManager.notify(NotificationConstants.OFFLINE_NOTIFY_ID, notification.build());
    }

    private Notification completedWithStorageErrorsNotification() {
        final NotificationCompat.Builder notification = buildBaseCompletedNotification();

        notification.setContentIntent(getSettingsIntent());
        notification.setContentTitle(resources.getString(R.string.offline_update_storage_limit_reached_title));
        notification.setContentText(resources.getString(R.string.offline_update_storage_limit_reached_message));
        return notification.build();
    }

    private Notification completedNotification(DownloadRequest request) {
        final NotificationCompat.Builder notification = buildBaseCompletedNotification();

        notification.setContentIntent(getPendingIntent(request));
        notification.setContentTitle(resources.getString(R.string.offline_update_completed_title));
        notification.setContentText(resources.getString(R.string.offline_update_completed_message));
        return notification.build();
    }

    private Notification updateProgressNotification(DownloadRequest request) {
        final int currentDownload = getCurrentPosition() + 1;
        final String downloadDescription = resources
                .getQuantityString(R.plurals.downloading_track_of_tracks, totalDownloads, currentDownload, totalDownloads);

        progressNotification.setSmallIcon(R.drawable.ic_notification_cloud);
        progressNotification.setVisibility(NotificationCompat.VISIBILITY_PUBLIC);
        progressNotification.setOngoing(true);

        progressNotification.setContentIntent(getPendingIntent(request));
        progressNotification.setContentTitle(resources.getString(R.string.offline_update_in_progress));
        progressNotification.setProgress(totalDownloads, getCurrentPosition(), false);
        progressNotification.setContentText(downloadDescription);

        return progressNotification.build();
    }

    private int getCurrentPosition() {
        return completed + errors;
    }

    private NotificationCompat.Builder buildBaseCompletedNotification() {
        final NotificationCompat.Builder builder = notificationBuilderProvider.get();

        builder.setSmallIcon(R.drawable.ic_notification_cloud);
        builder.setVisibility(NotificationCompat.VISIBILITY_PUBLIC);
        builder.setOngoing(false);
        builder.setAutoCancel(true);

        return builder;
    }

    private PendingIntent getPendingIntent(@Nullable DownloadRequest request) {
        Intent intent;
        if (request == null) {
            intent = new Intent(context, MainActivity.class);
        } else if (request.inPlaylists.isEmpty()) {
            intent = new Intent(Actions.LIKES);
        } else {
            intent = PlaylistDetailActivity.getIntent(request.inPlaylists.get(0), Screen.PLAYLIST_DETAILS);
        }

        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        return PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_CANCEL_CURRENT);
    }

    private PendingIntent getSettingsIntent() {
        final Intent intent = new Intent(context, OfflineSettingsActivity.class);
        return PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_CANCEL_CURRENT);
    }
}
