package com.soundcloud.android.playback.service;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import com.soundcloud.android.api.legacy.model.PublicApiTrack;
import com.soundcloud.android.playback.service.mediaplayer.MediaPlayerAdapter;
import com.soundcloud.android.playback.service.skippy.SkippyAdapter;
import com.soundcloud.android.tracks.TrackUrn;

import android.content.Context;
import android.content.SharedPreferences;

import javax.inject.Inject;

//Not a hater
public class StreamPlaya implements Playa, Playa.PlayaListener {


    public static final String TAG = "StreamPlaya";
    @VisibleForTesting
    static final String PLAYS_ON_CURRENT_PLAYER = "StreamPlaya.playsOnCurrentPlaya";

    @VisibleForTesting
    static boolean skippyFailedToInitialize;

    private final MediaPlayerAdapter mediaPlayaDelegate;
    private final SkippyAdapter skippyPlayaDelegate;
    private final BufferingPlaya bufferingPlayaDelegate;
    private final SharedPreferences playbackPreferences;

    private Playa currentPlaya, lastPlaya;
    private PlayaListener playaListener;

    // store start info so we can fallback and retry after Skippy failures
    private TrackPlaybackInfo trackPlaybackInfo;
    private StateTransition lastStateTransition = StateTransition.DEFAULT;

    @Inject
    public StreamPlaya(Context context, SharedPreferences sharedPreferences, MediaPlayerAdapter mediaPlayerAdapter,
                       SkippyAdapter skippyAdapter, BufferingPlaya bufferingPlaya){
        playbackPreferences = sharedPreferences;
        mediaPlayaDelegate = mediaPlayerAdapter;
        skippyPlayaDelegate = skippyAdapter;
        bufferingPlayaDelegate = bufferingPlaya;
        currentPlaya = bufferingPlayaDelegate;


        if (!skippyFailedToInitialize) {
            skippyFailedToInitialize = !skippyPlayaDelegate.init(context);
        }
    }

    /** state storage. should be gone in player v2 **/

    @Deprecated
    public StateTransition  getLastStateTransition() {
        return lastStateTransition;
    }

    @Deprecated
    public PlayaState getState() {
        return lastStateTransition.getNewState();
    }

    @Deprecated
    public boolean isPlaying() {
        return lastStateTransition.isPlaying();
    }

    @Deprecated
    public boolean isPlayerPlaying() {
        return lastStateTransition.isPlayerPlaying();
    }

    @Deprecated
    public boolean isBuffering() {
        return lastStateTransition.isBuffering();
    }

    @Deprecated
    public boolean playbackHasPaused() {
        return lastStateTransition.isPaused();
    }

    @Override
    public void play(PublicApiTrack track) {
        trackPlaybackInfo = new TrackPlaybackInfo(track);
        configureNextPlayaToUseViaPreferences();
        currentPlaya.play(track);
    }

    @Override
    public void play(PublicApiTrack track, long fromPos) {
        trackPlaybackInfo = new TrackPlaybackInfo(track);
        configureNextPlayaToUseViaPreferences();
        currentPlaya.play(track, fromPos);
    }

    @Override
    public void playUninterrupted(PublicApiTrack track) {
        trackPlaybackInfo = new TrackPlaybackInfo(track);
        configureNextPlayaToUseViaPreferences();
        currentPlaya.playUninterrupted(track);
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
    public boolean isSeekable() {
        return currentPlaya.isSeekable();
    }

    @Override
    public boolean isNotSeekablePastBuffer() {
        return currentPlaya.isNotSeekablePastBuffer();
    }


    @Override
    public void destroy() {
        // call stop first as it will save the queue/position
        mediaPlayaDelegate.destroy();
        if(!skippyFailedToInitialize) {
            skippyPlayaDelegate.destroy();
        }
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
        Preconditions.checkNotNull(playaListener, "Stream Player Listener is unexpectedly null when passing state");
        lastStateTransition = stateTransition;
        playaListener.onPlaystateChanged(stateTransition);
    }

    @Override
    public void onProgressEvent(long progress, long duration) {
        playaListener.onProgressEvent(progress, duration);
    }

    @Override
    public boolean requestAudioFocus() {
        Preconditions.checkNotNull(playaListener, "Stream Player Listener is unexpectedly null when requesting audio focus");
        return playaListener.requestAudioFocus();
    }

    public void startBufferingMode(TrackUrn trackUrn){
        lastPlaya = currentPlaya;
        currentPlaya = bufferingPlayaDelegate;

        lastStateTransition = new StateTransition(PlayaState.BUFFERING, Reason.NONE, trackUrn);
        onPlaystateChanged(lastStateTransition);

        if (lastPlaya != null) {
            lastPlaya.setListener(null);
            lastPlaya.stop();
        }

    }

    private void configureNextPlayaToUseViaPreferences(){
        configureNextPlayaToUse(getNextPlaya());
    }

    private void configureNextPlayaToUse(Playa nextPlaya){
        if (currentPlaya != null && currentPlaya != nextPlaya){
            currentPlaya.stop();
        }

        currentPlaya = nextPlaya;
        currentPlaya.setListener(this);
        updateConsecutivePlays(currentPlaya != lastPlaya);
    }

    private void updateConsecutivePlays(boolean changedPlayers) {
        if (changedPlayers){
            playbackPreferences.edit().putInt(PLAYS_ON_CURRENT_PLAYER, 1).apply();
        } else {
            int plays = playbackPreferences.getInt(PLAYS_ON_CURRENT_PLAYER, 0);
            playbackPreferences.edit().putInt(PLAYS_ON_CURRENT_PLAYER, plays + 1).apply();
        }
    }

    private Playa getNextPlaya() {
        if (skippyFailedToInitialize){
            return mediaPlayaDelegate;
        }

        return skippyPlayaDelegate;

    }

    private static class TrackPlaybackInfo {
        private final PublicApiTrack track;

        private TrackPlaybackInfo(PublicApiTrack track) {
            this.track = track;
        }

        public PublicApiTrack getTrack() {
            return track;
        }
    }
}
