package com.soundcloud.android.playback.service.mediaplayer;

import static com.soundcloud.android.events.PlaybackPerformanceEvent.PlayerType;

import com.google.common.annotations.VisibleForTesting;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.accounts.AccountOperations;
import com.soundcloud.android.api.legacy.model.PublicApiTrack;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.PlaybackPerformanceEvent;
import com.soundcloud.android.playback.PlaybackProtocol;
import com.soundcloud.android.playback.service.Playa;
import com.soundcloud.android.playback.streaming.StreamProxy;
import com.soundcloud.android.rx.eventbus.EventBus;
import com.soundcloud.android.rx.observers.DefaultSubscriber;
import com.soundcloud.android.tracks.TrackUrn;
import com.soundcloud.android.utils.NetworkConnectionHelper;
import com.soundcloud.android.utils.ScTextUtils;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.subscriptions.Subscriptions;

import android.content.Context;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.Message;
import android.os.PowerManager;
import android.util.Log;

import javax.annotation.Nullable;
import javax.inject.Inject;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.Locale;

public class MediaPlayerAdapter implements Playa, MediaPlayer.OnPreparedListener, MediaPlayer.OnErrorListener,
        MediaPlayer.OnSeekCompleteListener, MediaPlayer.OnInfoListener,MediaPlayer.OnBufferingUpdateListener {

    private static final String TAG = "MediaPlayerAdapter";
    private static final int POS_NOT_SET = -1;

    public static final int MAX_CONNECT_RETRIES = 3;
    public static final int SEEK_COMPLETE_PROGRESS_DELAY = 3000;
    private static final String MEDIA_PLAYER_IO_ERROR_MSG_FORMAT = "what(%d) extra(%d)";

    private final StreamProxy proxy;
    private final Context context;
    private final MediaPlayerManager mediaPlayerManager;
    private final PlayerHandler playerHandler;
    private final EventBus eventBus;
    private final NetworkConnectionHelper networkConnectionHelper;
    private final AccountOperations accountOperations;

    private PlaybackState internalState = PlaybackState.STOPPED;

    private PublicApiTrack track;
    private int connectionRetries = 0;

    private boolean waitingForSeek;
    private long seekPos = POS_NOT_SET;
    private long resumePos = POS_NOT_SET;

    @Nullable
    private volatile MediaPlayer mediaPlayer;
    private double loadPercent;
    @Nullable
    private PlayaListener playaListener;
    private Subscription uriSubscription = Subscriptions.empty();

    private long prepareStartTimeMs;

    @Inject
    public MediaPlayerAdapter(Context context, MediaPlayerManager mediaPlayerManager, StreamProxy streamProxy,
                              PlayerHandler playerHandler, EventBus eventBus, NetworkConnectionHelper networkConnectionHelper,
                              AccountOperations accountOperations) {
        this.context = context.getApplicationContext();
        this.mediaPlayerManager = mediaPlayerManager;
        proxy = streamProxy;
        this.playerHandler = playerHandler;
        this.eventBus = eventBus;
        this.playerHandler.setMediaPlayerAdapter(this);
        this.networkConnectionHelper = networkConnectionHelper;
        this.accountOperations = accountOperations;

        // perhaps start this lazily?
        proxy.start();
    }

    @Override
    public void play(PublicApiTrack track) {
        play(track, POS_NOT_SET);
    }

    @Override
    public void play(PublicApiTrack track, long fromPos) {
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

        uriSubscription.unsubscribe();
        uriSubscription = proxy.uriObservable(this.track.getStreamUrlWithAppendedId(), null)
                .subscribeOn(AndroidSchedulers.mainThread())
                .subscribe(new MediaPlayerDataSourceObserver());
    }

    @Override
    public void playUninterrupted(PublicApiTrack track) {
        // Not implemented for MediaPlayer
        play(track);
    }

    private class MediaPlayerDataSourceObserver extends DefaultSubscriber<Uri> {
        @Override
        public void onError(Throwable e) {
            setInternalState(PlaybackState.ERROR);
            Log.e(TAG, "Could not retrieve proxy uri ", e);
        }

        @Override
        public void onNext(Uri uri) {
            if (mediaPlayer != null) {
                try {
                    mediaPlayer.setDataSource(uri.toString());
                    mediaPlayer.prepareAsync();
                    prepareStartTimeMs = System.currentTimeMillis();
                } catch (IOException e){
                    handleMediaPlayerError(mediaPlayer, resumePos);
                }
            }
        }
    }

    @Override
    public void onPrepared(MediaPlayer mediaPlayer) {
        if(!accountOperations.isUserLoggedIn()) {
            return;
        }
        if (mediaPlayer.equals(this.mediaPlayer) && internalState == PlaybackState.PREPARING) {

            if (playaListener != null && playaListener.requestAudioFocus()) {
                play();
                publishTimeToPlayEvent(System.currentTimeMillis() - prepareStartTimeMs, track.getStreamUrl());

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
                PlaybackProtocol.HTTPS, PlayerType.MEDIA_PLAYER, networkConnectionHelper.getCurrentConnectionType(),
                streamUrl, accountOperations.getLoggedInUserUrn());
        eventBus.publish(EventQueue.PLAYBACK_PERFORMANCE, event);
    }

    private void play() {
        if (mediaPlayer != null){
            mediaPlayer.start();
            setInternalState(PlaybackState.PLAYING);
        }
    }

    @Override
    public boolean onError(MediaPlayer mp, int what, int extra) {
        SoundCloudApplication.handleSilentException(
                String.format(Locale.US, MEDIA_PLAYER_IO_ERROR_MSG_FORMAT, what, extra),
                new MediaPlayerIOException(extra));
        return handleMediaPlayerError(mp, getProgress());

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
            if (!internalState.isSupposedToBePlaying()) {
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
        return seekPos;
    }

    @Override
    public boolean onInfo(MediaPlayer mp, int what, int extra) {
        if (Log.isLoggable(TAG, Log.DEBUG)) {
            Log.d(TAG, "onInfo(" + what + "," + extra + ", state=" + internalState + ")");
        }

        if (MediaPlayer.MEDIA_INFO_BUFFERING_START == what){
            setInternalState(PlaybackState.PAUSED_FOR_BUFFERING);
            playerHandler.removeMessages(PlayerHandler.CLEAR_LAST_SEEK);
            return true;

        } else if (MediaPlayer.MEDIA_INFO_BUFFERING_END == what){
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
        if (mediaPlayer == this.mediaPlayer){
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
        if (mediaPlayer == this.mediaPlayer){
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
        setInternalState(playbackState, getProgress(),getDuration());
    }

    private void setInternalState(PlaybackState playbackState, long progress, long duration) {
        internalState = playbackState;

        if (playaListener != null) {
            playaListener.onPlaystateChanged( new StateTransition(getTranslatedState(), getTranslatedReason(), getTrackUrn(), progress, duration));
        }
    }

    private TrackUrn getTrackUrn() {
        return track == null ? TrackUrn.NOT_SET : track.getUrn();
    }

    boolean isInErrorState(){
        return internalState.isError();
    }

    boolean isPlayerPlaying() {
        return internalState == PlaybackState.PLAYING;
    }

    @Override
    public boolean resume() {
        if (mediaPlayer != null && internalState.isStartable()) {
            play();
            return true;
        } else {
            return false;
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

        if (proxy != null && proxy.isRunning()) {
            proxy.stop();
        }
    }

    @Override
    public void setListener(PlayaListener playaListener) {
        this.playaListener = playaListener;
    }

    public long seek(long ms) {
        return seek(ms, true);
    }

    @Override
    public long seek(long ms, boolean performSeek) {
        if (isSeekable()) {
            if (ms < 0) {
                throw new IllegalArgumentException("Trying to seek before 0");
            }

            final long currentPos = (mediaPlayer != null && !internalState.isError()) ? mediaPlayer.getCurrentPosition() : 0;
            // workaround for devices which can't do content-range requests
            if ((isNotSeekablePastBuffer() && isPastBuffer(ms)) || mediaPlayer == null) {
                Log.d(TAG, "MediaPlayer bug: cannot seek past buffer");
                return currentPos;
            } else {
                long duration = mediaPlayer.getDuration();

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

                    playerHandler.removeMessages(PlayerHandler.CLEAR_LAST_SEEK);
                    seekPos = newPos;
                    waitingForSeek = true;
                    mediaPlayer.seekTo((int) newPos);
                }
                return newPos;
            }
        } else {
            return -1;
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

    public long getDuration() {
        if (mediaPlayer != null && internalState.canGetMPProgress()) {
            return mediaPlayer.getDuration();
        } else {
            return 0;
        }
    }

    private PlayaState getTranslatedState() {
        switch (internalState) {
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
                throw new IllegalArgumentException("No translated state for " + internalState);
        }
    }

    private Reason getTranslatedReason() {
        switch (internalState) {
            case ERROR:
                return Reason.ERROR_FAILED;
            case COMPLETED:
                return Reason.TRACK_COMPLETE;
            default:
                return Reason.NONE;
        }
    }

    @Override
    public boolean isSeekable() {
        return mediaPlayer != null && internalState.isSeekable();
    }

    @Override
    public boolean isNotSeekablePastBuffer() {
        return Build.VERSION.SDK_INT <= Build.VERSION_CODES.FROYO && StreamProxy.isOpenCore();
    }

    @Override
    public void setVolume(float volume) {
        if (mediaPlayer != null){
            mediaPlayer.setVolume(volume, volume);
        }
    }

    @Override
    public void stop() {

        final MediaPlayer mediaPlayer = this.mediaPlayer;
        if (mediaPlayer != null) {
            // store times as they will not be accessible after release
            final long progress = getProgress();
            final long duration = getDuration();

            if (internalState.isStoppable()) {
                mediaPlayer.stop();
            }

            releaseUnresettableMediaPlayer();
            setInternalState(PlaybackState.STOPPED, progress, duration);
        }
        uriSubscription.unsubscribe();
    }

    private boolean isPastBuffer(long pos) {
        return mediaPlayer == null || (pos / (double) mediaPlayer.getDuration()) * 100 > loadPercent;
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
                    mediaPlayerAdapter.seekPos = -1;
                    break;

            }
        }


    }

    private static class MediaPlayerIOException extends Exception {

        private final int errorReason;

        private MediaPlayerIOException(int reason) {
            this.errorReason = Math.abs(reason);
        }

        @Override
        public String getMessage() {
            return "MediaPlayerIOErrorReason(" +errorReason+")";

        }

        @Override
        public StackTraceElement[] getStackTrace() {
            return new StackTraceElement[]{new StackTraceElement(MediaPlayerIOException.class.getSimpleName(),
                    ScTextUtils.EMPTY_STRING,
                    "MediaPlayerAdapter.java",
                    errorReason)};
        }
    }
}
