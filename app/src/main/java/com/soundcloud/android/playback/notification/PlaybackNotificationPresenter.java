package com.soundcloud.android.playback.notification;

import com.soundcloud.android.R;
import com.soundcloud.android.cast.CastConnectionHelper;
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
    private final CastConnectionHelper castConnectionHelper;
    private NotificationTrack trackViewModel;

    @Inject
    public PlaybackNotificationPresenter(Context context, CastConnectionHelper castConnectionHelper) {
        this.context = context;
        this.castConnectionHelper = castConnectionHelper;
    }

    void init(NotificationBuilder builder, boolean isPlaying) {
        builder.setSmallIcon(R.drawable.ic_notification_cloud);
        builder.setContentIntent(createPendingIntent(context));
        builder.setPlayingStatus(isPlaying);
    }

    void updateTrackInfo(NotificationBuilder notificationBuilder, PropertySet trackProperties) {
        trackViewModel = new NotificationTrack(context.getResources(), trackProperties);
        notificationBuilder.setTrackTitle(trackViewModel.getTitle());
        configureCastMode(notificationBuilder);

    }

    void updateToPlayingState(NotificationBuilder notificationBuilder) {
        updatePlaybackStatusFunc(notificationBuilder, true);
        configureCastMode(notificationBuilder);
    }

    void updateToIdleState(NotificationBuilder notificationBuilder) {
        updatePlaybackStatusFunc(notificationBuilder, false);
        configureCastMode(notificationBuilder);
    }

    private void configureCastMode(NotificationBuilder notificationBuilder) {
        if (castConnectionHelper.isConnected()) {
            notificationBuilder.setHeader(context.getString(R.string.casting_to_device, castConnectionHelper.getCastingDeviceName()));
        } else {
            notificationBuilder.setHeader(trackViewModel.getCreatorName());
        }
    }

    private void updatePlaybackStatusFunc(NotificationBuilder notificationBuilder, final boolean isPlaying) {
        notificationBuilder.setPlayingStatus(isPlaying);
    }

    protected PendingIntent createPendingIntent(Context context) {
        Intent intent = new Intent(context, MainActivity.class)
                .addFlags(Intent.FLAG_ACTIVITY_BROUGHT_TO_FRONT)
                .putExtra(SlidingPlayerController.EXTRA_EXPAND_PLAYER, true);
        return PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_CANCEL_CURRENT);
    }
}
