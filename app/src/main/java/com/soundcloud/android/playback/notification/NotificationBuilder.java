package com.soundcloud.android.playback.notification;

import android.app.Notification;
import android.app.PendingIntent;
import android.graphics.Bitmap;

public interface NotificationBuilder {
    void setOngoing(boolean isOngoing);

    void setIcon(Bitmap bitmap);

    void setSmallIcon(int icon);

    void clearIcon();

    void setContentIntent(PendingIntent pendingIntent);

    void setTrackTitle(String title);

    void setHeader(String creatorName);

    void setPlayingStatus(boolean isPlaying);

    boolean hasPlayStateSupport();

    boolean hasArtworkSupport();

    Notification build();
}
