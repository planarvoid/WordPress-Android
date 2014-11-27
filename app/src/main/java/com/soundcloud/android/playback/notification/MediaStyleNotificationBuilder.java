package com.soundcloud.android.playback.notification;

import com.soundcloud.android.R;
import com.soundcloud.android.events.PlayControlEvent;
import com.soundcloud.android.playback.external.PlaybackAction;

import android.annotation.TargetApi;
import android.app.Notification;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.os.Build;

@TargetApi(Build.VERSION_CODES.LOLLIPOP)
public class MediaStyleNotificationBuilder implements NotificationBuilder {

    private static final int PENDING_INTENT_REQUEST_CODE = MediaStyleNotificationBuilder.class.hashCode();
    private static final int TOGGLE_PLAY_ACTION_INDEX = 1;
    private static final int NEXT_ACTION_INDEX = 2;

    private final Notification.Builder builder;
    private final Notification.Action togglePlayAction;
    private final Notification.Action nextAction;
    private final Notification.Action previousAction;
    private final Resources resources;

    public MediaStyleNotificationBuilder(Context context) {
        resources = context.getResources();
        builder = new Notification.Builder(context);

        Notification.MediaStyle style = new Notification.MediaStyle();
        style.setShowActionsInCompactView(TOGGLE_PLAY_ACTION_INDEX, NEXT_ACTION_INDEX);
        builder.setStyle(style);
        builder.setUsesChronometer(false);
        builder.setShowWhen(false);

        previousAction = createAction(context, PlaybackAction.PREVIOUS);
        togglePlayAction = createAction(context, PlaybackAction.TOGGLE_PLAYBACK);
        nextAction = createAction(context, PlaybackAction.NEXT);
        builder.addAction(previousAction);
        builder.addAction(togglePlayAction);
        builder.addAction(nextAction);
    }

    private Notification.Action createAction(Context context, String action) {
        int icon;
        String title;
        switch (action) {
            case PlaybackAction.PREVIOUS:
                icon = R.drawable.notifications_previous;
                title = resources.getString(R.string.previous);
                break;
            case PlaybackAction.TOGGLE_PLAYBACK:
                icon = R.drawable.notifications_play;
                title = resources.getString(R.string.play);
                break;
            case PlaybackAction.NEXT:
                icon = R.drawable.notifications_next;
                title = resources.getString(R.string.next);
                break;
            default:
                throw new IllegalArgumentException("Unknown action : " + action);
        }
        return new Notification.Action(icon, title, createPendingIntent(context, action));
    }

    private PendingIntent createPendingIntent(Context context, String playbackAction) {
        return PendingIntent.getBroadcast(context, PENDING_INTENT_REQUEST_CODE, createIntent(playbackAction), 0);
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB_MR1)
    private Intent createIntent(String playbackAction) {
        final Intent intent = new Intent(playbackAction)
                .putExtra(PlayControlEvent.EXTRA_EVENT_SOURCE, PlayControlEvent.SOURCE_NOTIFICATION);

        // add this or it will not trigger a process start (as of 3.1)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR1) {
            intent.addFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES);
        }
        return intent;
    }

    @Override
    public void setOngoing(boolean isOngoing) {
        builder.setOngoing(isOngoing);
    }

    @Override
    public void setSmallIcon(int icon) {
        builder.setSmallIcon(icon);
    }

    @Override
    public void setIcon(Bitmap icon) {
        builder.setLargeIcon(icon);
    }

    @Override
    public void clearIcon() {
        builder.setLargeIcon(null);
    }

    @Override
    public void setContentIntent(PendingIntent pendingIntent) {
        builder.setContentIntent(pendingIntent);
    }

    @Override
    public void setTrackTitle(String trackTitle) {
        builder.setContentText(trackTitle);
    }

    @Override
    public void setCreatorName(String creatorName) {
        builder.setContentTitle(creatorName);
    }

    @Override
    public void setPlayingStatus(boolean isPlaying) {
        if (isPlaying) {
            togglePlayAction.icon = R.drawable.notifications_pause;
            togglePlayAction.title = resources.getString(R.string.pause);
        } else {
            togglePlayAction.icon = R.drawable.notifications_play;
            togglePlayAction.title = resources.getString(R.string.play);
        }
        setOngoing(isPlaying);
    }

    @Override
    public boolean hasPlayStateSupport() {
        return true;
    }

    @Override
    public boolean hasArtworkSupport() {
        return true;
    }

    @Override
    public Notification build() {
        return builder.build();
    }
}
