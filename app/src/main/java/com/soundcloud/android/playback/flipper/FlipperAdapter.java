package com.soundcloud.android.playback.flipper;

import com.soundcloud.android.accounts.AccountOperations;
import com.soundcloud.android.crypto.CryptoOperations;
import com.soundcloud.android.crypto.DeviceSecret;
import com.soundcloud.android.events.ConnectionType;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.PlaybackErrorEvent;
import com.soundcloud.android.events.PlayerType;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playback.HlsStreamUrlBuilder;
import com.soundcloud.android.playback.PlayStateReason;
import com.soundcloud.android.playback.PlaybackItem;
import com.soundcloud.android.playback.PlaybackState;
import com.soundcloud.android.playback.PlaybackStateTransition;
import com.soundcloud.android.playback.Player;
import com.soundcloud.android.playback.PreloadItem;
import com.soundcloud.android.playback.common.ProgressChangeHandler;
import com.soundcloud.android.playback.common.StateChangeHandler;
import com.soundcloud.android.utils.ConnectionHelper;
import com.soundcloud.android.utils.CurrentDateProvider;
import com.soundcloud.android.utils.ErrorUtils;
import com.soundcloud.android.utils.LockUtil;
import com.soundcloud.android.utils.Log;
import com.soundcloud.java.strings.Strings;
import com.soundcloud.rx.eventbus.EventBusV2;
import org.jetbrains.annotations.Nullable;

import javax.inject.Inject;

@SuppressWarnings("PMD.AvoidCatchingThrowable")
public class FlipperAdapter implements Player {

    private static final String TAG = "FlipperAdapter";
    private static final String TRUE_STRING = String.valueOf(true);

    private final FlipperWrapper flipperWrapper;
    private final EventBusV2 eventBus;
    private final AccountOperations accountOperations;
    private final HlsStreamUrlBuilder hlsStreamUrlBuilder;
    private final StateChangeHandler stateHandler;
    private final ProgressChangeHandler progressChangeHandler;
    private final CurrentDateProvider dateProvider;
    private final ConnectionHelper connectionHelper;
    private final LockUtil lockUtil;
    private final CryptoOperations cryptoOperations;
    private final PerformanceReporter performanceReporter;

    @Nullable private volatile String currentStreamUrl;
    @Nullable private PlaybackItem currentPlaybackItem;

    // Flipper may send past progress events when seeking, leading to UI glitches.
    // This boolean helps us to workaround this.
    private boolean isSeekPending;
    private long progress;

    @Inject
    FlipperAdapter(FlipperWrapperFactory flipperWrapperFactory,
                   AccountOperations accountOperations,
                   HlsStreamUrlBuilder hlsStreamUrlBuilder,
                   ConnectionHelper connectionHelper,
                   LockUtil lockUtil,
                   StateChangeHandler stateChangeHandler,
                   ProgressChangeHandler progressChangeHandler,
                   CurrentDateProvider dateProvider,
                   EventBusV2 eventBus,
                   CryptoOperations cryptoOperations,
                   PerformanceReporter performanceReporter) {
        this.flipperWrapper = flipperWrapperFactory.create(this);
        this.accountOperations = accountOperations;
        this.hlsStreamUrlBuilder = hlsStreamUrlBuilder;
        this.stateHandler = stateChangeHandler;
        this.progressChangeHandler = progressChangeHandler;
        this.dateProvider = dateProvider;
        this.connectionHelper = connectionHelper;
        this.lockUtil = lockUtil;
        this.eventBus = eventBus;
        this.performanceReporter = performanceReporter;
        this.isSeekPending = false;
        this.cryptoOperations = cryptoOperations;
    }

    @Override
    public void preload(PreloadItem preloadItem) {
        flipperWrapper.prefetch(hlsStreamUrlBuilder.buildStreamUrl(preloadItem));
    }

    @Override
    public void play(PlaybackItem playbackItem) {
        if (!accountOperations.isUserLoggedIn()) {
            throw new IllegalStateException("Cannot play a track if no soundcloud account exists");
        }

        currentPlaybackItem = playbackItem;
        ErrorUtils.log(android.util.Log.DEBUG, TAG, "play(): " + currentPlaybackItem.getUrn() + " in duration " + currentPlaybackItem.getDuration() + " ]");
        stateHandler.removeMessages(0);
        isSeekPending = false;
        progress = 0;

        if (isCurrentStreamUrl(hlsStreamUrlBuilder.buildStreamUrl(playbackItem))) {
            seek(playbackItem.getStartPosition());
            startPlayback();
        } else {
            initializePlayback(playbackItem);
            startPlayback();
        }
    }

    @Override
    public void resume(PlaybackItem playbackItem) {
        currentPlaybackItem = playbackItem;
        Log.d(TAG, "resume() called with: playbackItem = [" + currentPlaybackItem + ", " + currentPlaybackItem.getUrn() + " in duration " + currentPlaybackItem.getDuration() + " ]");
        startPlayback();
    }

    @Override
    public void pause() {
        flipperWrapper.pause();
    }

    @Override
    public void seek(long position) {
        Log.d(TAG, "seek() called with: position = [" + position + "]");
        setSeekingState(position);
        flipperWrapper.seek(position);
    }

    private void setSeekingState(long position) {
        isSeekPending = true;
        progress = position;
    }

    @Override
    public long getProgress() {
        return progress;
    }

    @Override
    public float getVolume() {
        return flipperWrapper.getVolume();
    }

    @Override
    public void setVolume(float level) {
        flipperWrapper.setVolume(level);
    }

    @Override
    public void stop() {
        flipperWrapper.pause();
    }

    @Override
    public void stopForTrackTransition() {
        stop();
    }

    @Override
    public void destroy() {
        flipperWrapper.destroy();
    }

    @Override
    public void setListener(PlayerListener playerListener) {
        if (playerListener == null) {
            throw new IllegalArgumentException("PlayerListener can't be null");
        }

        this.stateHandler.setPlayerListener(playerListener);
        this.progressChangeHandler.setPlayerListener(playerListener);
    }

    @Override
    public boolean isSeekable() {
        return true;
    }

    @Override
    public PlayerType getPlayerType() {
        return PlayerType.FLIPPER;
    }

    void onProgressChanged(ProgressChange event) {
        try {
            if (isCurrentStreamUrl(event.getUri()) && !isSeekPending) {
                reportProgress(event.getPosition(), event.getDuration());
            }
        } catch (Throwable t) {
            ErrorUtils.handleThrowableOnMainThread(t, getClass());
        }
    }

    private void reportProgress(long position, long duration) {
        setProgress(position);
        progressChangeHandler.report(position, duration);
    }

    void onPerformanceEvent(AudioPerformanceEvent event) {
        try {
            performanceReporter.report(currentPlaybackItem, event, getPlayerType());
        } catch (Throwable t) {
            ErrorUtils.handleThrowableOnMainThread(t, getClass());
        }
    }

    private boolean isCurrentStreamUrl(String uri) {
        return uri.equals(currentStreamUrl);
    }

    void onStateChanged(StateChange event) {
        ErrorUtils.log(android.util.Log.INFO, TAG, "onStateChanged() called in " + event.getState().toString() + " with: event = [" + event + "]");
        handleStateChanged(event);
    }

    void onBufferingChanged(StateChange event) {
        Log.i(TAG, "onBufferingChanged() called in " + event.getState().toString() + " with: event = [" + event + "]");
        handleStateChanged(event);
    }

    private void handleStateChanged(StateChange event) {
        try {
            if (currentPlaybackItem == null) {
                ErrorUtils.handleSilentException(new IllegalStateException("State reported with no playback item "));
            } else if (isCurrentStreamUrl(event.getUri())) {
                setProgress(event.getPosition());
                reportStateTransition(event, FlipperExtensionsKt.playbackState(event), FlipperExtensionsKt.playStateReason(event), currentPlaybackItem.getUrn());
            }
        } catch (Throwable t) {
            ErrorUtils.handleThrowableOnMainThread(t, getClass());
        }
    }

    void onSeekingStatusChanged(SeekingStatusChange seekingStatusChange) {
        try {
            if (isCurrentStreamUrl(seekingStatusChange.getUri())) {
                isSeekPending = seekingStatusChange.getSeekInProgress();
            }
        } catch (Throwable t) {
            ErrorUtils.handleThrowableOnMainThread(t, getClass());
        }
    }

    public void onError(FlipperError error) {
        try {
            ConnectionType currentConnectionType = connectionHelper.getCurrentConnectionType();
            // TODO : remove this check, as Skippy should filter out timeouts. Leaving it for this release as a precaution - JS
            if (!ConnectionType.OFFLINE.equals(currentConnectionType)) {
                // Use Log as Skippy dumps can be rather large
                ErrorUtils.handleSilentExceptionWithLog(new FlipperException(error.getCategory(), error.getLine(), error.getSourceFile()), error.getMessage());
            }

            final PlaybackErrorEvent event = new PlaybackErrorEvent(error.getCategory(),
                                                                    FlipperExtensionsKt.playbackProtocol(error.getStreamingProtocol()),
                                                                    error.getCdn(),
                                                                    error.getFormat(),
                                                                    error.getBitrate(),
                                                                    currentConnectionType,
                                                                    getPlayerType());
            eventBus.publish(EventQueue.PLAYBACK_ERROR, event);
        } catch (Throwable t) {
            ErrorUtils.handleThrowableOnMainThread(t, getClass());
        }
    }

    private void setProgress(long position) {
        if (!isSeekPending) {
            progress = position;
        }
    }

    private void reportStateTransition(StateChange event,
                                       PlaybackState translatedState,
                                       PlayStateReason translatedReason,
                                       Urn urn) {
        final PlaybackStateTransition transition = new PlaybackStateTransition(translatedState,
                                                                               translatedReason,
                                                                               urn,
                                                                               progress,
                                                                               event.getDuration(),
                                                                               dateProvider);
        final String connectionType = connectionHelper.getCurrentConnectionType().getValue();

        transition.addExtraAttribute(PlaybackStateTransition.EXTRA_PLAYBACK_PROTOCOL, FlipperExtensionsKt.playbackProtocol(event.getStreamingProtocol()).getValue())
                  .addExtraAttribute(PlaybackStateTransition.EXTRA_PLAYER_TYPE, getPlayerType().getValue())
                  .addExtraAttribute(PlaybackStateTransition.EXTRA_CONNECTION_TYPE, connectionType)
                  .addExtraAttribute(PlaybackStateTransition.EXTRA_NETWORK_AND_WAKE_LOCKS_ACTIVE, TRUE_STRING)
                  .addExtraAttribute(PlaybackStateTransition.EXTRA_URI, currentStreamUrl);

        stateHandler.report(currentPlaybackItem, transition);
        configureLockBasedOnNewState(transition);

        if (transition.playbackHasStopped()) {
            currentPlaybackItem = null;
            currentStreamUrl = null;
        }
    }

    private void configureLockBasedOnNewState(PlaybackStateTransition transition) {
        if (transition.isPlayerPlaying() || transition.isBuffering()) {
            lockUtil.lock();
        } else {
            lockUtil.unlock();
        }
    }

    private void initializePlayback(PlaybackItem playbackItem) {
        currentStreamUrl = hlsStreamUrlBuilder.buildStreamUrl(playbackItem);

        switch (playbackItem.getPlaybackType()) {
            case AUDIO_DEFAULT:
            case AUDIO_SNIPPET:
                flipperWrapper.open(currentStreamUrl, playbackItem.getStartPosition());
                break;
            case AUDIO_OFFLINE:
                final DeviceSecret deviceSecret = cryptoOperations.checkAndGetDeviceKey();
                flipperWrapper.openEncrypted(currentStreamUrl, deviceSecret.getKey(), deviceSecret.getInitVector(), playbackItem.getStartPosition());
                break;
            case AUDIO_AD:
            case VIDEO_AD:
            default:
                throw new IllegalStateException("Flipper does not accept playback type: " + playbackItem.getPlaybackType());
        }
    }

    private void startPlayback() {
        flipperWrapper.play();
    }

    private static class FlipperException extends Exception {
        private final String errorCategory;
        private final int line;
        private final String sourceFile;

        FlipperException(String category, int line, String sourceFile) {
            this.errorCategory = category;
            this.line = line;
            this.sourceFile = sourceFile;
        }

        @Override
        public String getMessage() {
            return errorCategory;

        }

        @Override
        public StackTraceElement[] getStackTrace() {
            final StackTraceElement element = new StackTraceElement(errorCategory, Strings.EMPTY, sourceFile, line);
            return new StackTraceElement[]{element};
        }
    }
}
