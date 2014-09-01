package com.soundcloud.android.playback.service;

import com.soundcloud.android.R;
import com.soundcloud.android.playback.views.NotificationPlaybackRemoteViews;
import com.soundcloud.android.playback.views.PlaybackRemoteViews;
import com.soundcloud.propeller.PropertySet;

import android.annotation.TargetApi;
import android.app.Notification;
import android.content.Context;
import android.graphics.Bitmap;
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
    Notification createNotification(PropertySet trackProperties){
        final NotificationTrack trackViewModel = new NotificationTrack(getContext().getResources(), trackProperties);
        final Notification notification = super.createNotification(trackProperties);

        final String packageName = getContext().getPackageName();
        final NotificationPlaybackRemoteViews bigRemoteViews = getRemoteViewsFactory().create(packageName, R.layout.playback_status_large_v16);
        bigRemoteViews.linkButtonsNotification(getContext());
        bigRemoteViews.setCurrentTrackTitle(trackViewModel.getTitle());
        bigRemoteViews.setCurrentUsername(trackViewModel.getCreatorName());
        notification.bigContentView = bigRemoteViews;
        return notification;
    }

    @Override
    void setIcon(Notification notification, Bitmap bitmap) {
        super.setIcon(notification, bitmap);
        ((NotificationPlaybackRemoteViews) notification.bigContentView).setIcon(bitmap);
    }

    @Override
    void clearIcon(Notification notification){
        super.clearIcon(notification);
        ((NotificationPlaybackRemoteViews) notification.bigContentView).clearIcon();
    }

    @Override
    protected void setPlaybackStatus(Notification notification, boolean isPlaying) {
        super.setPlaybackStatus(notification, isPlaying);
        ((PlaybackRemoteViews) notification.bigContentView).setPlaybackStatus(isPlaying);
    }
}
