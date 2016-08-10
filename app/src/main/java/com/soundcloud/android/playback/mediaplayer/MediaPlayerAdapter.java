package com.soundcloud.android.playback.mediaplayer;

import static com.soundcloud.android.playback.PlaybackState.BUFFERING;
import static com.soundcloud.android.playback.PlaybackState.IDLE;
import static com.soundcloud.android.playback.PlaybackState.PLAYING;
import static com.soundcloud.android.playback.PlaybackType.VIDEO_AD;

import com.soundcloud.android.Consts;
import com.soundcloud.android.accounts.AccountOperations;
import com.soundcloud.android.ads.AdUtils;
import com.soundcloud.android.ads.VideoSource;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.PlaybackPerformanceEvent;
import com.soundcloud.android.events.PlayerType;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playback.AudioAdPlaybackItem;
import com.soundcloud.android.playback.BufferUnderrunListener;
import com.soundcloud.android.playback.PlayStateReason;
import com.soundcloud.android.playback.PlaybackConstants;
import com.soundcloud.android.playback.PlaybackItem;
import com.soundcloud.android.playback.PlaybackProtocol;
import com.soundcloud.android.playback.PlaybackStateTransition;
import com.soundcloud.android.playback.Player;
import com.soundcloud.android.playback.StreamUrlBuilder;
import com.soundcloud.android.playback.VideoAdPlaybackItem;
import com.soundcloud.android.playback.VideoSourceProvider;
import com.soundcloud.android.playback.VideoSurfaceProvider;
import com.soundcloud.android.skippy.Skippy;
import com.soundcloud.android.utils.CurrentDateProvider;
import com.soundcloud.android.utils.ErrorUtils;
import com.soundcloud.android.utils.NetworkConnectionHelper;
import com.soundcloud.java.optional.Optional;
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
import android.view.Surface;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.io.IOException;
import java.lang.ref.WeakReference;

@Singleton
public class MediaPlayerAdapter implements Player, MediaPlayer.OnPreparedListener, MediaPlayer.OnErrorListener,
        MediaPlayer.OnSeekCompleteListener, MediaPlayer.OnInfoListener, MediaPlayer.OnBufferingUpdateListener,
        VideoSurfaceProvider.Listener {

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
    private final VideoSourceProvider videoSourceProvider;
    private final VideoSurfaceProvider videoSurfaceProvider;
    private final StreamUrlBuilder urlBuilder;
    private final CurrentDateProvider dateProvider;

    private PlaybackState internalState = PlaybackState.STOPPED;

    private PlaybackItem currentItem;
    private int connectionRetries = 0;
    private float volume = 1.0f;

    private boolean waitingForSeek;
    private long seekPos = POS_NOT_SET;
    private long resumePos = POS_NOT_SET;

    private volatile MediaPlayer mediaPlayer;
    @Nullable private PlayerListener playerListener;

    private double loadPercent;
    private long prepareStartTimeMs;
    private String currentStreamUrl = Strings.EMPTY;

    private Optional<VideoSource> currentVideoSource = Optional.absent();

    @Inject
    public MediaPlayerAdapter(Context context, MediaPlayerManager mediaPlayerManager,
                              PlayerHandler playerHandler, EventBus eventBus,
                              NetworkConnectionHelper networkConnectionHelper,
                              AccountOperations accountOperations,
                              BufferUnderrunListener bufferUnderrunListener,
                              VideoSourceProvider videoSourceProvider,
                              VideoSurfaceProvider videoSurfaceProvider,
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
        this.videoSourceProvider = videoSourceProvider;
        this.videoSurfaceProvider = videoSurfaceProvider;
        this.videoSurfaceProvider.setListener(this);
    }

    @Override
    public void play(PlaybackItem playbackItem) {
        switch (playbackItem.getPlaybackType()) {
            case AUDIO_DEFAULT:
            case AUDIO_SNIPPET:
            case VIDEO_AD:
                play(playbackItem, playbackItem.getStartPosition());
                break;
            case AUDIO_AD:
                play(playbackItem, 0);
                break;
            default:
                throw new IllegalStateException("MediaPlayer cannot play this item: " + playbackItem);
        }
    }

    private void play(PlaybackItem playbackItem, long fromPos) {
        if (mediaPlayer == null || releaseUnresettableMediaPlayer()) {
            createMediaPlayer();
        } else {
            // do we need to stop it if it's playing?
            mediaPlayer.reset();
        }

        this.currentItem = playbackItem;
        waitingForSeek = false;
        resumePos = fromPos;
        seekPos = POS_NOT_SET;

        setInternalState(PlaybackState.PREPARING);
        prepareStartTimeMs = dateProvider.getCurrentDate().getTime();

        try {
            setStreamUrl(playbackItem);
            attemptToSetSurface(playbackItem.getUrn());
            mediaPlayer.setDataSource(currentStreamUrl);
            mediaPlayer.prepareAsync();
        } catch (IOException e) {
            if (isPlayingVideo()) { // Temporarily logging to understand #5310 and what errors we're seeing with video playback
                ErrorUtils.handleSilentException(new IllegalStateException("MediaPlayer video playback error: " + e.getMessage()));
            }
            handleMediaPlayerError(mediaPlayer, resumePos);
        }
    }

    private void setStreamUrl(PlaybackItem playbackItem) {
        if (playbackItem.getPlaybackType() == VIDEO_AD) {
            currentVideoSource = Optional.of(videoSourceProvider.selectOptimalSource((VideoAdPlaybackItem) playbackItem));
            currentStreamUrl = currentVideoSource.get().getUrl();
        } else {
            currentVideoSource = Optional.absent();
            currentStreamUrl = getAudioStreamUrl(playbackItem);
        }
    }

    private String getAudioStreamUrl(PlaybackItem playbackItem) {
        switch (playbackItem.getPlaybackType()) {
            case AUDIO_AD:
                return getAudioAdStream((AudioAdPlaybackItem) playbackItem);
            default:
                return urlBuilder.buildHttpsStreamUrl(playbackItem.getUrn());
        }
    }

    private String getAudioAdStream(AudioAdPlaybackItem adPlaybackItem) {
        return adPlaybackItem.isThirdParty()
               ? adPlaybackItem.getThirdPartyStreamUrl()
               : urlBuilder.buildHttpsStreamUrl(adPlaybackItem.getUrn());
    }

    @Override
    public void onPrepared(MediaPlayer mediaPlayer) {
        if (!accountOperations.isUserLoggedIn()) {
            return;
        }
        if (mediaPlayer.equals(this.mediaPlayer) && internalState == PlaybackState.PREPARING) {

            if (playerListener != null) {
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
        if (!isThirdPartyAudio()) {
            final PlaybackPerformanceEvent event = PlaybackPerformanceEvent.timeToPlay(timeToPlay,
                                                                                       getPlaybackProtocol(),
                                                                                       PlayerType.MEDIA_PLAYER,
                                                                                       networkConnectionHelper.getCurrentConnectionType(),
                                                                                       streamUrl,
                                                                                       getCurrentFormat(),
                                                                                       getCurrentBitrate(),
                                                                                       accountOperations.getLoggedInUserUrn(),
                                                                                       isPlayingVideo());
            eventBus.publish(EventQueue.PLAYBACK_PERFORMANCE, event);
        }
    }

    private boolean isThirdPartyAudio() {
        return AdUtils.isThirdPartyAudioAd(currentItem.getUrn());
    }

    private boolean isPlayingVideo() {
        return currentItem.getPlaybackType() == VIDEO_AD;
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
        if (isPlayingVideo()) { // Temporarily logging to understand #5310 and what errors we're seeing with video playback
            ErrorUtils.handleSilentException(new IllegalStateException("MediaPlayer video playback error what:" + what + " extra:" + extra));
        }
        return handleMediaPlayerError(mp, getAdjustedProgress());
    }

    private boolean handleMediaPlayerError(MediaPlayer mp, long resumePosition) {
        //noinspection ObjectEquality
        if (mp.equals(mediaPlayer) && internalState != PlaybackState.STOPPED) {
            if (!isPlayingVideo() && connectionRetries++ < MAX_CONNECT_RETRIES) {
                Log.d(TAG, "stream disconnected, retrying (try=" + connectionRetries + ")");
                setInternalState(PlaybackState.ERROR_RETRYING);
                play(currentItem, resumePosition);
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

    void onPlaybackEnded() {
        setInternalState(PlaybackState.COMPLETED);
        clearSurface();
        resetConnectionRetries();
    }

    void setResumeTimeAndInvokeErrorListener(MediaPlayer mediaPlayer, long lastPosition) {
        if (mediaPlayer == this.mediaPlayer) {
            handleMediaPlayerError(mediaPlayer, lastPosition);
        }
    }

    public boolean hasValidSeekPosition() {
        return seekPos != POS_NOT_SET;
    }

    public boolean isTryingToResumePlayback() {
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
        mediaPlayer.setOnCompletionListener(new PlaybackCompletionListener(this));
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

        if (playerListener != null && currentItem != null) {
            final PlaybackStateTransition stateTransition = new PlaybackStateTransition(
                    getTranslatedState(), getTranslatedReason(),
                    currentItem.getUrn(), progress, duration,
                    getCurrentFormat(), getCurrentBitrate(),
                    dateProvider);
            stateTransition.addExtraAttribute(PlaybackStateTransition.EXTRA_PLAYBACK_PROTOCOL,
                                              getPlaybackProtocol().getValue());
            stateTransition.addExtraAttribute(PlaybackStateTransition.EXTRA_PLAYER_TYPE,
                                              PlayerType.MEDIA_PLAYER.getValue());
            stateTransition.addExtraAttribute(PlaybackStateTransition.EXTRA_NETWORK_AND_WAKE_LOCKS_ACTIVE, "false");
            stateTransition.addExtraAttribute(PlaybackStateTransition.EXTRA_CONNECTION_TYPE,
                                              networkConnectionHelper.getCurrentConnectionType().getValue());
            playerListener.onPlaystateChanged(stateTransition);
            bufferUnderrunListener.onPlaystateChanged(stateTransition,
                                                      getPlaybackProtocol(),
                                                      PlayerType.MEDIA_PLAYER,
                                                      networkConnectionHelper.getCurrentConnectionType()
            );
        }
    }

    String getCurrentFormat() {
        return currentVideoSource.isPresent() ?
               currentVideoSource.get().getType() :
               Skippy.SkippyMediaType.UNKNOWN.name();
    }

    int getCurrentBitrate() {
        return currentVideoSource.isPresent() ? currentVideoSource.get().getBitRateKbps() * 1000 : 0;
    }

    boolean isInErrorState() {
        return internalState.isError();
    }

    boolean isPlayerPlaying() {
        return internalState == PlaybackState.PLAYING;
    }

    @Override
    public void resume(PlaybackItem ignored) {
        if (mediaPlayer != null && internalState.isStartable()) {
            play();
        } else {
            play(currentItem, resumePos);
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

            final long currentPos = (mediaPlayer != null && !internalState.isError()) ?
                                    mediaPlayer.getCurrentPosition() :
                                    0;
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

    private com.soundcloud.android.playback.PlaybackState getTranslatedState() {
        switch (internalState) {
            case PREPARING:
            case PAUSED_FOR_BUFFERING:
                return BUFFERING;
            case PLAYING:
                return PLAYING;
            case ERROR:
            case COMPLETED:
            case PAUSED:
            case STOPPED:
            case ERROR_RETRYING:
                return IDLE;
            default:
                throw new IllegalArgumentException("No translated state for " + internalState);
        }
    }

    private PlayStateReason getTranslatedReason() {
        switch (internalState) {
            case ERROR:
                return networkConnectionHelper.isNetworkConnected() ?
                       PlayStateReason.ERROR_NOT_FOUND :
                       PlayStateReason.ERROR_FAILED;
            case COMPLETED:
                return PlayStateReason.PLAYBACK_COMPLETE;
            default:
                return PlayStateReason.NONE;
        }
    }

    @Override
    public boolean isSeekable() {
        return mediaPlayer != null && internalState.isSeekable();
    }

    @Override
    public void setVolume(float volume) {
        if (mediaPlayer != null) {
            this.volume = volume;
            mediaPlayer.setVolume(volume, volume);
        }
    }

    @Override
    public float getVolume() {
        return volume;
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

            clearSurface();
            mediaPlayerManager.stopAndReleaseAsync(mediaPlayer);
            this.mediaPlayer = null;

            setInternalState(PlaybackState.STOPPED, progress, duration);
        }
    }

    @Override
    public void stopForTrackTransition() {
        stop();
    }

    public void attemptToSetSurface(Urn urn) {
        if (isPlayingVideo()) {
            final Surface surface = videoSurfaceProvider.getSurface(urn);
            if (mediaPlayer != null && surface != null) {
                mediaPlayer.setSurface(surface);
            }
        }
    }

    private void clearSurface() {
        if (mediaPlayer != null) {
            mediaPlayer.setSurface(null);
        }
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