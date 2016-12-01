package com.soundcloud.android.events;

import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playback.PlaybackProgress;

public class AdFailedToBufferEvent extends LegacyTrackingEvent {
    public static final String PLAYBACK_POSITION = "playback_position";
    public static final String WAIT_PERIOD = "wait_period_secs";

    public AdFailedToBufferEvent(Urn track, PlaybackProgress position, int failedAdWaitSecs) {
        super(KIND_DEFAULT);
        put(PlayableTrackingKeys.KEY_AD_URN, track.toString());
        put(PLAYBACK_POSITION, Long.toString(position.getPosition()));
        put(WAIT_PERIOD, Integer.toString(failedAdWaitSecs));
    }
}
