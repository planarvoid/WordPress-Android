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
        flipperAdapter.onProgressChanged(new ProgressChange(event.getUri(), event.getPosition(), event.getDuration()));
    }

    @Override
    public void onPerformanceEvent(audio_performance event) {
        flipperAdapter.onPerformanceEvent(new AudioPerformanceEvent(event.getType().const_get_value(), event.getLatency().const_get_value(), event.getProtocol().const_get_value(),
                                                                    event.getHost().const_get_value(), event.getFormat().const_get_value(), (int) event.getBitrate().const_get_value(),
                                                                    event.getDetails().get_value().toJson()));
    }

    @Override
    public void onStateChanged(state_change event) {
        flipperAdapter.onStateChanged(mapToStateChange(event));
    }

    @Override
    public void onBufferingChanged(state_change event) {
        flipperAdapter.onBufferingChanged(mapToStateChange(event));
    }

    private StateChange mapToStateChange(state_change event) {
        return new StateChange(event.getUri(), event.getState(),
                               event.getReason(), event.getBuffering(),
                               event.getPosition(), event.getDuration(), event.getStreamingProtocol());
    }

    @Override
    public void onDurationChanged(state_change event) {
        // FIXME DO NOT CALL SUPER AS IT WILL CRASH THE APP WHILE SEEKING
        // FIXME Check JIRA: PLAYBACK-2706
    }

    @Override
    public void onSeekingStatusChanged(state_change stateChangeEvent) {
        flipperAdapter.onSeekingStatusChanged(new SeekingStatusChange(stateChangeEvent.getUri(), stateChangeEvent.getSeekingInProgress()));
    }

    @Override
    public void onError(error_message error) {
        flipperAdapter.onError(new FlipperError(error.getCategory(), error.getSourceFile(), error.getLine(),
                                                error.getErrorMessage(), error.getStreamingProtocol(),
                                                error.getCdn(), error.getFormat(), error.getBitRate()));
    }
}
