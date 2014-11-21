package com.soundcloud.android.playback.service;

import com.soundcloud.android.R;
import com.soundcloud.android.main.MainActivity;
import com.soundcloud.android.playback.ui.SlidingPlayerController;
import com.soundcloud.propeller.PropertySet;
import rx.Observable;
import rx.Subscriber;
import rx.functions.Func1;
import rx.internal.util.UtilityFunctions;

import android.app.Notification;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.support.v4.app.NotificationCompat;

import javax.inject.Provider;

public class PlaybackNotificationPresenter {

    protected final Provider<NotificationCompat.Builder> builderProvider;
    protected final Context context;

    public PlaybackNotificationPresenter(Context context,
                                         Provider<NotificationCompat.Builder> builderProvider) {
        this.context = context;
        this.builderProvider = builderProvider;
    }

    Notification createNotification(PropertySet trackProperties){
        final NotificationTrack trackViewModel = new NotificationTrack(context.getResources(), trackProperties);

        final NotificationCompat.Builder builder = builderProvider.get();
        builder.setOngoing(true);
        builder.setSmallIcon(R.drawable.ic_notification_cloud);
        builder.setContentIntent(createPendingIntent(context));
        builder.setContentTitle(trackViewModel.getTitle());
        builder.setContentText(trackViewModel.getCreatorName());
        return builder.build();
    }

    public boolean artworkCapable() {
        return false;
    }

    void setIcon(Notification notification, Bitmap bitmap) {
        // no-op, overridetn in RichNotificationPresenter
    }

    void clearIcon(Notification notification){
        // no-op, overridetn in RichNotificationPresenter
    }

    Func1<Notification, Notification> updateToPlayingState() {
        return UtilityFunctions.identity();
    }

    boolean updateToIdleState(Observable<Notification> notificationObservable, Subscriber<Notification> notificationSubscriber) {
        return false;
    }

    Context getContext(){
        return context;
    }

    protected PendingIntent createPendingIntent(Context context) {
        Intent intent = new Intent(context, MainActivity.class)
                .addFlags(Intent.FLAG_ACTIVITY_BROUGHT_TO_FRONT)
                .putExtra(SlidingPlayerController.EXTRA_EXPAND_PLAYER, true);
        return PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_CANCEL_CURRENT);
    }
}
