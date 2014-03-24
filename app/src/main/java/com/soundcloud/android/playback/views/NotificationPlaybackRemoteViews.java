package com.soundcloud.android.playback.views;

import com.soundcloud.android.R;
import com.soundcloud.android.events.PlayControlEvent;
import com.soundcloud.android.model.Track;
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
        mTrack = track;
        mIsPlaying = isPlaying;
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
        final Intent previous = new Intent(PlaybackService.Actions.PREVIOUS_ACTION)
                .putExtra(PlayControlEvent.EXTRA_EVENT_SOURCE, PlayControlEvent.SOURCE_NOTIFICATION);
        setOnClickPendingIntent(R.id.prev, PendingIntent.getService(context,
                PENDING_INTENT_REQUEST_CODE, previous, 0));

        final Intent pause = new Intent(PlaybackService.Actions.TOGGLEPLAYBACK_ACTION)
                .putExtra(PlayControlEvent.EXTRA_EVENT_SOURCE, PlayControlEvent.SOURCE_NOTIFICATION);
        setOnClickPendingIntent(R.id.pause, PendingIntent.getService(context,
                PENDING_INTENT_REQUEST_CODE, pause, 0));

        final Intent next = new Intent(PlaybackService.Actions.NEXT_ACTION)
                .putExtra(PlayControlEvent.EXTRA_EVENT_SOURCE, PlayControlEvent.SOURCE_NOTIFICATION);
        setOnClickPendingIntent(R.id.next, PendingIntent.getService(context,
                PENDING_INTENT_REQUEST_CODE, next, 0));
    }

    public boolean isAlreadyNotifying(Track track, boolean isPlaying){
        return track == mTrack && isPlaying == mIsPlaying;
    }
}
