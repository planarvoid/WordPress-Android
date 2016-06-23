package com.soundcloud.android.testsupport.fixtures;

import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playback.PlayStateReason;
import com.soundcloud.android.playback.PlaybackState;
import com.soundcloud.android.playback.PlaybackStateTransition;
import com.soundcloud.android.utils.CurrentDateProvider;
import com.soundcloud.android.utils.DateProvider;
import com.soundcloud.android.utils.TestDateProvider;

public class TestPlayerTransitions {

    public static final Urn URN = Urn.forTrack(123L);

    public static PlaybackStateTransition idle() {
        return new PlaybackStateTransition(PlaybackState.IDLE, PlayStateReason.NONE, URN);
    }

    public static PlaybackStateTransition buffering() {
        return new PlaybackStateTransition(PlaybackState.BUFFERING, PlayStateReason.NONE, URN);
    }

    public static PlaybackStateTransition playing() {
        return playing(URN);
    }

    public static PlaybackStateTransition playing(Urn urn) {
        return new PlaybackStateTransition(PlaybackState.PLAYING, PlayStateReason.NONE, urn);
    }

    public static PlaybackStateTransition playing(long position, long duration) {
        return playing(URN, position, duration);
    }

    public static PlaybackStateTransition playing(Urn urn, long position, long duration) {
        return playing(urn, position, duration, new TestDateProvider());
    }


    public static PlaybackStateTransition playing(Urn urn, long position, long duration, DateProvider dateProvider) {
        return new PlaybackStateTransition(PlaybackState.PLAYING, PlayStateReason.NONE, urn, position, duration, dateProvider);
    }

    public static PlaybackStateTransition playing(long position, long duration, CurrentDateProvider dateProvider) {
        return new PlaybackStateTransition(PlaybackState.PLAYING,
                                                PlayStateReason.NONE,
                                                URN,
                                                position,
                                                duration,
                                                dateProvider);
    }

    public static PlaybackStateTransition idle(long position, long duration) {
        return new PlaybackStateTransition(PlaybackState.IDLE, PlayStateReason.NONE, URN, position, duration);
    }
}
