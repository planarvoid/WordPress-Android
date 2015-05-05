package com.soundcloud.android.playback.notification;

import com.soundcloud.android.R;
import com.soundcloud.android.analytics.Referrer;
import com.soundcloud.android.analytics.Screen;
import com.soundcloud.android.main.MainActivity;
import com.soundcloud.android.playback.service.NotificationTrack;
import com.soundcloud.android.playback.ui.SlidingPlayerController;
import com.soundcloud.propeller.PropertySet;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;

import javax.inject.Inject;

public class PlaybackNotificationPresenter {

    protected final Context context;

    @Inject
    public PlaybackNotificationPresenter(Context context) {
        this.context = context;
    }

    void init(NotificationBuilder builder, boolean isPlaying) {
        builder.setSmallIcon(R.drawable.ic_notification_cloud);
        builder.setContentIntent(createPendingIntent(context));
        builder.setPlayingStatus(isPlaying);
    }

    void updateTrackInfo(NotificationBuilder notificationBuilder, PropertySet trackProperties) {
        final NotificationTrack trackViewModel = new NotificationTrack(context.getResources(), trackProperties);
        notificationBuilder.setTrackTitle(trackViewModel.getTitle());
        notificationBuilder.setCreatorName(trackViewModel.getCreatorName());
    }

    void updateToPlayingState(NotificationBuilder notificationBuilder) {
        updatePlaybackStatusFunc(notificationBuilder, true);
    }

    void updateToIdleState(NotificationBuilder notificationBuilder) {
        updatePlaybackStatusFunc(notificationBuilder, false);
    }

    private void updatePlaybackStatusFunc(NotificationBuilder notificationBuilder, final boolean isPlaying) {
        notificationBuilder.setPlayingStatus(isPlaying);
    }

    protected PendingIntent createPendingIntent(Context context) {
        Intent intent = new Intent(context, MainActivity.class)
                .addFlags(Intent.FLAG_ACTIVITY_BROUGHT_TO_FRONT)
                .putExtra(SlidingPlayerController.EXTRA_EXPAND_PLAYER, true);

        Screen.NOTIFICATION.addToIntent(intent);
        Referrer.PLAYBACK_NOTIFICATION.addToIntent(intent);

        return PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_CANCEL_CURRENT);
    }
}
