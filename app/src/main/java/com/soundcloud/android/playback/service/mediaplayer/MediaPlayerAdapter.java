package com.soundcloud.android.playback.service.mediaplayer;

import com.google.common.annotations.VisibleForTesting;
import com.soundcloud.android.model.Track;
import com.soundcloud.android.playback.service.MediaPlayerDataSourceObserver;
import com.soundcloud.android.playback.service.Playa;
import com.soundcloud.android.playback.streaming.StreamProxy;
import rx.android.schedulers.AndroidSchedulers;

import android.content.Context;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Build;
import android.os.Handler;
import android.os.Message;
import android.os.PowerManager;
import android.util.Log;

import javax.annotation.Nullable;
import javax.inject.Inject;
import java.lang.ref.WeakReference;

public class MediaPlayerAdapter implements Playa, MediaPlayer.OnPreparedListener, MediaPlayer.OnErrorListener,
        MediaPlayer.OnSeekCompleteListener, MediaPlayer.OnInfoListener,MediaPlayer.OnBufferingUpdateListener {

    public static final int MAX_CONNECT_RETRIES = 3;
    public static final int SEEK_COMPLETE_PROGRESS_DELAY = 3000;

    private static final int POS_NOT_SET = -1;
    private final String TAG = "MediaPlayerAdapter";

    private final StreamProxy mProxy;
    private final Context mContext;
    private final MediaPlayerManager mMediaPlayerManager;

    private final PlayerHandler mPlayerHandler;

    private PlaybackState mInternalState = PlaybackState.STOPPED;

    private Track mTrack;
    private int mConnectRetries = 0;

    private boolean mWaitingForSeek;
    private long mSeekPos = POS_NOT_SET;
    private long mResumePos = POS_NOT_SET;

    @Nullable
    private volatile MediaPlayer mMediaPlayer;
    private double mLoadPercent;
    @Nullable
    private PlayaListener mPlayaListener;


    @Inject
    public MediaPlayerAdapter(Context context, MediaPlayerManager mediaPlayerManager, StreamProxy streamProxy,
                              PlayerHandler playerHandler) {
        mContext = context.getApplicationContext();
        mMediaPlayerManager = mediaPlayerManager;
        mProxy = streamProxy;
        mPlayerHandler = playerHandler;
        mPlayerHandler.setMediaPlayerAdapter(this);

        // perhaps start this lazily?
        mProxy.start();
    }

    @Override
    public void play(Track track) {
        play(track, POS_NOT_SET);
    }

    @Override
    public void play(Track track, long fromPos) {
        if (mMediaPlayer == null || releaseUnresettableMediaPlayer()) {
            createMediaPlayer();
        } else {
            // do we need to stop it if it's playing?
            mMediaPlayer.reset();
        }

        mTrack = track;
        mWaitingForSeek = false;
        mResumePos = fromPos;
        mSeekPos = POS_NOT_SET;

        setInternalState(PlaybackState.PREPARING);

        final MediaPlayerDataSourceObserver observer = new MediaPlayerDataSourceObserver(mMediaPlayer, this);
        mProxy.uriObservable(mTrack.getStreamUrlWithAppendedId(), null).subscribe(observer, AndroidSchedulers.mainThread());
    }

    @Override
    public void onPrepared(MediaPlayer mp) {
        if (mp.equals(mMediaPlayer) && mInternalState == PlaybackState.PREPARING) {

            mConnectRetries = 0;
            if (mPlayaListener != null && mPlayaListener.requestAudioFocus()) {
                play();
                if (mResumePos != POS_NOT_SET) {
                    seek(mResumePos, true);
                }
            } else {
                setInternalState(PlaybackState.PAUSED);
                Log.e(TAG, "Could not acquire audio focus");
            }
            mResumePos = POS_NOT_SET;

        } else {
            // when could this possibly happen??
            Log.e(TAG, "OnPrepared called unexpectedly in state " + mInternalState);
        }
    }

    private void play() {
        if (mMediaPlayer != null){
            mMediaPlayer.start();
            setInternalState(PlaybackState.PLAYING);
        }
    }

    @Override
    public boolean onError(MediaPlayer mp, int what, int extra) {
        return handleMediaPlayerError(mp, POS_NOT_SET);

    }

    private boolean handleMediaPlayerError(MediaPlayer mp, long resumePosition) {
        //noinspection ObjectEquality
        if (mp.equals(mMediaPlayer) && mInternalState != PlaybackState.STOPPED) {

            if (mConnectRetries++ < MAX_CONNECT_RETRIES) {
                Log.d(TAG, "stream disconnected, retrying (try=" + mConnectRetries + ")");
                setInternalState(PlaybackState.ERROR_RETRYING);
                play(mTrack, resumePosition);
                return true;
            }

            Log.d(TAG, "stream disconnected, giving up");
            setInternalState(PlaybackState.ERROR);
            mp.release();
            mConnectRetries = 0;
            mMediaPlayer = null;
        }
        return false;
    }

    @Override
    public void onSeekComplete(MediaPlayer mp) {
        if (Log.isLoggable(TAG, Log.DEBUG)) {
            Log.d(TAG, "onSeekComplete(state=" + mInternalState + ")");
        }
        //noinspection ObjectEquality
        if (mMediaPlayer == mp) {

            // only clear seek if we are not buffering. If we are buffering, it will be cleared after buffering completes
            if (mInternalState != PlaybackState.PAUSED_FOR_BUFFERING) {
                // keep the last seek time for 3000 ms because getCurrentPosition will be incorrect at first. this way if seeking fails,
                // we can resume from the proper position
                mPlayerHandler.removeMessages(PlayerHandler.CLEAR_LAST_SEEK);
                mPlayerHandler.sendEmptyMessageDelayed(PlayerHandler.CLEAR_LAST_SEEK, SEEK_COMPLETE_PROGRESS_DELAY);

            } else {
                Log.d(TAG, "Not clearing seek, waiting for buffer");
            }

            mWaitingForSeek = false;

            // respect pauses during seeks
            if (!mInternalState.isSupposedToBePlaying()) {
                pause();
            } else if (Build.VERSION.SDK_INT > Build.VERSION_CODES.JELLY_BEAN) {
                // KitKat sucks, and doesn't resume playback after seeking sometimes, with no discernible
                // output. Toggling playback seems to fix it
                mp.pause();
                mp.start();
            }
        }
    }

    public long getSeekPosition() {
        return mSeekPos;
    }

    @Override
    public boolean onInfo(MediaPlayer mp, int what, int extra) {
        if (Log.isLoggable(TAG, Log.DEBUG)) {
            Log.d(TAG, "onInfo(" + what + "," + extra + ", state=" + mInternalState + ")");
        }

        switch (what) {
            case MediaPlayer.MEDIA_INFO_BUFFERING_START:
                setInternalState(PlaybackState.PAUSED_FOR_BUFFERING);
                mPlayerHandler.removeMessages(PlayerHandler.CLEAR_LAST_SEEK);
                break;

            case MediaPlayer.MEDIA_INFO_BUFFERING_END:
                if (mSeekPos != -1 && !mWaitingForSeek) {
                    mPlayerHandler.removeMessages(PlayerHandler.CLEAR_LAST_SEEK);
                    mPlayerHandler.sendEmptyMessageDelayed(PlayerHandler.CLEAR_LAST_SEEK, 3000);
                } else if (Log.isLoggable(TAG, Log.DEBUG)) {
                    Log.d(TAG, "Not clearing seek, waiting for seek to finish");
                }

                if (!mInternalState.isSupposedToBePlaying()) {
                    pause();
                } else {
                    // still playing back, set proper state after buffering state
                    setInternalState(PlaybackState.PLAYING);
                }
                break;
            default:
        }
        return true;
    }

    @Override
    public void onBufferingUpdate(MediaPlayer mp, int percent) {
        if (mMediaPlayer == mp) {
            if (Log.isLoggable(TAG, Log.DEBUG) && mLoadPercent != percent) {
                Log.d(TAG, "onBufferingUpdate(" + percent + ")");
            }

            mLoadPercent = percent;
        }
    }

    void onTrackEnded() {
        setInternalState(PlaybackState.COMPLETED);
    }

    void setResumeTimeAndInvokeErrorListener(MediaPlayer mediaPlayer, long lastPosition) {
        if (mediaPlayer == mMediaPlayer){
            handleMediaPlayerError(mediaPlayer, lastPosition);
        }
    }

    public boolean hasValidSeekPosition() {
        return mSeekPos != POS_NOT_SET;
    }

    public boolean isTryingToResumeTrack() {
        return mResumePos != POS_NOT_SET;
    }

    public long getResumeTime() {
        return mResumePos;
    }

    void stop(MediaPlayer mediaPlayer) {
        if (mediaPlayer == mMediaPlayer){
            stop();
        }
    }

    private boolean releaseUnresettableMediaPlayer() {
        if (mWaitingForSeek || mInternalState.isLoading()) {
            mMediaPlayerManager.stopAndReleaseAsync(mMediaPlayer);
            return true;
        }
        return false;
    }

    private void createMediaPlayer() {
        mMediaPlayer = mMediaPlayerManager.create();
        mMediaPlayer.setWakeMode(mContext, PowerManager.PARTIAL_WAKE_LOCK);
        mMediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
        mMediaPlayer.setOnErrorListener(this);
        mMediaPlayer.setOnPreparedListener(this);
        mMediaPlayer.setOnSeekCompleteListener(this);
        mMediaPlayer.setOnInfoListener(this);
        mMediaPlayer.setOnCompletionListener(new TrackCompletionListener(this));
    }

    private void setInternalState(PlaybackState playbackState) {
        mInternalState = playbackState;

        if (mPlayaListener != null) {
            mPlayaListener.onPlaystateChanged(getLastStateTransition());
        }
    }

    public boolean isPlaying(){
        return getState().isPlaying();
    }

    public boolean isPlayerPlaying(){
        return getState().isPlayerPlaying();
    }

    public boolean isBuffering(){
        return getState().isBuffering();
    }

    @Override
    public boolean resume() {
        if (mMediaPlayer != null && mInternalState.isStartable()) {
            play();
            return true;
        } else {
            return false;
        }
    }

    @Override
    public void pause() {
        if (mMediaPlayer != null && mInternalState.isPausable()) {
            mMediaPlayer.pause();
            setInternalState(PlaybackState.PAUSED);
        }
    }

    @Override
    public void destroy() {
        stop();
        // make sure there aren't any other messages coming
        mPlayerHandler.removeCallbacksAndMessages(null);

        if (mProxy != null && mProxy.isRunning()) {
            mProxy.stop();
        }
    }

    @Override
    public void setListener(PlayaListener playaListener) {
        mPlayaListener = playaListener;
    }

    public long seek(long ms) {
        return seek(ms, true);
    }

    @Override
    public long seek(long ms, boolean performSeek) {
        if (isSeekable()) {
            if (ms <= 0) {
                throw new IllegalArgumentException("Trying to seek before 0");
            }

            final long currentPos = (mMediaPlayer != null && !mInternalState.isError()) ? mMediaPlayer.getCurrentPosition() : 0;
            // workaround for devices which can't do content-range requests
            if ((isNotSeekablePastBuffer() && isPastBuffer(ms)) || mMediaPlayer == null) {
                Log.d(TAG, "MediaPlayer bug: cannot seek past buffer");
                return currentPos;
            } else {
                long duration = mMediaPlayer.getDuration();

                final long newPos;
                // don't go before the playhead if they are trying to seek
                // beyond, just maintain their current position
                if (ms > currentPos && currentPos > duration) {
                    newPos = currentPos;
                } else if (ms > duration) {
                    newPos = duration;
                } else {
                    newPos = ms;
                }

                if (performSeek && newPos != currentPos) {
                    if (Log.isLoggable(TAG, Log.DEBUG)) {
                        Log.d(TAG, "seeking to " + newPos);
                    }
                    mSeekPos = newPos;
                    mWaitingForSeek = true;
                    mMediaPlayer.seekTo((int) newPos);
                }
                return newPos;
            }
        } else {
            return -1;
        }
    }

    @Override
    public long getProgress() {
        if (mResumePos != POS_NOT_SET) {
            return mResumePos;
        } else if (mWaitingForSeek) {
            return mSeekPos;
        } else if (mMediaPlayer != null && !mInternalState.isError() && mInternalState != PlaybackState.PREPARING) {
            return mMediaPlayer.getCurrentPosition();
        } else {
            return 0;
        }
    }

    @Override
    public StateTransition getLastStateTransition() {
        return new StateTransition(getState(), getTranslatedReason());
    }

    @Override
    public PlayaState getState() {
        PlayaState playaState = getTranslatedState();
        return playaState == null ? PlayaState.IDLE : playaState;
    }

    private PlayaState getTranslatedState() {
        switch (mInternalState) {
            case PREPARING:
            case PAUSED_FOR_BUFFERING:
                return PlayaState.BUFFERING;
            case PLAYING:
                return PlayaState.PLAYING;
            case ERROR:
            case COMPLETED:
            case PAUSED:
            case STOPPED:
            case ERROR_RETRYING:
                return PlayaState.IDLE;
            default:
                throw new IllegalArgumentException("No translated state for " + mInternalState);
        }
    }

    private Reason getTranslatedReason() {
        switch (mInternalState) {
            case ERROR:
                return Reason.ERROR_FAILED;
            case COMPLETED:
                return Reason.COMPLETE;
            default:
                return Reason.NONE;
        }
    }

    @Override
    public boolean isSeekable() {
        return mMediaPlayer != null && mInternalState.isSeekable();
    }

    @Override
    public boolean isNotSeekablePastBuffer() {
        return Build.VERSION.SDK_INT <= Build.VERSION_CODES.FROYO && StreamProxy.isOpenCore();
    }

    @Override
    public void setVolume(float volume) {
        if (mMediaPlayer != null){
            mMediaPlayer.setVolume(volume, volume);
        }
    }

    @Override
    public void stop() {
        if (mMediaPlayer != null && mInternalState.isStoppable()) {
            mMediaPlayer.stop();
            setInternalState(PlaybackState.STOPPED);
        }
    }

    private boolean isPastBuffer(long pos) {
        return mMediaPlayer == null || (pos / (double) mMediaPlayer.getDuration()) * 100 > mLoadPercent;
    }

    @VisibleForTesting
    static class MediaPlayerManager {
        @Inject
        public MediaPlayerManager() {
        }

        public MediaPlayer create() {
            return new MediaPlayer();
        }

        public void stopAndReleaseAsync(final MediaPlayer mediaPlayer) {
            new Thread() {
                @Override
                public void run() {
                    mediaPlayer.reset();
                    mediaPlayer.release();
                }
            }.start();
        }
    }

    @VisibleForTesting
    static class PlayerHandler extends Handler {

        static final int CLEAR_LAST_SEEK = 0;
        private WeakReference<MediaPlayerAdapter> mediaPlayerAdapterWeakReference;

        @Inject
        PlayerHandler() {
        }

        @VisibleForTesting
        void setMediaPlayerAdapter(MediaPlayerAdapter adapter) {
            mediaPlayerAdapterWeakReference = new WeakReference<MediaPlayerAdapter>(adapter);
        }

        @Override
        public void handleMessage(Message msg) {
            final MediaPlayerAdapter mediaPlayerAdapter = mediaPlayerAdapterWeakReference.get();
            if (mediaPlayerAdapter == null) {
                return;
            }

            switch (msg.what) {
                case CLEAR_LAST_SEEK:
                    mediaPlayerAdapter.mSeekPos = -1;
                    break;

            }
        }


    }
}
