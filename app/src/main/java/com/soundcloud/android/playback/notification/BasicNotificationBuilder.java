package com.soundcloud.android.playback.notification;

import com.soundcloud.android.image.ApiImageSize;

import android.app.Notification;
import android.app.PendingIntent;
import android.content.Context;
import android.graphics.Bitmap;
import android.support.v4.app.NotificationCompat;

import javax.inject.Inject;

public class BasicNotificationBuilder implements NotificationBuilder {
    private final NotificationCompat.Builder builder;

    @Inject
    public BasicNotificationBuilder(Context context) {
        builder = new NotificationCompat.Builder(context);
        builder.setOngoing(true);
        builder.setVisibility(NotificationCompat.VISIBILITY_PUBLIC);
    }

    @Override
    public void setSmallIcon(int icon) {
        builder.setSmallIcon(icon);
    }

    @Override
    public void setIcon(Bitmap icon) {
        // no-op
    }

    @Override
    public void clearIcon() {
        // no-op
    }

    @Override
    public void setContentIntent(PendingIntent pendingIntent) {
        builder.setContentIntent(pendingIntent);
    }

    @Override
    public void setTrackTitle(String title) {
        builder.setContentTitle(title);
    }

    @Override
    public void setCreatorName(String creatorName) {
        builder.setContentText(creatorName);
    }

    @Override
    public void setPlayingStatus(boolean isPlaying) {
        // no-op
    }

    @Override
    public boolean hasPlayStateSupport() {
        return false;
    }

    @Override
    public boolean hasArtworkSupport() {
        return false;
    }

    @Override
    public ApiImageSize getImageSize() {
        return ApiImageSize.Unknown;
    }

    @Override
    public int getTargetImageSize() {
        return NOT_SET;
    }

    @Override
    public Notification build() {
        return builder.build();
    }
}
