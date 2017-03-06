package com.soundcloud.android.testsupport.fixtures;

import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playback.AudioPlaybackItem;
import com.soundcloud.android.playback.PlaybackType;

public class TestPlaybackItem {

    public static final Urn URN = TestPlayerTransitions.URN;
    public static final int DURATION = 456;

    public static AudioPlaybackItem audio() {
        return audio(URN, 0, DURATION);
    }

    public static AudioPlaybackItem audio(Urn urn) {
        return audio(urn, 0, DURATION);
    }

    public static AudioPlaybackItem audio(Urn urn, long position) {
        return audio(urn, position, DURATION);
    }

    public static AudioPlaybackItem audio(Urn urn, long position, long duration) {
        return AudioPlaybackItem.create(urn, position, duration, PlaybackType.AUDIO_DEFAULT);
    }

    public static AudioPlaybackItem audioAd(Urn urn, long position, long duration) {
        return AudioPlaybackItem.create(urn, position, duration, PlaybackType.AUDIO_AD);
    }

    public static AudioPlaybackItem videoAd(Urn urn, long position, long duration) {
        return AudioPlaybackItem.create(urn, position, duration, PlaybackType.VIDEO_AD);
    }
}
