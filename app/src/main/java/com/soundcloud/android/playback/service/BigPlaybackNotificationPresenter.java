package com.soundcloud.android.playback.service;

import com.soundcloud.android.R;
import com.soundcloud.android.model.Track;
import com.soundcloud.android.playback.views.NotificationPlaybackRemoteViews;
import com.soundcloud.android.playback.views.PlaybackRemoteViews;

import android.annotation.TargetApi;
import android.app.Notification;
import android.content.Context;
import android.net.Uri;
import android.os.Build;
import android.support.v4.app.NotificationCompat;

import javax.inject.Provider;

@TargetApi(Build.VERSION_CODES.JELLY_BEAN)
public class BigPlaybackNotificationPresenter extends RichNotificationPresenter {

    public BigPlaybackNotificationPresenter(Context context, NotificationPlaybackRemoteViews.Factory remoteViewsFactory,
                                            Provider<NotificationCompat.Builder> builder) {
        super(context, remoteViewsFactory, builder);
    }

    @Override
    Notification createNotification(Track track){
        Notification notification = super.createNotification(track);

        final String packageName = getContext().getPackageName();
        final NotificationPlaybackRemoteViews bigRemoteViews = getRemoteViewsFactory().create(packageName, R.layout.playback_status_large_v16);
        bigRemoteViews.linkButtonsNotification(getContext());
        bigRemoteViews.setCurrentTrackTitle(track.getTitle());
        bigRemoteViews.setCurrentUsername(track.getUsername());
        notification.bigContentView = bigRemoteViews;
        return notification;
    }

    @Override
    void setIcon(Notification notification, Uri bitmapUri) {
        ((NotificationPlaybackRemoteViews) notification.bigContentView).setIcon(bitmapUri);
    }

    @Override
    void clearIcon(Notification notification){
        ((NotificationPlaybackRemoteViews) notification.bigContentView).clearIcon();
    }

    @Override
    protected void setPlaybackStatus(Notification notification, boolean isPlaying) {
        super.setPlaybackStatus(notification, isPlaying);
        ((PlaybackRemoteViews) notification.bigContentView).setPlaybackStatus(isPlaying);
    }
}
