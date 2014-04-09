package com.soundcloud.android.playback.service;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import com.soundcloud.android.model.Track;
import com.soundcloud.android.playback.service.mediaplayer.MediaPlayerAdapter;
import com.soundcloud.android.playback.service.skippy.SkippyAdapter;
import com.soundcloud.android.preferences.DevSettings;
import com.soundcloud.android.utils.SharedPreferencesUtils;

import android.content.SharedPreferences;
import android.util.Log;

import javax.inject.Inject;
//Not a hater
public class StreamPlaya implements Playa, Playa.PlayaListener {


    public static final String TAG = "StreamPlaya";
    @VisibleForTesting
    static final String PLAYS_SINCE_SKIPPY = "StreamPlaya.playsSinceSkippy";
    @VisibleForTesting
    static final int MAX_PLAYS_OFF_SKIPPY = 3; // TODO increase to 10 before launch

    private final MediaPlayerAdapter mMediaPlayaDelegate;
    private final SkippyAdapter mSkippyPlayaDelegate;
    private final BufferingPlaya mBufferingPlayaDelegate;
    private final SharedPreferences mSharedPreferences;

    private Playa mCurrentPlaya;
    private PlayaListener mPlayaListener;

    // store start info so we can fallback and retry after Skippy failures
    private StartInfo mPlayStartInfo;

    @Inject
    public StreamPlaya(SharedPreferences sharedPreferences, MediaPlayerAdapter mediaPlayerAdapter,
                       SkippyAdapter skippyAdapter, BufferingPlaya bufferingPlaya){
        mSharedPreferences = sharedPreferences;
        mMediaPlayaDelegate = mediaPlayerAdapter;
        mSkippyPlayaDelegate = skippyAdapter;
        mBufferingPlayaDelegate = bufferingPlaya;
        mCurrentPlaya = mBufferingPlayaDelegate;
    }

    @Override
    public void play(Track track) {
        mPlayStartInfo = new StartInfo(track, 0L);
        configureNextPlayaToUseViaPreferences();
        mCurrentPlaya.play(track);
    }

    @Override
    public void play(Track track, long fromPos) {
        mPlayStartInfo = new StartInfo(track, fromPos);
        configureNextPlayaToUseViaPreferences();
        mCurrentPlaya.play(track, fromPos);
    }

    @Override
    public boolean resume() {
        return mCurrentPlaya.resume();
    }

    @Override
    public void pause() {
        mCurrentPlaya.pause();
    }

    @Override
    public long seek(long ms, boolean performSeek) {
        return mCurrentPlaya.seek(ms, performSeek);
    }

    @Override
    public long getProgress() {
        return mCurrentPlaya.getProgress();
    }

    @Override
    public void setVolume(float v) {
        mCurrentPlaya.setVolume(v);
    }

    @Override
    public void stop() {
        mCurrentPlaya.stop();
    }

    @Override
    public StateTransition getLastStateTransition() {
        return mCurrentPlaya.getLastStateTransition();
    }

    @Override
    public PlayaState getState() {
        return mCurrentPlaya.getState();
    }

    @Override
    public boolean isSeekable() {
        return mCurrentPlaya.isSeekable();
    }

    @Override
    public boolean isNotSeekablePastBuffer() {
        return mCurrentPlaya.isNotSeekablePastBuffer();
    }

    @Override
    public boolean isPlaying() {
        return mCurrentPlaya.isPlaying();
    }

    @Override
    public boolean isPlayerPlaying() {
        return mCurrentPlaya.isPlayerPlaying();
    }

    @Override
    public boolean isBuffering() {
        return mCurrentPlaya.isBuffering();
    }

    @Override
    public void destroy() {
        // call stop first as it will save the queue/position
        mMediaPlayaDelegate.destroy();
        mSkippyPlayaDelegate.destroy();
    }

    @Override
    public void setListener(PlayaListener playaListener) {
        mPlayaListener = playaListener;
        if (mCurrentPlaya != null){
            mCurrentPlaya.setListener(playaListener);
        }
    }

    @Override
    public void onPlaystateChanged(StateTransition stateTransition) {
        if (mCurrentPlaya == mSkippyPlayaDelegate &&
                stateTransition.wasError() && mSkippyPlayaDelegate.getProgress() == mPlayStartInfo.getStartPosition()) {
            Log.i(TAG, "Falling back to MediaPlayer");

            configureNextPlayaToUse(mMediaPlayaDelegate);
            mMediaPlayaDelegate.play(mPlayStartInfo.getTrack(), mPlayStartInfo.getStartPosition());

        } else {
            Preconditions.checkNotNull(mPlayaListener, "Stream Player Listener is unexpectedly null when passing state");
            mPlayaListener.onPlaystateChanged(stateTransition);
        }
    }

    @Override
    public boolean requestAudioFocus() {
        Preconditions.checkNotNull(mPlayaListener, "Stream Player Listener is unexpectedly null when requesting audio focus");
        return mPlayaListener.requestAudioFocus();
    }

    public void startBufferingMode(){
        final Playa lastPlaya = mCurrentPlaya;
        mCurrentPlaya = mBufferingPlayaDelegate;
        onPlaystateChanged(mBufferingPlayaDelegate.getLastStateTransition());

        if (lastPlaya != null) {
            lastPlaya.setListener(null);
            lastPlaya.stop();
        }

    }

    private void configureNextPlayaToUseViaPreferences(){
        configureNextPlayaToUse(getNextPlayaViaPreferences());
    }
    private void configureNextPlayaToUse(Playa nextPlaya){

        mCurrentPlaya = nextPlaya;
        mCurrentPlaya.setListener(this);

        if (mCurrentPlaya == mSkippyPlayaDelegate){
            SharedPreferencesUtils.apply(mSharedPreferences.edit().putInt(PLAYS_SINCE_SKIPPY, 0));
            Log.i(TAG, "Configuring Playa to SkippyPlaya");
        } else {
            final int plays = mSharedPreferences.getInt(PLAYS_SINCE_SKIPPY, 0);
            SharedPreferencesUtils.apply(mSharedPreferences.edit().putInt(PLAYS_SINCE_SKIPPY, plays + 1));
            Log.i(TAG, "Configuring Playa to MediaPlaya");
        }
    }

    private Playa getNextPlayaViaPreferences() {
        if (mSharedPreferences.getBoolean(DevSettings.DEV_ENABLE_SKIPPY, false)) {
            return mSkippyPlayaDelegate;
        } else if (mSharedPreferences.getInt(PLAYS_SINCE_SKIPPY, 0) >= MAX_PLAYS_OFF_SKIPPY) {
            return mSkippyPlayaDelegate;
        } else {
            return mMediaPlayaDelegate;
        }
    }

    private static class StartInfo {
        private final Track track;
        private final long startPosition;

        private StartInfo(Track track, long startPosition) {
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
