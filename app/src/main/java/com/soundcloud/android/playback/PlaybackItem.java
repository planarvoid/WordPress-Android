package com.soundcloud.android.playback;

import com.soundcloud.android.model.Urn;

public interface PlaybackItem {

    Urn getUrn();

    long getStartPosition();

    PlaybackType getPlaybackType();

    long getDuration();

}
