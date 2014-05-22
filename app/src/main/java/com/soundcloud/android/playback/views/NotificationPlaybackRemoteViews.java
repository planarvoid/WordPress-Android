package com.soundcloud.android.playback.views;

import com.soundcloud.android.R;
import com.soundcloud.android.events.PlayControlEvent;
import com.soundcloud.android.model.Track;
import com.soundcloud.android.playback.external.PlaybackAction;
import com.soundcloud.android.playback.service.PlaybackService;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Parcel;

/**
 * Play controls used in notifications (API level 11 and up)
 */
public class NotificationPlaybackRemoteViews extends PlaybackRemoteViews {

    private static final int PENDING_INTENT_REQUEST_CODE = NotificationPlaybackRemoteViews.class.hashCode();

    public NotificationPlaybackRemoteViews(String packageName) {
        super(packageName,
                R.layout.playback_status_v11,
                R.drawable.ic_notification_play_states,
                R.drawable.ic_notification_pause_states);
    }

    @SuppressWarnings("UnusedDeclaration")
    public NotificationPlaybackRemoteViews(Parcel parcel) {
        super(parcel);
    }

    @Deprecated
    public void setNotification(Track track, boolean isPlaying) {
        this.track = track;
        this.isPlaying = isPlaying;
        setCurrentTrackTitle(track.title);
        setCurrentUsername(track.user == null ? "" : track.user.username);
    }

    public void linkButtonsNotification(Context context) {
        linkPlayerControls(context);
        final Intent close = new Intent(PlaybackService.Actions.STOP_ACTION);
        setOnClickPendingIntent(R.id.close, PendingIntent.getService(context,
                PENDING_INTENT_REQUEST_CODE, close, 0));
    }

    private void linkPlayerControls(Context context) {
        setOnClickPendingIntent(R.id.toggle_playback, createPendingIntent(context, PlaybackAction.TOGGLE_PLAYBACK));
        setOnClickPendingIntent(R.id.next, createPendingIntent(context, PlaybackAction.NEXT));
    }

    private PendingIntent createPendingIntent(Context context, String playbackAction) {
        return PendingIntent.getBroadcast(context, PENDING_INTENT_REQUEST_CODE, createIntent(playbackAction), 0);
    }

    private Intent createIntent(String playbackAction) {
        return new Intent(playbackAction)
                .putExtra(PlayControlEvent.EXTRA_EVENT_SOURCE, PlayControlEvent.SOURCE_WIDGET);
    }

    public boolean isAlreadyNotifying(Track track, boolean isPlaying) {
        return track == this.track && isPlaying == this.isPlaying;
    }
}
