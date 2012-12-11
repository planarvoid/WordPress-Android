package com.soundcloud.android.view.play;

import com.soundcloud.android.R;
import com.soundcloud.android.model.Track;
import com.soundcloud.android.service.playback.CloudPlaybackService;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Parcel;


/**
 * Play controls used in notifications (API level 11 and up)
 */
public class NotificationPlaybackRemoteViews extends PlaybackRemoteViews {

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
        final Intent close = new Intent(CloudPlaybackService.STOP_ACTION);
        setOnClickPendingIntent(R.id.close, PendingIntent.getService(context,
                0 /* requestCode */, close, 0 /* flags */));
    }

    public boolean isAlreadyNotifying(Track track, boolean isPlaying){
        return track == mTrack && isPlaying == mIsPlaying;
    }
}
