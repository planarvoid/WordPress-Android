package com.soundcloud.android.playback.notification;

import com.soundcloud.android.playback.views.NotificationPlaybackRemoteViews;

import android.app.Notification;
import android.app.PendingIntent;
import android.content.Context;
import android.graphics.Bitmap;
import android.support.v4.app.NotificationCompat;

import javax.inject.Inject;

public class RichNotificationBuilder implements NotificationBuilder {
    private final NotificationCompat.Builder builder;
    private final NotificationPlaybackRemoteViews remoteViews;

    @Inject
    public RichNotificationBuilder(Context context, NotificationPlaybackRemoteViews.Factory remoteViewsFactory) {
        builder = new NotificationCompat.Builder(context);
        builder.setOngoing(true);
        remoteViews = remoteViewsFactory.create(context.getPackageName());
        remoteViews.linkButtonsNotification(context);
    }

    @Override
    public void setOngoing(boolean isOngoing) {
        builder.setOngoing(isOngoing);
    }

    @Override
    public void setSmallIcon(int icon) {
        builder.setSmallIcon(icon);
    }

    @Override
    public void setIcon(Bitmap icon) {
        remoteViews.setIcon(icon);
    }

    @Override
    public void clearIcon() {
        remoteViews.setIcon(null);
    }

    @Override
    public void setContentIntent(PendingIntent pendingIntent) {
        builder.setContentIntent(pendingIntent);
    }

    @Override
    public void setTrackTitle(String title) {
        remoteViews.setCurrentTrackTitle(title);
    }

    @Override
    public void setHeader(String creator) {
        remoteViews.setCurrentCreator(creator);
    }

    @Override
    public void setPlayingStatus(boolean isPlaying) {
        remoteViews.setPlaybackStatus(isPlaying);
    }

    @Override
    public boolean hasPlayStateSupport() {
        return true;
    }

    @Override
    public boolean hasArtworkSupport() {
        return true;
    }

    @Override
    public Notification build() {
        Notification build = builder.build();
        build.contentView = remoteViews;
        return build;
    }
}
