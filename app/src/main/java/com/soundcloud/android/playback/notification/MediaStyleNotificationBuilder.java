package com.soundcloud.android.playback.notification;

import com.soundcloud.android.R;
import com.soundcloud.android.events.PlayControlEvent;
import com.soundcloud.android.image.ApiImageSize;
import com.soundcloud.android.image.ImageOperations;
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
    private static final int PREVIOUS_ACTION_INDEX = 0;
    private static final int TOGGLE_PLAY_ACTION_INDEX = 1;
    private static final int NEXT_ACTION_INDEX = 2;

    private final Resources resources;
    private final Context context;

    private Notification.Builder builder;
    private int smallIcon;
    private Bitmap largeIcon;
    private String trackTitle;
    private String creatorName;
    private PendingIntent pendingIntent;

    public MediaStyleNotificationBuilder(Context context, ImageOperations imageOperations) {
        this.context = context;
        resources = context.getResources();

        smallIcon = R.drawable.notification_loading;
        largeIcon = imageOperations.decodeResource(resources, R.drawable.notification_loading);
        builder = createBuilder();

        builder.addAction(createAction(context, PlaybackAction.PREVIOUS));
        builder.addAction(createAction(context, PlaybackAction.TOGGLE_PLAYBACK));
        builder.addAction(createAction(context, PlaybackAction.NEXT));
    }

    private Notification.Builder createBuilder() {
        Notification.Builder builder = new Notification.Builder(context);
        Notification.MediaStyle style = new Notification.MediaStyle()
                .setShowActionsInCompactView(PREVIOUS_ACTION_INDEX, TOGGLE_PLAY_ACTION_INDEX, NEXT_ACTION_INDEX);

        builder.setStyle(style);
        builder.setVisibility(Notification.VISIBILITY_PUBLIC);
        builder.setUsesChronometer(false);
        builder.setShowWhen(false);

        builder.setSmallIcon(smallIcon);
        builder.setLargeIcon(largeIcon);
        builder.setContentTitle(creatorName);
        builder.setContentText(trackTitle);
        builder.setContentIntent(pendingIntent);

        return builder;
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
            case PlaybackAction.PAUSE:
                icon = R.drawable.notifications_play;
                title = resources.getString(R.string.play);
                action = PlaybackAction.TOGGLE_PLAYBACK;
                break;
            case PlaybackAction.PLAY:
                icon = R.drawable.notifications_pause;
                title = resources.getString(R.string.pause);
                action = PlaybackAction.TOGGLE_PLAYBACK;
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

    private Intent createIntent(String playbackAction) {
        return new Intent(playbackAction)
                .putExtra(PlayControlEvent.EXTRA_EVENT_SOURCE, PlayControlEvent.SOURCE_NOTIFICATION)
                .addFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES);
    }

    @Override
    public void setSmallIcon(int smallIcon) {
        this.smallIcon = smallIcon;
        builder.setSmallIcon(smallIcon);
    }

    @Override
    public void setIcon(Bitmap largeIcon) {
        this.largeIcon = largeIcon;
        builder.setLargeIcon(largeIcon);
    }

    @Override
    public void setContentIntent(PendingIntent pendingIntent) {
        this.pendingIntent = pendingIntent;
        builder.setContentIntent(pendingIntent);
    }

    @Override
    public void setTrackTitle(String trackTitle) {
        this.trackTitle = trackTitle;
        builder.setContentText(trackTitle);
    }

    @Override
    public void setCreatorName(String creatorName) {
        this.creatorName = creatorName;
        builder.setContentTitle(creatorName);
    }

    @Override
    public void setPlayingStatus(boolean isPlaying) {
        builder = createBuilder();
        builder.addAction(createAction(context, PlaybackAction.PREVIOUS));

        if (isPlaying) {
            builder.addAction(createAction(context, PlaybackAction.PLAY));
        } else {
            builder.addAction(createAction(context, PlaybackAction.PAUSE));
        }

        builder.addAction(createAction(context, PlaybackAction.NEXT));
        builder.setOngoing(isPlaying);
    }

    @Override
    public boolean hasPlayStateSupport() {
        return true;
    }

    @Override
    public ApiImageSize getImageSize() {
        return ApiImageSize.getNotificationLargeIconImageSize(resources);
    }

    @Override
    public int getTargetImageSize() {
        return resources.getDimensionPixelSize(R.dimen.notification_image_large_width);
    }

    @Override
    public Notification build() {
        return builder.build();
    }
}
