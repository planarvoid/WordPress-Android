package com.soundcloud.android.playback.ui;

import com.soundcloud.android.model.PlayableProperty;
import com.soundcloud.android.tracks.TrackProperty;
import com.soundcloud.android.tracks.TrackUrn;
import com.soundcloud.propeller.PropertySet;

class PlayerTrack {

    private final PropertySet source;

    PlayerTrack(PropertySet source) {
        this.source = source;
    }

    TrackUrn getUrn() {
        return source.get(TrackProperty.URN);
    }

    String getTitle() {
        return source.get(PlayableProperty.TITLE);
    }

    String getUserName() {
        return source.get(PlayableProperty.CREATOR_NAME);
    }

    long getDuration() {
        return source.get(PlayableProperty.DURATION);
    }

    String getWaveformUrl() {
        return source.get(TrackProperty.WAVEFORM_URL);
    }

    boolean isUserLike() {
        return source.get(PlayableProperty.IS_LIKED);
    }

}
