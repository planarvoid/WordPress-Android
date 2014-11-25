package com.soundcloud.android.playback.notification;

import com.soundcloud.android.R;
import com.soundcloud.android.playback.views.NotificationPlaybackRemoteViews;

import android.annotation.TargetApi;
import android.app.Notification;
import android.app.PendingIntent;
import android.content.Context;
import android.graphics.Bitmap;
import android.os.Build;
import android.support.v4.app.NotificationCompat;

import javax.inject.Inject;

@TargetApi(Build.VERSION_CODES.JELLY_BEAN)
public class BigNotificationBuilder implements NotificationBuilder {
    private final NotificationCompat.Builder builder;
    private final NotificationPlaybackRemoteViews bigRemoteViews;
    private final NotificationPlaybackRemoteViews smallRemoteViews;

    @Inject
    public BigNotificationBuilder(Context context, NotificationPlaybackRemoteViews.Factory remoteViewsFactory) {
        builder = new NotificationCompat.Builder(context);
        smallRemoteViews = remoteViewsFactory.create(context.getPackageName());
        smallRemoteViews.linkButtonsNotification(context);
        bigRemoteViews = remoteViewsFactory.create(context.getPackageName(), R.layout.playback_status_large_v16);
        bigRemoteViews.linkButtonsNotification(context);
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
        smallRemoteViews.setIcon(icon);
        bigRemoteViews.setIcon(icon);
    }

    @Override
    public void clearIcon() {
        smallRemoteViews.setIcon(null);
        bigRemoteViews.setIcon(null);
    }

    @Override
    public void setContentIntent(PendingIntent pendingIntent) {
        builder.setContentIntent(pendingIntent);
    }

    @Override
    public void setContentTitle(String title) {
        smallRemoteViews.setCurrentTrackTitle(title);
        bigRemoteViews.setCurrentTrackTitle(title);
    }

    @Override
    public void setContentText(String creator) {
        smallRemoteViews.setCurrentCreator(creator);
        bigRemoteViews.setCurrentCreator(creator);
    }

    @Override
    public void setPlayingStatus(boolean isPlaying) {
        smallRemoteViews.setPlaybackStatus(isPlaying);
        bigRemoteViews.setPlaybackStatus(isPlaying);
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
        build.contentView = smallRemoteViews;
        build.bigContentView = bigRemoteViews;
        return build;
    }
}
