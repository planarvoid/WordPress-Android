package com.soundcloud.android.testsupport.fixtures;

import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playback.PlaybackStateTransition;
import com.soundcloud.android.playback.PlaybackState;
import com.soundcloud.android.playback.PlayStateReason;
import com.soundcloud.android.utils.CurrentDateProvider;
import com.soundcloud.android.utils.TestDateProvider;

public class TestPlayStates {

    public static final Urn URN = Urn.forTrack(123L);

    public static PlaybackStateTransition playing() {
        return new PlaybackStateTransition(PlaybackState.PLAYING, PlayStateReason.NONE, URN);
    }

    public static PlaybackStateTransition playing(long position, long duration) {
        return new PlaybackStateTransition(PlaybackState.PLAYING, PlayStateReason.NONE, URN, position, duration);
    }

    public static PlaybackStateTransition playing(long position, long duration, CurrentDateProvider dateProvider) {
        return new PlaybackStateTransition(PlaybackState.PLAYING,
                                           PlayStateReason.NONE,
                                           URN,
                                           position,
                                           duration,
                                           dateProvider);
    }

    public static PlaybackStateTransition idle() {
        return new PlaybackStateTransition(PlaybackState.IDLE, PlayStateReason.NONE, URN);
    }

    public static PlaybackStateTransition idle(long position, long duration) {
        return new PlaybackStateTransition(PlaybackState.IDLE, PlayStateReason.NONE, URN, position, duration);
    }

    public static PlaybackStateTransition idleDefault() {
        return new PlaybackStateTransition(PlaybackState.IDLE, PlayStateReason.NONE, Urn.NOT_SET);
    }

    public static PlaybackStateTransition complete() {
        return new PlaybackStateTransition(PlaybackState.IDLE, PlayStateReason.PLAYBACK_COMPLETE, URN);
    }

    public static PlaybackStateTransition buffering() {
        return new PlaybackStateTransition(PlaybackState.BUFFERING, PlayStateReason.NONE, URN);
    }

    public static PlaybackStateTransition buffering(TestDateProvider dateProvider) {
        return new PlaybackStateTransition(PlaybackState.BUFFERING, PlayStateReason.NONE, URN, 0, 0, dateProvider);
    }

    public static PlaybackStateTransition buffering(long position, long duration) {
        return new PlaybackStateTransition(PlaybackState.BUFFERING, PlayStateReason.NONE, URN, position, duration);
    }

    public static PlaybackStateTransition error(PlayStateReason reason) {
        return new PlaybackStateTransition(PlaybackState.IDLE, reason, URN);
    }
}
