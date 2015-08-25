package com.soundcloud.android.offline;

import static com.soundcloud.android.offline.DownloadOperations.ConnectionState;

import com.soundcloud.android.Actions;
import com.soundcloud.android.NotificationConstants;
import com.soundcloud.android.R;
import com.soundcloud.android.analytics.Screen;
import com.soundcloud.android.main.MainActivity;
import com.soundcloud.android.playlists.PlaylistDetailActivity;
import com.soundcloud.android.settings.OfflineSettingsActivity;
import com.soundcloud.java.collections.Iterables;
import com.soundcloud.java.collections.MoreCollections;
import com.soundcloud.java.functions.Predicate;
import com.soundcloud.java.objects.MoreObjects;
import org.jetbrains.annotations.Nullable;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.support.annotation.VisibleForTesting;
import android.support.v4.app.NotificationCompat;

import javax.inject.Inject;
import javax.inject.Provider;
import java.util.ArrayList;
import java.util.List;

class DownloadNotificationController {
    @VisibleForTesting
    static final int PROGRESS_MAX = 1000;

    private final Context context;
    private final Resources resources;
    private final NotificationManager notificationManager;
    private final Provider<NotificationCompat.Builder> notificationBuilderProvider;


    private int totalDownloads;
    private int totalBytesToDownload;
    private long completedBytes;
    private List<DownloadState> previousDownloads = new ArrayList<>();
    private DownloadState currentDownload;
    private NotificationCompat.Builder progressNotification;
    private ProgressNotificationData lastProgressNotificationData;

    @Inject
    public DownloadNotificationController(Context context, NotificationManager notificationManager,
                                          Provider<NotificationCompat.Builder> notificationBuilderProvider,
                                          Resources resources) {
        this.context = context;
        this.resources = resources;
        this.notificationManager = notificationManager;
        this.notificationBuilderProvider = notificationBuilderProvider;
    }

    public Notification onPendingRequests(DownloadQueue pendingQueue) {
        final int pendingAndCompleted = pendingQueue.size() + previousDownloads.size();
        totalDownloads = currentDownload == null ? pendingAndCompleted : pendingAndCompleted + 1;
        totalBytesToDownload = (int) (currentDownload == null ? completedBytes : completedBytes + currentDownload.getTotalBytes());

        for (DownloadRequest request : pendingQueue.getRequests()){
            totalBytesToDownload += SecureFileStorage.calculateFileSizeInBytes(request.duration);
        }

        progressNotification = notificationBuilderProvider.get();

        if (currentDownload == null){
            return updateProgressNotification(pendingQueue.getFirst(), new ProgressNotificationData(previousDownloads.size() + 1,
                    totalDownloads, calculateAdjustedProgress((int) completedBytes, totalBytesToDownload)));
        } else {
            return updateProgressNotification(currentDownload.request, new ProgressNotificationData(previousDownloads.size() + 1,
                    totalDownloads, calculateAdjustedProgress((int) (completedBytes + currentDownload.getProgress()), totalBytesToDownload)));
        }
    }

    public void onDownloadProgress(DownloadState currentDownload) {
        this.currentDownload = currentDownload;
        updateProgressNotificationIfChanged(currentDownload);
    }

    public void onDownloadSuccess(DownloadState lastDownload) {
        currentDownload = null;
        previousDownloads.add(lastDownload);
        updateProgressNotificationIfChanged(lastDownload);
        completedBytes += lastDownload.getTotalBytes();
    }

    public void onDownloadError(DownloadState lastDownload) {
        // we want to show this as completed in the progress bar, even though it failed
        completedBytes += lastDownload.getTotalBytes();
        currentDownload = null;
        previousDownloads.add(lastDownload);
        updateProgressNotificationIfChanged(lastDownload);
    }

    public void onDownloadCancel(DownloadState cancelled) {
        if (totalDownloads > 0) {
            totalDownloads--;
            updateProgressNotificationIfChanged(cancelled);
        }
    }

    public void onDownloadsFinished(@Nullable DownloadState lastDownload) {
        if (hasStorageErrors()) {
            notificationManager.notify(NotificationConstants.OFFLINE_NOTIFY_ID,
                    completedWithStorageErrorsNotification());
        } else if (lastDownload != null && totalDownloads != getErrorCount()) {
            notificationManager.notify(NotificationConstants.OFFLINE_NOTIFY_ID,
                    completedNotification(lastDownload.request));
        } else {
            notificationManager.cancel(NotificationConstants.OFFLINE_NOTIFY_ID);
        }

        totalDownloads = 0;
        completedBytes = 0;
        previousDownloads = new ArrayList<>();
    }

    private int getErrorCount() {
        return MoreCollections.filter(previousDownloads, new Predicate<DownloadState>() {
            @Override
            public boolean apply(DownloadState downloadState) {
                return downloadState.isConnectionError()
                        || downloadState.isDownloadFailed()
                        || downloadState.isUnavailable();
            }
        }).size();
    }

    private boolean hasStorageErrors() {
        return Iterables.tryFind(previousDownloads, new Predicate<DownloadState>() {
            @Override
            public boolean apply(DownloadState downloadState) {
                return downloadState.isNotEnoughSpace();
            }
        }).isPresent();
    }

    public void onConnectionError(DownloadState lastDownload) {
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

    private void updateProgressNotificationIfChanged(DownloadState currentDownload) {
        ProgressNotificationData newProgressNotificationData = new ProgressNotificationData(previousDownloads.size() + 1, totalDownloads,
                calculateAdjustedProgress((int) (completedBytes + currentDownload.getProgress()), totalBytesToDownload));

        // this logic is here to throttle notifications spamming, and only update when the notification changed.
        if (!newProgressNotificationData.equals(lastProgressNotificationData)) {
            lastProgressNotificationData = newProgressNotificationData;
            notificationManager.notify(NotificationConstants.OFFLINE_NOTIFY_ID,
                    updateProgressNotification(currentDownload.request, newProgressNotificationData));
        }
    }

    private int calculateAdjustedProgress(float downloadedBytes, int totalBytesToDownload) {
        return (int) (PROGRESS_MAX * downloadedBytes / totalBytesToDownload);
    }

    private Notification updateProgressNotification(DownloadRequest request, ProgressNotificationData notificationData) {

        final String downloadDescription = resources
                .getQuantityString(R.plurals.downloading_track_of_tracks, notificationData.totalDownloads,
                        notificationData.currentDownload, notificationData.totalDownloads);

        progressNotification.setSmallIcon(R.drawable.ic_notification_cloud);
        progressNotification.setVisibility(NotificationCompat.VISIBILITY_PUBLIC);
        progressNotification.setOngoing(true);

        progressNotification.setContentIntent(getPendingIntent(request));
        progressNotification.setContentTitle(resources.getString(R.string.offline_update_in_progress));
        progressNotification.setProgress(PROGRESS_MAX, notificationData.downloadProgress, false);
        progressNotification.setContentText(downloadDescription);
        return progressNotification.build();
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

    private static class ProgressNotificationData {
        private final int currentDownload;
        private final int totalDownloads;
        private final int downloadProgress;

        private ProgressNotificationData(int currentDownload, int totalDownloads, int downloadProgress) {
            // We display the current download as completed + 1, so display total as current when last download completes
            this.currentDownload = Math.min(currentDownload, totalDownloads);
            this.totalDownloads = totalDownloads;
            this.downloadProgress = downloadProgress;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof ProgressNotificationData)) {
                return false;
            }
            ProgressNotificationData that = (ProgressNotificationData) o;
            return MoreObjects.equal(currentDownload, that.currentDownload) &&
                    MoreObjects.equal(totalDownloads, that.totalDownloads) &&
                    MoreObjects.equal(downloadProgress, that.downloadProgress);
        }

        @Override
        public int hashCode() {
            return MoreObjects.hashCode(currentDownload, totalDownloads, downloadProgress);
        }
    }
}
