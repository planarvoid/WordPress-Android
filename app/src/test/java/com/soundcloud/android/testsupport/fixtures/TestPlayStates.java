package com.soundcloud.android.testsupport.fixtures;

import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playback.PlayStateEvent;
import com.soundcloud.android.playback.PlayStateReason;
import com.soundcloud.android.playback.PlaybackStateTransition;
import com.soundcloud.android.utils.CurrentDateProvider;
import com.soundcloud.android.utils.DateProvider;

import android.support.annotation.NonNull;

public class TestPlayStates {

    public static final Urn URN = TestPlayerTransitions.URN;
    private static final String PLAY_ID = "play-id";
    private static final boolean IS_FIRST_PLAY = false;
    private static final int API_DURATION = 456;

    public static PlayStateEvent playing() {
        return wrap(TestPlayerTransitions.playing());
    }

    public static PlayStateEvent playing(Urn urn) {
        return wrap(TestPlayerTransitions.playing(urn));
    }

    public static PlayStateEvent playing(long position, long duration) {
        return playing(URN, position, duration);
    }

    public static PlayStateEvent playing(Urn urn, long position, long duration) {
        return wrap(TestPlayerTransitions.playing(urn, position, duration));
    }


    public static PlayStateEvent playing(Urn urn, int position, int duration, DateProvider dateProvider) {
        return wrap(TestPlayerTransitions.playing(urn, position, duration, dateProvider));
    }

    public static PlayStateEvent playing(long position, long duration, CurrentDateProvider dateProvider) {
        return wrap(TestPlayerTransitions.playing(position, duration, dateProvider));
    }

    public static PlayStateEvent idle() {
        return wrap(TestPlayerTransitions.idle());
    }

    public static PlayStateEvent idle(long position, long duration) {
        return wrap(TestPlayerTransitions.idle(position, duration));
    }

    public static PlayStateEvent idle(Urn urn, long position, long duration) {
        return wrap(TestPlayerTransitions.idle(urn, position, duration, PlayStateReason.NONE));
    }

    public static PlayStateEvent complete() {
        return complete(URN);
    }

    public static PlayStateEvent complete(Urn urn) {
        return wrap(TestPlayerTransitions.complete(urn));
    }

    public static PlayStateEvent complete(Urn urn, long position, long duration) {
        return wrap(TestPlayerTransitions.complete(urn, position, duration));
    }

    public static PlayStateEvent playQueueComplete() {
        return PlayStateEvent.createPlayQueueCompleteEvent(complete());
    }

    public static PlayStateEvent buffering() {
        return wrap(TestPlayerTransitions.buffering());
    }

    public static PlayStateEvent error(PlayStateReason reason) {
        return wrap(TestPlayerTransitions.idle(0, API_DURATION, reason));
    }

    @NonNull
    public static PlayStateEvent wrap(PlaybackStateTransition transition) {
        return PlayStateEvent.create(transition, API_DURATION, IS_FIRST_PLAY, PLAY_ID);
    }

    @NonNull
    public static PlayStateEvent wrap(PlaybackStateTransition transition, boolean isFirstPlay) {
        return PlayStateEvent.create(transition, API_DURATION, isFirstPlay, PLAY_ID);
    }
}
