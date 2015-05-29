package com.soundcloud.android.playback.notification;

import com.soundcloud.android.image.ApiImageSize;

import android.app.Notification;
import android.app.PendingIntent;
import android.graphics.Bitmap;

public interface NotificationBuilder {

    int NOT_SET = -1;

    void setIcon(Bitmap bitmap);

    void setSmallIcon(int icon);

    void clearIcon();

    void setContentIntent(PendingIntent pendingIntent);

    void setTrackTitle(String title);

    void setCreatorName(String creatorName);

    void setPlayingStatus(boolean isPlaying);

    boolean hasPlayStateSupport();

    boolean hasArtworkSupport();

    ApiImageSize getImageSize();

    int getTargetImageSize();

    Notification build();
}
