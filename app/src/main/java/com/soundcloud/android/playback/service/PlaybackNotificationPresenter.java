package com.soundcloud.android.playback.service;

import com.soundcloud.android.R;
import com.soundcloud.android.model.PlayableProperty;
import com.soundcloud.android.model.PropertySet;
import rx.Observable;
import rx.functions.Action1;
import rx.functions.Func1;
import rx.functions.Functions;

import android.app.Notification;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.support.v4.app.NotificationCompat;

import javax.inject.Provider;

public class PlaybackNotificationPresenter {

    private final Provider<NotificationCompat.Builder> builderProvider;
    private final Context context;

    public PlaybackNotificationPresenter(Context context,
                                         Provider<NotificationCompat.Builder> builderProvider) {
        this.context = context;
        this.builderProvider = builderProvider;
    }

    Notification createNotification(PropertySet propertySet){
        final NotificationCompat.Builder builder = builderProvider.get();
        builder.setOngoing(true);
        builder.setSmallIcon(R.drawable.ic_notification_cloud);
        builder.setContentIntent(createPendingIntent(context));
        builder.setContentTitle(propertySet.get(PlayableProperty.TITLE));
        builder.setContentText(propertySet.get(PlayableProperty.CREATOR));
        return builder.build();
    }

    public boolean artworkCapable() {
        return false;
    }

    void setIcon(Notification notification, Uri bitmapUri) {
        // no-op, overridetn in RichNotificationPresenter
    }

    void clearIcon(Notification notification){
        // no-op, overridetn in RichNotificationPresenter
    }

    Func1<Notification, Notification> updateToPlayingState() {
        return Functions.identity();
    }

    boolean updateToIdleState(Observable<Notification> notificationObservable, Action1<Notification> notifyAction) {
        return false;
    }

    Context getContext(){
        return context;
    }

    private PendingIntent createPendingIntent(Context context) {
        Intent intent = new Intent(com.soundcloud.android.Actions.PLAYER)
                .addFlags(Intent.FLAG_ACTIVITY_BROUGHT_TO_FRONT);
        return PendingIntent.getActivity(context, 0, intent, 0);
    }
}
