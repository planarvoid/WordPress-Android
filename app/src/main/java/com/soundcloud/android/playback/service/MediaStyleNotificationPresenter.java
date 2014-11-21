package com.soundcloud.android.playback.service;

import com.soundcloud.android.R;
import com.soundcloud.propeller.PropertySet;

import android.annotation.TargetApi;
import android.app.Notification;
import android.content.Context;
import android.support.v4.app.NotificationCompat;

import javax.inject.Provider;

public class MediaStyleNotificationPresenter extends PlaybackNotificationPresenter {

    public MediaStyleNotificationPresenter(Context context, Provider<NotificationCompat.Builder> builderProvider) {
        super(context, builderProvider);
    }

    @TargetApi(21)
    @Override
    Notification createNotification(PropertySet trackProperties) {
        final NotificationTrack trackViewModel = new NotificationTrack(context.getResources(), trackProperties);
        final Notification.Builder builder = new Notification.Builder(context);
        builder.setOngoing(true);
        builder.setSmallIcon(R.drawable.ic_notification_cloud);
        builder.setContentIntent(createPendingIntent(context));
        builder.setContentTitle(trackViewModel.getTitle());
        builder.setContentText(trackViewModel.getCreatorName());
        builder.setStyle(new Notification.MediaStyle());
        return builder.build();
    }

}
