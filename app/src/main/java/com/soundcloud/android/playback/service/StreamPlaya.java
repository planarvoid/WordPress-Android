package com.soundcloud.android.playback.service;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import com.soundcloud.android.model.Track;
import com.soundcloud.android.playback.service.mediaplayer.MediaPlayerAdapter;
import com.soundcloud.android.playback.service.skippy.SkippyAdapter;
import com.soundcloud.android.preferences.DevSettings;
import com.soundcloud.android.utils.SharedPreferencesUtils;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import javax.inject.Inject;
import java.util.BitSet;

//Not a hater
public class StreamPlaya implements Playa, Playa.PlayaListener {


    public static final String TAG = "StreamPlaya";
    @VisibleForTesting
    static final String PLAYS_SINCE_SKIPPY = "StreamPlaya.playsSinceSkippy";
    @VisibleForTesting
    static final int MAX_PLAYS_OFF_SKIPPY = 3; // TODO increase to 10 before launch

    @VisibleForTesting
    static BitSet skippyInitialisationStatus = new BitSet(2);

    private final MediaPlayerAdapter mediaPlayaDelegate;
    private final SkippyAdapter skippyPlayaDelegate;
    private final BufferingPlaya bufferingPlayaDelegate;
    private final SharedPreferences playbackPreferences;

    private Playa currentPlaya;
    private PlayaListener playaListener;

    // store start info so we can fallback and retry after Skippy failures
    private TrackPlaybackInfo trackPlaybackInfo;

    @Inject
    public StreamPlaya(Context context, SharedPreferences sharedPreferences, MediaPlayerAdapter mediaPlayerAdapter,
                       SkippyAdapter skippyAdapter, BufferingPlaya bufferingPlaya){
        playbackPreferences = sharedPreferences;
        mediaPlayaDelegate = mediaPlayerAdapter;
        skippyPlayaDelegate = skippyAdapter;
        bufferingPlayaDelegate = bufferingPlaya;
        currentPlaya = bufferingPlayaDelegate;

        if (skippyLoadHasNotBeenAttempted()) {
            skippyInitialisationStatus.set(1, skippyPlayaDelegate.init(context));
            skippyInitialisationStatus.set(0, true);
        }
    }



    @Override
    public void play(Track track) {
        trackPlaybackInfo = new TrackPlaybackInfo(track, 0L);
        configureNextPlayaToUseViaPreferences();
        currentPlaya.play(track);
    }

    @Override
    public void play(Track track, long fromPos) {
        trackPlaybackInfo = new TrackPlaybackInfo(track, fromPos);
        configureNextPlayaToUseViaPreferences();
        currentPlaya.play(track, fromPos);
    }

    @Override
    public boolean resume() {
        return currentPlaya.resume();
    }

    @Override
    public void pause() {
        currentPlaya.pause();
    }

    @Override
    public long seek(long ms, boolean performSeek) {
        return currentPlaya.seek(ms, performSeek);
    }

    @Override
    public long getProgress() {
        return currentPlaya.getProgress();
    }

    @Override
    public void setVolume(float v) {
        currentPlaya.setVolume(v);
    }

    @Override
    public void stop() {
        currentPlaya.stop();
    }

    @Override
    public StateTransition getLastStateTransition() {
        return currentPlaya.getLastStateTransition();
    }

    @Override
    public PlayaState getState() {
        return currentPlaya.getState();
    }

    @Override
    public boolean isSeekable() {
        return currentPlaya.isSeekable();
    }

    @Override
    public boolean isNotSeekablePastBuffer() {
        return currentPlaya.isNotSeekablePastBuffer();
    }

    @Override
    public boolean isPlaying() {
        return currentPlaya.isPlaying();
    }

    @Override
    public boolean isPlayerPlaying() {
        return currentPlaya.isPlayerPlaying();
    }

    @Override
    public boolean isBuffering() {
        return currentPlaya.isBuffering();
    }

    @Override
    public void destroy() {
        // call stop first as it will save the queue/position
        mediaPlayaDelegate.destroy();
        skippyPlayaDelegate.destroy();
    }

    @Override
    public void setListener(PlayaListener playaListener) {
        this.playaListener = playaListener;
        if (currentPlaya != null){
            currentPlaya.setListener(playaListener);
        }
    }

    @Override
    public void onPlaystateChanged(StateTransition stateTransition) {
        if (currentPlaya == skippyPlayaDelegate &&
                stateTransition.wasError() && skippyPlayaDelegate.getProgress() == trackPlaybackInfo.getStartPosition()) {
            Log.i(TAG, "Falling back to MediaPlayer");

            configureNextPlayaToUse(mediaPlayaDelegate);
            mediaPlayaDelegate.play(trackPlaybackInfo.getTrack(), trackPlaybackInfo.getStartPosition());

        } else {
            Preconditions.checkNotNull(playaListener, "Stream Player Listener is unexpectedly null when passing state");
            playaListener.onPlaystateChanged(stateTransition);
        }
    }

    @Override
    public boolean requestAudioFocus() {
        Preconditions.checkNotNull(playaListener, "Stream Player Listener is unexpectedly null when requesting audio focus");
        return playaListener.requestAudioFocus();
    }

    public void startBufferingMode(){
        final Playa lastPlaya = currentPlaya;
        currentPlaya = bufferingPlayaDelegate;
        onPlaystateChanged(bufferingPlayaDelegate.getLastStateTransition());

        if (lastPlaya != null) {
            lastPlaya.setListener(null);
            lastPlaya.stop();
        }

    }

    private void configureNextPlayaToUseViaPreferences(){
        configureNextPlayaToUse(getNextPlaya());
    }
    private void configureNextPlayaToUse(Playa nextPlaya){

        currentPlaya = nextPlaya;
        currentPlaya.setListener(this);

        if (currentPlaya == skippyPlayaDelegate){
            SharedPreferencesUtils.apply(playbackPreferences.edit().putInt(PLAYS_SINCE_SKIPPY, 0));
            Log.i(TAG, "Configuring Playa to SkippyPlaya");
        } else {
            int plays = playbackPreferences.getInt(PLAYS_SINCE_SKIPPY, 0);
            SharedPreferencesUtils.apply(playbackPreferences.edit().putInt(PLAYS_SINCE_SKIPPY, plays + 1));
            Log.i(TAG, "Configuring Playa to MediaPlaya");
        }
    }

    private boolean skippyLoadHasNotBeenAttempted() {
        return !skippyInitialisationStatus.get(0);
    }

    private Playa getNextPlaya() {
        if (skippyInitialisationFailed()){
            return mediaPlayaDelegate;
        }

        if (playbackPreferences.getBoolean(DevSettings.DEV_ENABLE_SKIPPY, false)) {
            return skippyPlayaDelegate;
        } else if (playbackPreferences.getInt(PLAYS_SINCE_SKIPPY, 0) >= MAX_PLAYS_OFF_SKIPPY) {
            return skippyPlayaDelegate;
        } else {
            return mediaPlayaDelegate;
        }

    }

    private boolean skippyInitialisationFailed() {
        return !skippyInitialisationStatus.get(1);
    }

    private static class TrackPlaybackInfo {
        private final Track track;
        private final long startPosition;

        private TrackPlaybackInfo(Track track, long startPosition) {
            this.track = track;
            this.startPosition = startPosition;
        }

        public Track getTrack() {
            return track;
        }

        public long getStartPosition() {
            return startPosition;
        }
    }
}
