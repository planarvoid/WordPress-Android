package com.soundcloud.android.playback.service;

import com.soundcloud.android.playback.views.NotificationPlaybackRemoteViews;
import com.soundcloud.android.playback.views.PlaybackRemoteViews;
import com.soundcloud.propeller.PropertySet;
import rx.Observable;
import rx.Subscriber;
import rx.functions.Func1;

import android.app.Notification;
import android.content.Context;
import android.graphics.Bitmap;
import android.support.v4.app.NotificationCompat;

import javax.inject.Provider;

public class RichNotificationPresenter extends PlaybackNotificationPresenter {

    private final NotificationPlaybackRemoteViews.Factory remoteViewsFactory;

    public RichNotificationPresenter(Context context, NotificationPlaybackRemoteViews.Factory remoteViewsFactory,
                                     Provider<NotificationCompat.Builder> builderProvider) {

        super(context, builderProvider);
        this.remoteViewsFactory = remoteViewsFactory;
    }

    Notification createNotification(PropertySet trackProperties) {
        NotificationTrack trackViewModel = new NotificationTrack(getContext().getResources(), trackProperties);
        Notification notification = super.createNotification(trackProperties);

        final NotificationPlaybackRemoteViews playbackRemoteViews = remoteViewsFactory.create(getContext().getPackageName());
        playbackRemoteViews.linkButtonsNotification(getContext());
        playbackRemoteViews.setCurrentTrackTitle(trackViewModel.getTitle());
        playbackRemoteViews.setCurrentUsername(trackViewModel.getCreatorName());
        notification.contentView = playbackRemoteViews;
        return notification;
    }

    public boolean artworkCapable() {
        return true;
    }

    void setIcon(Notification notification, Bitmap bitmap) {
        ((NotificationPlaybackRemoteViews) notification.contentView).setIcon(bitmap);
    }

    void clearIcon(Notification notification) {
        ((NotificationPlaybackRemoteViews) notification.contentView).clearIcon();
    }

    @Override
    Func1<Notification, Notification> updateToPlayingState() {
        return updatePlaybackStatusFunc(true);
    }

    @Override
    boolean updateToIdleState(Observable<Notification> notificationObservable, final Subscriber<Notification> notificationSubscriber) {
        notificationObservable.map(updatePlaybackStatusFunc(false)).subscribe(notificationSubscriber);
        return true;
    }

    private Func1<Notification, Notification> updatePlaybackStatusFunc(final boolean isPlaying) {
        return new Func1<Notification, Notification>() {
            @Override
            public Notification call(Notification notification) {
                setPlaybackStatus(notification, isPlaying);
                return notification;
            }
        };
    }

    protected void setPlaybackStatus(Notification notification, boolean isPlaying) {
        ((PlaybackRemoteViews) notification.contentView).setPlaybackStatus(isPlaying);
    }

    NotificationPlaybackRemoteViews.Factory getRemoteViewsFactory() {
        return remoteViewsFactory;
    }


}
