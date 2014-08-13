package com.soundcloud.android.playback.service;

import com.soundcloud.propeller.PropertySet;

import javax.inject.Inject;

// this class is getting deleted
@SuppressWarnings({"PMD.UncommentedEmptyMethod"})
public class BufferingPlaya implements Playa {

    @Inject
    public BufferingPlaya() {
    }


    @Override
    public void play(PropertySet track) {

    }

    @Override
    public void play(PropertySet track, long fromPos) {

    }

    @Override
    public void playUninterrupted(PropertySet track) {

    }

    @Override
    public boolean resume() {
        return true;
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
    public boolean isSeekable() {
        return false;
    }

    @Override
    public boolean isNotSeekablePastBuffer() {
        return false;
    }
}
