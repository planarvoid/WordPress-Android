package com.soundcloud.android.playback.mediaplayer;

import android.content.Context;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.SurfaceHolder;

import com.soundcloud.android.Consts;
import com.soundcloud.android.events.PlayerType;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playback.PlaybackConstants;
import com.soundcloud.android.playback.PlaybackProtocol;
import com.soundcloud.android.playback.Player;
import com.soundcloud.android.playback.VideoPlaybackItem;
import com.soundcloud.android.utils.CurrentDateProvider;
import com.soundcloud.android.utils.NetworkConnectionHelper;
import com.soundcloud.annotations.VisibleForTesting;

import java.io.IOException;
import java.lang.ref.WeakReference;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class VideoPlayerAdapter implements Player, SurfaceHolder.Callback, MediaPlayer.OnPreparedListener,
        MediaPlayer.OnBufferingUpdateListener, MediaPlayer.OnErrorListener, MediaPlayer.OnInfoListener {

    public static final String TAG = "VideoPlayerAdapter";
    public static final int MAX_CONNECT_RETRIES = 2;

    private final Context context;
    private final MediaPlayerManager mediaPlayerManager;
    private final NetworkConnectionHelper networkConnectionHelper;
    private final CurrentDateProvider currentDateProvider;
    private final PlayerHandler playerHandler;

    private volatile MediaPlayer mediaPlayer;
    private SurfaceHolder surfaceHolder;
    private PlayerListener playerListener;

    private PlaybackState internalState = PlaybackState.STOPPED;
    private VideoPlaybackItem videoPlaybackItem;
    private int connectionRetries = 0;
    private double loadPercent;

    @Inject
    public VideoPlayerAdapter(Context context,
                              MediaPlayerManager mediaPlayerManager,
                              NetworkConnectionHelper networkConnectionHelper,
                              CurrentDateProvider currentDateProvider,
                              PlayerHandler playerHandler) {
        this.context = context.getApplicationContext();
        this.mediaPlayerManager = mediaPlayerManager;
        this.networkConnectionHelper = networkConnectionHelper;
        this.currentDateProvider = currentDateProvider;
        this.playerHandler = playerHandler;
        this.playerHandler.setVideoPlayerAdapterWeakReference(this);
    }

    @Override
    public void playVideo(VideoPlaybackItem videoPlaybackItem) {
        if (mediaPlayer == null || releaseUnresettableMediaPlayer()) {
            createMediaPlayer();
        } else {
            mediaPlayer.reset();
        }

        setInternalState(PlaybackState.PREPARING);
        this.videoPlaybackItem = videoPlaybackItem;

        try {
            final String videoUrl = this.videoPlaybackItem.getSources().get(0).getUrl();
            mediaPlayer.setDisplay(surfaceHolder);
            mediaPlayer.setScreenOnWhilePlaying(true);
            mediaPlayer.setDataSource(videoUrl);
            mediaPlayer.prepareAsync();
        } catch (IOException e) {
            Log.d(TAG, "Failed to load video media :/");
        }
    }

    @Override
    public void onPrepared(MediaPlayer mediaPlayer) {
        if (mediaPlayer.equals(this.mediaPlayer) && internalState == PlaybackState.PREPARING) {
            if (playerListener != null && playerListener.requestAudioFocus()) {
                play();
            } else {
                setInternalState(PlaybackState.PAUSED);
                Log.e(TAG, "Could not acquire audio focus");
            }
        } else {
            Log.e(TAG, "OnPrepared called unexpectedly in state " + internalState);
        }
    }

    private void play() {
        if (mediaPlayer != null) {
            mediaPlayer.start();
            setInternalState(PlaybackState.PLAYING);
        }
    }

    @Override
    public void play(Urn track) {
        throw new IllegalStateException("VideoPlayerAdapter cannot play tracks!");
    }

    @Override
    public void play(Urn track, long fromPos) {
        throw new IllegalStateException("VideoPlayerAdapter cannot play tracks!");
    }

    @Override
    public void playUninterrupted(Urn track) {
        throw new IllegalStateException("VideoPlayerAdapter cannot play tracks!");
    }

    @Override
    public void playOffline(Urn track, long fromPos) {
        throw new IllegalStateException("VideoPlayerAdapter cannot play tracks!");
    }

    @Override
    public long seek(long ms, boolean performSeek) {
        throw new IllegalStateException("VideoPlayerAdapter cannot seek!");
    }

    @Override
    public boolean isSeekable() {
        return false;
    }

    @Override
    public void resume() {
        if (mediaPlayer != null && internalState.isStartable()) {
            play();
        }
    }

    @Override
    public void pause() {
        if (mediaPlayer != null && internalState.isPausable()) {
            mediaPlayer.pause();
            setInternalState(PlaybackState.PAUSED);
        }
    }

    @Override
    public long getProgress() {
        if (mediaPlayer != null && internalState.canGetMPProgress()) {
            return mediaPlayer.getCurrentPosition();
        } else {
            return 0;
        }
    }

    private void sendProgress() {
        if (playerListener != null) {
            final long progress = getProgress();
            final long duration = getDuration();

            playerListener.onProgressEvent(progress, duration);
        }
    }

    public long getDuration() {
        if (mediaPlayer != null && internalState.canGetMPProgress()) {
            return mediaPlayer.getDuration();
        } else {
            return Consts.NOT_SET;
        }
    }

    void onTrackEnded() {
        resetConnectionRetries();
        setInternalState(PlaybackState.COMPLETED);
    }

    @Override
    public void setVolume(float v) {
        if (mediaPlayer != null) {
            mediaPlayer.setVolume(v, v);
        }
    }

    @Override
    public void stop() {
        final MediaPlayer mediaPlayer = this.mediaPlayer;
        if (mediaPlayer != null) {
            final long progress = getProgress();
            final long duration = getDuration();

            if (internalState.isStoppable()) {
                mediaPlayer.stop();
            }

            mediaPlayerManager.stopAndReleaseAsync(mediaPlayer);
            this.mediaPlayer = null;

            setInternalState(PlaybackState.STOPPED, progress, duration);
        }
    }

    void stop(MediaPlayer mediaPlayer) {
        if (mediaPlayer == this.mediaPlayer) {
            stop();
        }
    }

    @Override
    public void stopForTrackTransition() {
        stop();
    }

    @Override
    public void destroy() {
        stop();
        playerHandler.removeCallbacksAndMessages(null);
    }

    @Override
    public void setListener(PlayerListener playerListener) {
        this.playerListener = playerListener;
    }

    @Override
    public void onBufferingUpdate(MediaPlayer mp, int percent) {
        if (mediaPlayer == mp) {
            if (Log.isLoggable(TAG, Log.DEBUG) && loadPercent != percent) {
                Log.d(TAG, "onBufferingUpdate(" + percent + ")");
                loadPercent = percent;
            }
        }
    }

    @Override
    public boolean onInfo(MediaPlayer mp, int what, int extra) {
        if (Log.isLoggable(TAG, Log.DEBUG)) {
            Log.d(TAG, "onInfo(" + what + "," + extra + ", state=" + internalState + ")");
        }

        if (internalState == PlaybackState.PREPARING) {
            return true; // swallow info callbacks if preparing. HTC Bug
        } else if (MediaPlayer.MEDIA_INFO_BUFFERING_START == what) {
            setInternalState(PlaybackState.PAUSED_FOR_BUFFERING);
            return true;
        } else if (MediaPlayer.MEDIA_INFO_BUFFERING_END == what) {
            if (!internalState.isSupposedToBePlaying()) {
                pause();
            } else {
                // still playing back, set proper state after buffering state
                setInternalState(PlaybackState.PLAYING);
            }
            return true;
        }

        return false;
    }

    void resetConnectionRetries() {
        connectionRetries = 0;
    }

    @Override
    public boolean onError(MediaPlayer mp, int what, int extra) {
        return handleMediaPlayerError(mp);
    }

    private boolean handleMediaPlayerError(MediaPlayer mp) {
        if (mp.equals(mediaPlayer) && internalState != PlaybackState.STOPPED) {
            if (connectionRetries++ < MAX_CONNECT_RETRIES) {
                Log.d(TAG, "stream disconnected, retrying (try=" + connectionRetries + ")");
                setInternalState(PlaybackState.ERROR_RETRYING);
                playVideo(videoPlaybackItem); // TODO: Have playVideo(time) for restarting from error?
            } else {
                Log.d(TAG, "stream disconnected, giving up");
                setInternalState(PlaybackState.ERROR);
                mp.release();
                resetConnectionRetries();
                mediaPlayer = null;
            }
        }
        return true;
    }

    private void createMediaPlayer() {
        mediaPlayer = mediaPlayerManager.create();
        mediaPlayer.setOnPreparedListener(this);
        mediaPlayer.setOnErrorListener(this);
        mediaPlayer.setOnInfoListener(this);
        mediaPlayer.setOnBufferingUpdateListener(this);
        mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
        mediaPlayer.setOnCompletionListener(new VideoCompletionListener(this));
    }

    private boolean releaseUnresettableMediaPlayer() {
        if (internalState.isLoading()) {
            mediaPlayerManager.stopAndReleaseAsync(mediaPlayer);
            mediaPlayer = null;
            return true;
        }
        return false;
    }

    private void setInternalState(PlaybackState playbackState) {
        internalState = playbackState;
        reportStateChange(playbackState, getProgress(), getDuration());
    }

    private void setInternalState(PlaybackState playbackState, long progress, long duration) {
        internalState = playbackState;
        reportStateChange(playbackState, progress, duration);
    }

    private void reportStateChange(PlaybackState playbackState, long progress, long duration) {
        playerHandler.removeMessages(PlayerHandler.SEND_PROGRESS);
        if (playbackState == PlaybackState.PLAYING) {
            playerHandler.sendEmptyMessage(PlayerHandler.SEND_PROGRESS);
        }

        if (playerListener != null && videoPlaybackItem != null) {
            final StateTransition stateTransition = new StateTransition(getTranslatedState(), getTranslatedReason(), videoPlaybackItem.getAdUrn(), progress, duration, currentDateProvider);
            stateTransition.addExtraAttribute(StateTransition.EXTRA_PLAYBACK_PROTOCOL, PlaybackProtocol.HTTPS.getValue());
            stateTransition.addExtraAttribute(StateTransition.EXTRA_PLAYER_TYPE, PlayerType.VIDEO_PLAYER.getValue());
            stateTransition.addExtraAttribute(StateTransition.EXTRA_NETWORK_AND_WAKE_LOCKS_ACTIVE, "false");
            stateTransition.addExtraAttribute(StateTransition.EXTRA_CONNECTION_TYPE, networkConnectionHelper.getCurrentConnectionType().getValue());
            playerListener.onPlaystateChanged(stateTransition);
        }
    }

    private PlayerState getTranslatedState() {
        switch (internalState) {
            case PREPARING:
            case PAUSED_FOR_BUFFERING:
                return PlayerState.BUFFERING;
            case PLAYING:
                return PlayerState.PLAYING;
            case ERROR:
            case COMPLETED:
            case PAUSED:
            case STOPPED:
            case ERROR_RETRYING:
                return PlayerState.IDLE;
            default:
                throw new IllegalArgumentException("No translated state for " + internalState);
        }
    }

    private Reason getTranslatedReason() {
        switch (internalState) {
            case ERROR:
                return networkConnectionHelper.isNetworkConnected() ? Reason.ERROR_NOT_FOUND : Reason.ERROR_FAILED;
            case COMPLETED:
                return Reason.PLAYBACK_COMPLETE;
            default:
                return Reason.NONE;
        }
    }

    boolean isInErrorState() {
        return internalState.isError();
    }

    private void updateVideoViewHolder(SurfaceHolder holder) {
        if (surfaceHolder != holder) {
            surfaceHolder = holder;
            if (mediaPlayer != null) {
                mediaPlayer.setDisplay(surfaceHolder);
            }
        }
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        updateVideoViewHolder(holder);
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {}

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        updateVideoViewHolder(null);
    }

    static class PlayerHandler extends Handler {
        static final int SEND_PROGRESS = 0;

        private WeakReference<VideoPlayerAdapter> videoPlayerAdapterWeakReference;

        @Inject
        PlayerHandler() {}

        @VisibleForTesting
        void setVideoPlayerAdapterWeakReference(VideoPlayerAdapter adapter) {
            videoPlayerAdapterWeakReference = new WeakReference<>(adapter);
        }

        @Override
        public void handleMessage(Message msg) {
            final VideoPlayerAdapter videoPlayerAdapter = videoPlayerAdapterWeakReference.get();
            if (videoPlayerAdapter == null) {
                return;
            }

            switch (msg.what) {
                case SEND_PROGRESS:
                    videoPlayerAdapter.sendProgress();
                    sendEmptyMessageDelayed(SEND_PROGRESS, PlaybackConstants.PROGRESS_DELAY_MS);
                    break;
            }
        }
    }

    static class VideoCompletionListener implements MediaPlayer.OnCompletionListener {
        private final VideoPlayerAdapter videoPlayerAdapter;

        VideoCompletionListener(VideoPlayerAdapter videoPlayerAdapter) {
            this.videoPlayerAdapter = videoPlayerAdapter;
        }

        @Override
        public void onCompletion(MediaPlayer mediaPlayer) {
            if (videoPlayerAdapter.isInErrorState()) {
                videoPlayerAdapter.stop(mediaPlayer);
            } else {
                videoPlayerAdapter.onTrackEnded();
            }
        }
    }
}

