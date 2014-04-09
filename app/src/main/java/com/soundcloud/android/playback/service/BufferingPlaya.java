package com.soundcloud.android.playback.service;

import com.soundcloud.android.model.Track;

import javax.inject.Inject;

public class BufferingPlaya implements Playa {

    public static final StateTransition BUFFERING_TRANSITION = new StateTransition(PlayaState.BUFFERING, Reason.NONE);

    @Inject
    public BufferingPlaya() {
    }

    @Override
    public void play(Track track) {

    }

    @Override
    public void play(Track track, long fromPos) {

    }

    @Override
    public boolean resume() {
        return false;
    }

    @Override
    public void pause() {

    }

    @Override
    public long seek(long ms, boolean performSeek) {
        return 0;
    }

    @Override
    public long getProgress() {
        return 0;
    }

    @Override
    public void setVolume(float v) {

    }

    @Override
    public void stop() {

    }

    @Override
    public void destroy() {

    }

    @Override
    public void setListener(PlayaListener playaListener) {

    }

    @Override
    public StateTransition getLastStateTransition() {
        return BUFFERING_TRANSITION;
    }

    @Override
    public PlayaState getState() {
        return PlayaState.BUFFERING;
    }

    @Override
    public boolean isSeekable() {
        return false;
    }

    @Override
    public boolean isNotSeekablePastBuffer() {
        return false;
    }

    @Override
    public boolean isPlaying() {
        return true;
    }

    @Override
    public boolean isPlayerPlaying() {
        return false;
    }

    @Override
    public boolean isBuffering() {
        return true;
    }
}
