package com.soundcloud.android.playback.notification;

import android.annotation.TargetApi;
import android.app.Notification;
import android.app.PendingIntent;
import android.content.Context;
import android.graphics.Bitmap;
import android.os.Build;

@TargetApi(Build.VERSION_CODES.LOLLIPOP)
public class MediaStyleNotificationBuilder implements NotificationBuilder {
    private final Notification.Builder builder;

    public MediaStyleNotificationBuilder(Context context) {
        builder = new Notification.Builder(context);
        builder.setStyle(new Notification.MediaStyle());
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
        builder.setLargeIcon(icon);
    }

    @Override
    public void clearIcon() {
        builder.setLargeIcon(null);
    }

    @Override
    public void setContentIntent(PendingIntent pendingIntent) {
        builder.setContentIntent(pendingIntent);
    }

    @Override
    public void setContentTitle(String title) {
        builder.setContentTitle(title);
    }

    @Override
    public void setContentText(String creatorName) {
        builder.setContentText(creatorName);
    }

    @Override
    public void setPlayingStatus(boolean isPlaying) {
        // TODO: Change play/pause action
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
        return builder.build();
    }
}
