package com.soundcloud.android.events;

import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playback.PlaybackProgress;

public class AudioAdFailedToBufferEvent extends TrackingEvent {
    public static final String PLAYBACK_POSITION = "playback_position";
    public static final String WAIT_PERIOD = "wait_period_secs";

    public AudioAdFailedToBufferEvent(Urn track, PlaybackProgress position, int failedAdWaitSecs) {
        super(KIND_DEFAULT, System.currentTimeMillis());
        put(AdTrackingKeys.KEY_AD_URN, track.toString());
        put(PLAYBACK_POSITION, Long.toString(position.getPosition()));
        put(WAIT_PERIOD, Integer.toString(failedAdWaitSecs));
    }
}
