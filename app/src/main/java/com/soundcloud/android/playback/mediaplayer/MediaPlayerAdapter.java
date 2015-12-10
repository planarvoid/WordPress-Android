package com.soundcloud.android.playback.mediaplayer;

import com.soundcloud.android.Consts;
import com.soundcloud.android.accounts.AccountOperations;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.PlaybackPerformanceEvent;
import com.soundcloud.android.events.PlayerType;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playback.BufferUnderrunListener;
import com.soundcloud.android.playback.PlaybackConstants;
import com.soundcloud.android.playback.PlaybackProtocol;
import com.soundcloud.android.playback.Player;
import com.soundcloud.android.playback.StreamUrlBuilder;
import com.soundcloud.android.playback.VideoPlaybackItem;
import com.soundcloud.android.utils.CurrentDateProvider;
import com.soundcloud.android.utils.NetworkConnectionHelper;
import com.soundcloud.java.strings.Strings;
import com.soundcloud.rx.eventbus.EventBus;
import org.jetbrains.annotations.Nullable;

import android.content.Context;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Build;
import android.os.Handler;
import android.os.Message;
import android.os.PowerManager;
import android.support.annotation.VisibleForTesting;
import android.util.Log;

import javax.inject.Inject;
import java.io.IOException;
import java.lang.ref.WeakReference;

public class MediaPlayerAdapter implements Player, MediaPlayer.OnPreparedListener, MediaPlayer.OnErrorListener,
        MediaPlayer.OnSeekCompleteListener, MediaPlayer.OnInfoListener, MediaPlayer.OnBufferingUpdateListener {

    private static final String TAG = "MediaPlayerAdapter";
    private static final int POS_NOT_SET = Consts.NOT_SET;

    public static final int MAX_CONNECT_RETRIES = 2;
    public static final int SEEK_COMPLETE_PROGRESS_DELAY = 3000;

    private final Context context;
    private final MediaPlayerManager mediaPlayerManager;
    private final PlayerHandler playerHandler;
    private final EventBus eventBus;
    private final NetworkConnectionHelper networkConnectionHelper;
    private final AccountOperations accountOperations;
    private final BufferUnderrunListener bufferUnderrunListener;
    private final StreamUrlBuilder urlBuilder;
    private final CurrentDateProvider dateProvider;

    private PlaybackState internalState = PlaybackState.STOPPED;

    private Urn track = Urn.NOT_SET;
    private int connectionRetries = 0;

    private boolean waitingForSeek;
    private long seekPos = POS_NOT_SET;
    private long resumePos = POS_NOT_SET;

    @Nullable
    private volatile MediaPlayer mediaPlayer;
    private double loadPercent;
    @Nullable
    private PlayerListener playerListener;

    private long prepareStartTimeMs;
    private String currentStreamUrl = Strings.EMPTY;

    @Inject
    public MediaPlayerAdapter(Context context, MediaPlayerManager mediaPlayerManager,
                              PlayerHandler playerHandler, EventBus eventBus,
                              NetworkConnectionHelper networkConnectionHelper,
                              AccountOperations accountOperations,
                              BufferUnderrunListener bufferUnderrunListener,
                              StreamUrlBuilder urlBuilder,
                              CurrentDateProvider dateProvider) {
        this.bufferUnderrunListener = bufferUnderrunListener;
        this.urlBuilder = urlBuilder;
        this.dateProvider = dateProvider;
        this.context = context.getApplicationContext();
        this.mediaPlayerManager = mediaPlayerManager;
        this.playerHandler = playerHandler;
        this.eventBus = eventBus;
        this.playerHandler.setMediaPlayerAdapter(this);
        this.networkConnectionHelper = networkConnectionHelper;
        this.accountOperations = accountOperations;
    }

    @Override
    public void play(Urn track) {
        play(track, 0);
    }

    @Override
    public void play(Urn track, long fromPos) {
        if (mediaPlayer == null || releaseUnresettableMediaPlayer()) {
            createMediaPlayer();
        } else {
            // do we need to stop it if it's playing?
            mediaPlayer.reset();
        }

        this.track = track;
        waitingForSeek = false;
        resumePos = fromPos;
        seekPos = POS_NOT_SET;

        setInternalState(PlaybackState.PREPARING);
        prepareStartTimeMs = dateProvider.getCurrentDate().getTime();

        try {
            currentStreamUrl = urlBuilder.buildHttpsStreamUrl(track);
            mediaPlayer.setDataSource(currentStreamUrl);
            mediaPlayer.prepareAsync();
        } catch (IOException e) {
            handleMediaPlayerError(mediaPlayer, resumePos);
        }
    }

    @Override
    public void playUninterrupted(Urn track) {
        // Not implemented for MediaPlayer
        play(track);
    }

    @Override
    public void playOffline(Urn track, long fromPos) {
        throw new IllegalStateException("MediaPlayer cannot play offline content!!");
    }

    @Override
    public void playVideo(VideoPlaybackItem videoPlaybackItem) {
        throw new IllegalStateException("MediaPlayer cannot play video!");
    }

    @Override
    public void onPrepared(MediaPlayer mediaPlayer) {
        if (!accountOperations.isUserLoggedIn()) {
            return;
        }
        if (mediaPlayer.equals(this.mediaPlayer) && internalState == PlaybackState.PREPARING) {

            if (playerListener != null && playerListener.requestAudioFocus()) {
                play();
                publishTimeToPlayEvent(dateProvider.getCurrentDate().getTime() - prepareStartTimeMs, currentStreamUrl);

                if (resumePos > 0) {
                    seek(resumePos, true);
                }
            } else {
                setInternalState(PlaybackState.PAUSED);
                Log.e(TAG, "Could not acquire audio focus");
            }
            resumePos = POS_NOT_SET;

        } else {
            // when could this possibly happen??
            Log.e(TAG, "OnPrepared called unexpectedly in state " + internalState);
        }
    }

    void resetConnectionRetries() {
        connectionRetries = 0;
    }

    private void publishTimeToPlayEvent(long timeToPlay, String streamUrl) {
        final PlaybackPerformanceEvent event = PlaybackPerformanceEvent.timeToPlay(timeToPlay,
                getPlaybackProtocol(), PlayerType.MEDIA_PLAYER, networkConnectionHelper.getCurrentConnectionType(),
                streamUrl, accountOperations.getLoggedInUserUrn());
        eventBus.publish(EventQueue.PLAYBACK_PERFORMANCE, event);
    }

    private PlaybackProtocol getPlaybackProtocol() {
        return PlaybackProtocol.HTTPS;
    }

    private void play() {
        if (mediaPlayer != null) {
            mediaPlayer.start();
            setInternalState(PlaybackState.PLAYING);
        }
    }

    @Override
    public boolean onError(MediaPlayer mp, int what, int extra) {
        return handleMediaPlayerError(mp, getAdjustedProgress());
    }

    private boolean handleMediaPlayerError(MediaPlayer mp, long resumePosition) {
        //noinspection ObjectEquality
        if (mp.equals(mediaPlayer) && internalState != PlaybackState.STOPPED) {

            if (connectionRetries++ < MAX_CONNECT_RETRIES) {
                Log.d(TAG, "stream disconnected, retrying (try=" + connectionRetries + ")");
                setInternalState(PlaybackState.ERROR_RETRYING);
                play(track, resumePosition);
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

    @Override
    public void onSeekComplete(MediaPlayer mp) {
        if (Log.isLoggable(TAG, Log.DEBUG)) {
            Log.d(TAG, "onSeekComplete(state=" + internalState + ")");
        }
        //noinspection ObjectEquality
        if (mediaPlayer == mp) {

            // only clear seek if we are not buffering. If we are buffering, it will be cleared after buffering completes
            if (internalState != PlaybackState.PAUSED_FOR_BUFFERING) {
                // keep the last seek time for 3000 ms because getCurrentPosition will be incorrect at first. this way if seeking fails,
                // we can resume from the proper position
                playerHandler.removeMessages(PlayerHandler.CLEAR_LAST_SEEK);
                playerHandler.sendEmptyMessageDelayed(PlayerHandler.CLEAR_LAST_SEEK, SEEK_COMPLETE_PROGRESS_DELAY);

            } else {
                Log.d(TAG, "Not clearing seek, waiting for buffer");
            }

            waitingForSeek = false;

            // respect pauses during seeks
            if (internalState == PlaybackState.PAUSED) {
                pause();
            } else if (internalState.isSupposedToBePlaying()) {
                if (Build.VERSION.SDK_INT > Build.VERSION_CODES.JELLY_BEAN) {
                    // KitKat sucks, and doesn't resume playback after seeking sometimes, with no discernible
                    // output. Toggling playback seems to fix it
                    mp.pause();
                    mp.start();
                }
                setInternalState(PlaybackState.PLAYING);
            }
        }
    }

    public long getSeekPosition() {
        return seekPos;
    }

    @Override
    public boolean onInfo(MediaPlayer mp, int what, int extra) {
        if (Log.isLoggable(TAG, Log.DEBUG)) {
            Log.d(TAG, "onInfo(" + what + "," + extra + ", state=" + internalState + ")");
        }

        if (internalState == PlaybackState.PREPARING) {
            return true; // swallow info callbacks if preparing. HTC Bug
        }

        if (MediaPlayer.MEDIA_INFO_BUFFERING_START == what) {
            setInternalState(PlaybackState.PAUSED_FOR_BUFFERING);
            playerHandler.removeMessages(PlayerHandler.CLEAR_LAST_SEEK);
            return true;

        } else if (MediaPlayer.MEDIA_INFO_BUFFERING_END == what) {
            if (seekPos != -1 && !waitingForSeek) {
                playerHandler.removeMessages(PlayerHandler.CLEAR_LAST_SEEK);
                playerHandler.sendEmptyMessageDelayed(PlayerHandler.CLEAR_LAST_SEEK, 3000);
            } else if (Log.isLoggable(TAG, Log.DEBUG)) {
                Log.d(TAG, "Not clearing seek, waiting for seek to finish");
            }

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

    @Override
    public void onBufferingUpdate(MediaPlayer mp, int percent) {
        if (mediaPlayer == mp) {
            if (Log.isLoggable(TAG, Log.DEBUG) && loadPercent != percent) {
                Log.d(TAG, "onBufferingUpdate(" + percent + ")");
            }

            loadPercent = percent;
        }
    }

    void onTrackEnded() {
        resetConnectionRetries();
        setInternalState(PlaybackState.COMPLETED);
    }

    void setResumeTimeAndInvokeErrorListener(MediaPlayer mediaPlayer, long lastPosition) {
        if (mediaPlayer == this.mediaPlayer) {
            handleMediaPlayerError(mediaPlayer, lastPosition);
        }
    }

    public boolean hasValidSeekPosition() {
        return seekPos != POS_NOT_SET;
    }

    public boolean isTryingToResumeTrack() {
        return resumePos != POS_NOT_SET;
    }

    public long getResumeTime() {
        return resumePos;
    }

    void stop(MediaPlayer mediaPlayer) {
        if (mediaPlayer == this.mediaPlayer) {
            stop();
        }
    }

    private boolean releaseUnresettableMediaPlayer() {
        if (waitingForSeek || internalState.isLoading()) {
            mediaPlayerManager.stopAndReleaseAsync(mediaPlayer);
            mediaPlayer = null;
            return true;
        }
        return false;
    }

    private void createMediaPlayer() {
        mediaPlayer = mediaPlayerManager.create();
        mediaPlayer.setWakeMode(context, PowerManager.PARTIAL_WAKE_LOCK);
        mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
        mediaPlayer.setOnErrorListener(this);
        mediaPlayer.setOnPreparedListener(this);
        mediaPlayer.setOnSeekCompleteListener(this);
        mediaPlayer.setOnInfoListener(this);
        mediaPlayer.setOnCompletionListener(new TrackCompletionListener(this));
    }

    private void setInternalState(PlaybackState playbackState) {
        internalState = playbackState;
        reportStateChange(playbackState, getAdjustedProgress(), getDuration());
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

        if (playerListener != null) {
            final StateTransition stateTransition = new StateTransition(getTranslatedState(), getTranslatedReason(), track, progress, duration, dateProvider);
            stateTransition.addExtraAttribute(StateTransition.EXTRA_PLAYBACK_PROTOCOL, getPlaybackProtocol().getValue());
            stateTransition.addExtraAttribute(StateTransition.EXTRA_PLAYER_TYPE, PlayerType.MEDIA_PLAYER.getValue());
            stateTransition.addExtraAttribute(StateTransition.EXTRA_NETWORK_AND_WAKE_LOCKS_ACTIVE, "false");
            stateTransition.addExtraAttribute(StateTransition.EXTRA_CONNECTION_TYPE, networkConnectionHelper.getCurrentConnectionType().getValue());
            playerListener.onPlaystateChanged(stateTransition);
            bufferUnderrunListener.onPlaystateChanged(stateTransition,
                    getPlaybackProtocol(),
                    PlayerType.MEDIA_PLAYER,
                    networkConnectionHelper.getCurrentConnectionType()
            );
        }
    }

    boolean isInErrorState() {
        return internalState.isError();
    }

    boolean isPlayerPlaying() {
        return internalState == PlaybackState.PLAYING;
    }

    @Override
    public void resume() {
        if (mediaPlayer != null && internalState.isStartable()) {
            play();
        } else {
            play(track, resumePos);
        }
    }

    @Override
    public void pause() {
        if (mediaPlayer != null && internalState.isPausable()) {
            mediaPlayer.pause();
            setInternalState(PlaybackState.PAUSED);
        } else {
            stop();
        }
    }

    @Override
    public void destroy() {
        stop();
        // make sure there aren't any other messages coming
        playerHandler.removeCallbacksAndMessages(null);
    }

    @Override
    public void setListener(PlayerListener playerListener) {
        this.playerListener = playerListener;
    }

    public long seek(long position) {
        return seek(position, true);
    }

    @Override
    public long seek(long position, boolean performSeek) {
        if (isSeekable()) {
            if (position < 0) {
                throw new IllegalArgumentException("Trying to seek before 0");
            }

            final long currentPos = (mediaPlayer != null && !internalState.isError()) ? mediaPlayer.getCurrentPosition() : 0;
            if (mediaPlayer == null) {
                return currentPos;
            } else {
                if (performSeek && position != currentPos) {
                    if (Log.isLoggable(TAG, Log.DEBUG)) {
                        Log.d(TAG, "seeking to " + position);
                    }

                    resetConnectionRetries();
                    playerHandler.removeMessages(PlayerHandler.CLEAR_LAST_SEEK);
                    seekPos = position;
                    waitingForSeek = true;
                    bufferUnderrunListener.onSeek();
                    mediaPlayer.seekTo((int) position);
                }
                return position;
            }
        } else {
            return POS_NOT_SET;
        }
    }

    @Override
    public long getProgress() {
        if (resumePos != POS_NOT_SET) {
            return resumePos;
        } else if (waitingForSeek) {
            return seekPos;
        } else if (mediaPlayer != null && internalState.canGetMPProgress()) {
            return mediaPlayer.getCurrentPosition();
        } else {
            return 0;
        }
    }

    private void sendProgress() {
        if (playerListener != null) {
            long progress = getAdjustedProgress();
            final long duration = getDuration();

            playerListener.onProgressEvent(progress, duration);
        }
    }

    private long getAdjustedProgress() {
        long duration = getDuration();
        long progress = getProgress();

        // Media player reports progress > expectedDuration refs #2035
        if (duration > 0 && progress > duration) {
            Log.d(TAG, "Progress > expectedDuration: " + progress + " > " + duration);
            return duration;
        }
        return progress;
    }

    public long getDuration() {
        if (mediaPlayer != null && internalState.canGetMPProgress()) {
            return mediaPlayer.getDuration();
        } else {
            return POS_NOT_SET;
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

    @Override
    public boolean isSeekable() {
        return mediaPlayer != null && internalState.isSeekable();
    }

    @Override
    public void setVolume(float volume) {
        if (mediaPlayer != null) {
            mediaPlayer.setVolume(volume, volume);
        }
    }

    @Override
    public void stop() {

        final MediaPlayer mediaPlayer = this.mediaPlayer;
        if (mediaPlayer != null) {
            // store times as they will not be accessible after release
            final long progress = getAdjustedProgress();
            final long duration = getDuration();

            if (internalState.isStoppable()) {
                mediaPlayer.stop();
            }

            mediaPlayerManager.stopAndReleaseAsync(mediaPlayer);
            this.mediaPlayer = null;

            setInternalState(PlaybackState.STOPPED, progress, duration);
        }
    }

    @Override
    public void stopForTrackTransition() {
        stop();
    }

    @VisibleForTesting
    static class PlayerHandler extends Handler {

        static final int CLEAR_LAST_SEEK = 0;
        static final int SEND_PROGRESS = 1;

        private WeakReference<MediaPlayerAdapter> mediaPlayerAdapterWeakReference;

        @Inject
        PlayerHandler() {
        }

        @VisibleForTesting
        void setMediaPlayerAdapter(MediaPlayerAdapter adapter) {
            mediaPlayerAdapterWeakReference = new WeakReference<>(adapter);
        }

        @Override
        public void handleMessage(Message msg) {
            final MediaPlayerAdapter mediaPlayerAdapter = mediaPlayerAdapterWeakReference.get();
            if (mediaPlayerAdapter == null) {
                return;
            }

            switch (msg.what) {
                case CLEAR_LAST_SEEK:
                    mediaPlayerAdapter.seekPos = POS_NOT_SET;
                    break;
                case SEND_PROGRESS:
                    mediaPlayerAdapter.sendProgress();
                    sendEmptyMessageDelayed(SEND_PROGRESS, PlaybackConstants.PROGRESS_DELAY_MS);
                    break;

            }
        }
    }
}
