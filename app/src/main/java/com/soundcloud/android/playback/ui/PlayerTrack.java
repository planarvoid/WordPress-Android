package com.soundcloud.android.playback.ui;

import com.soundcloud.android.model.PlayableProperty;
import com.soundcloud.android.model.PropertySetSource;
import com.soundcloud.android.tracks.TrackProperty;
import com.soundcloud.android.tracks.TrackUrn;
import com.soundcloud.android.users.UserUrn;
import com.soundcloud.propeller.PropertySet;

class PlayerTrack implements PropertySetSource {

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

    UserUrn getUserUrn() {
        return source.get(PlayableProperty.CREATOR_URN);
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

    public boolean isUserRepost() {
        return source.get(PlayableProperty.IS_REPOSTED);
    }

    int getLikeCount() {
        return source.get(PlayableProperty.LIKES_COUNT);
    }

    public String getPermalinkUrl() {
        return source.get(PlayableProperty.PERMALINK_URL);
    }

    public boolean isPrivate() {
        return source.get(PlayableProperty.IS_PRIVATE);
    }

    @Override
    public PropertySet toPropertySet() {
        return source;
    }
}
