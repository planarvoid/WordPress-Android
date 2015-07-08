package com.soundcloud.android.playback;

import com.soundcloud.propeller.PropertySet;

import javax.inject.Inject;

// this class is getting deleted
@SuppressWarnings({"PMD.UncommentedEmptyMethod"})
class BufferingPlaya implements Playa {

    @Inject
    public BufferingPlaya() {
        // no-op
    }

    @Override
    public void play(PropertySet track) {
        // no-op
    }

    @Override
    public void play(PropertySet track, long fromPos) {
        // no-op
    }

    @Override
    public void playUninterrupted(PropertySet track) {
        // no-op
    }

    @Override
    public void playOffline(PropertySet track, long fromPos) {
        // no-op
    }

    @Override
    public boolean resume() {
        return true;
    }

    @Override
    public void pause() {
        // no-op
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
        // no-op
    }

    @Override
    public void stop() {
        // no-op
    }

    @Override
    public void stopForTrackTransition() {
        // no-op
    }

    @Override
    public void destroy() {
        // no-op
    }

    @Override
    public void setListener(PlayaListener playaListener) {
        // no-op
    }

    @Override
    public boolean isSeekable() {
        return false;
    }

}
