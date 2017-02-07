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

    public static PlaybackStateTransition idle(Urn urn) {
        return withExtras(new PlaybackStateTransition(PlaybackState.IDLE, PlayStateReason.NONE, urn, 0, 0));
    }

    public static PlaybackStateTransition idle() {
        return idle(PlayStateReason.NONE);
    }

    public static PlaybackStateTransition idle(PlayStateReason reason) {
        return idle(0, 0, reason);
    }

    public static PlaybackStateTransition idle(long position, long duration) {
        return idle(position, duration, PlayStateReason.NONE);
    }

    public static PlaybackStateTransition idle(long position, long duration, PlayStateReason reason) {
        return withExtras(new PlaybackStateTransition(PlaybackState.IDLE, reason, URN, position, duration));
    }

    public static PlaybackStateTransition buffering() {
        return withExtras(new PlaybackStateTransition(PlaybackState.BUFFERING, PlayStateReason.NONE, URN, 0, 0));
    }

    public static PlaybackStateTransition playing() {
        return playing(URN);
    }

    public static PlaybackStateTransition playing(Urn urn) {
        return withExtras(new PlaybackStateTransition(PlaybackState.PLAYING, PlayStateReason.NONE, urn, 0, 0));
    }

    public static PlaybackStateTransition playing(long position, long duration) {
        return playing(URN, position, duration);
    }

    public static PlaybackStateTransition playing(Urn urn, long position, long duration) {
        return playing(urn, position, duration, new TestDateProvider());
    }

    public static PlaybackStateTransition playing(Urn urn, long position, long duration, DateProvider dateProvider) {
        return withExtras(new PlaybackStateTransition(PlaybackState.PLAYING, PlayStateReason.NONE, urn, position, duration, dateProvider));
    }

    public static PlaybackStateTransition playing(long position, long duration, CurrentDateProvider dateProvider) {
        return withExtras(new PlaybackStateTransition(PlaybackState.PLAYING,
                                                PlayStateReason.NONE,
                                                URN,
                                                position,
                                                duration,
                                                dateProvider));
    }

    public static PlaybackStateTransition complete() {
        return complete(URN);
    }

    public static PlaybackStateTransition complete(Urn urn) {
        return withExtras(new PlaybackStateTransition(PlaybackState.IDLE, PlayStateReason.PLAYBACK_COMPLETE, urn, 0, 0));
    }

    public static PlaybackStateTransition error(PlayStateReason REASON) {
        return new PlaybackStateTransition(PlaybackState.IDLE, REASON, URN, 0, 0);
    }

    private static PlaybackStateTransition withExtras(PlaybackStateTransition transition) {
        return transition
                .addExtraAttribute(PlaybackStateTransition.EXTRA_PLAYER_TYPE, "player")
                .addExtraAttribute(PlaybackStateTransition.EXTRA_PLAYBACK_PROTOCOL, "hls")
                .addExtraAttribute(PlaybackStateTransition.EXTRA_CONNECTION_TYPE, "6g");
    }
}
