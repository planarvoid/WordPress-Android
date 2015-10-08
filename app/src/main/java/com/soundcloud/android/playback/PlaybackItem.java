package com.soundcloud.android.playback;

import com.soundcloud.android.model.Urn;

public interface PlaybackItem {

    Urn getTrackUrn();

    long getStartPosition();

    PlaybackType getPlaybackType();

    long getDuration();
}
