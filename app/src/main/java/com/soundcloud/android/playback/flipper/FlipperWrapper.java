package com.soundcloud.android.playback.flipper;

import com.soundcloud.flippernative.api.Player;
import com.soundcloud.flippernative.api.PlayerListener;
import com.soundcloud.flippernative.api.audio_performance;
import com.soundcloud.flippernative.api.error_message;
import com.soundcloud.flippernative.api.state_change;

public class FlipperWrapper extends PlayerListener {

    private final Player flipper;
    private final FlipperAdapter flipperAdapter;

    public FlipperWrapper(FlipperAdapter flipperAdapter, FlipperFactory flipperFactory) {
        this.flipperAdapter = flipperAdapter;
        this.flipper = flipperFactory.create(this);
    }

    public void prefetch(String mediaUri) {
        flipper.prefetch(mediaUri);
    }

    public void play() {
        flipper.play();
    }

    public void pause() {
        flipper.pause();
    }

    public void seek(long positionMs) {
        flipper.seek(positionMs);
    }

    public float getVolume() {
        return (float) flipper.getVolume();
    }

    public void setVolume(float level) {
        flipper.setVolume(level);
    }

    public void destroy() {
        flipper.destroy();
    }

    public void open(String mediaUri, long positionMs) {
        flipper.open(mediaUri, positionMs);
    }

    void openEncrypted(String mediaUri, byte[] key, byte[] initVector, long positionMs) {
        flipper.openEncrypted(mediaUri, key, initVector, positionMs);
    }

    @Override
    public void onProgressChanged(state_change event) {
        flipperAdapter.onProgressChanged(event);
    }

    @Override
    public void onPerformanceEvent(audio_performance event) {
        flipperAdapter.onPerformanceEvent(event);
    }

    @Override
    public void onStateChanged(state_change event) {
        flipperAdapter.onStateChanged(event);
    }

    @Override
    public void onBufferingChanged(state_change event) {
        flipperAdapter.onBufferingChanged(event);
    }

    @Override
    public void onDurationChanged(state_change event) {
        // FIXME DO NOT CALL SUPER AS IT WILL CRASH THE APP WHILE SEEKING
        // FIXME Check JIRA: PLAYBACK-2706
    }

    @Override
    public void onSeekingStatusChanged(state_change stateChangeEvent) {
        flipperAdapter.onSeekingStatusChanged(stateChangeEvent);
    }

    @Override
    public void onError(error_message message) {
        flipperAdapter.onError(message);
    }
}
