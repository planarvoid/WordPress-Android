package com.soundcloud.android.playback.flipper;

import static com.soundcloud.java.checks.Preconditions.checkNotNull;

import com.soundcloud.android.ApplicationModule;
import com.soundcloud.android.accounts.AccountOperations;
import com.soundcloud.android.crypto.CryptoOperations;
import com.soundcloud.android.crypto.DeviceSecret;
import com.soundcloud.android.events.ConnectionType;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.PlaybackErrorEvent;
import com.soundcloud.android.events.PlayerType;
import com.soundcloud.android.playback.HlsStreamUrlBuilder;
import com.soundcloud.android.playback.PlayStateReason;
import com.soundcloud.android.playback.PlaybackItem;
import com.soundcloud.android.playback.PlaybackProtocol;
import com.soundcloud.android.playback.PlaybackState;
import com.soundcloud.android.playback.PlaybackStateTransition;
import com.soundcloud.android.playback.PlaybackType;
import com.soundcloud.android.playback.Player;
import com.soundcloud.android.playback.PreloadItem;
import com.soundcloud.android.utils.ConnectionHelper;
import com.soundcloud.android.utils.CurrentDateProvider;
import com.soundcloud.android.utils.ErrorUtils;
import com.soundcloud.android.utils.LockUtil;
import com.soundcloud.android.utils.Log;
import com.soundcloud.flippernative.api.ErrorReason;
import com.soundcloud.flippernative.api.PlayerState;
import com.soundcloud.flippernative.api.audio_performance;
import com.soundcloud.flippernative.api.error_message;
import com.soundcloud.flippernative.api.state_change;
import com.soundcloud.java.strings.Strings;
import com.soundcloud.rx.eventbus.EventBus;
import org.jetbrains.annotations.Nullable;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;

import javax.inject.Inject;
import javax.inject.Named;

@SuppressWarnings("PMD.AvoidCatchingThrowable")
public class FlipperAdapter extends com.soundcloud.flippernative.api.PlayerListener implements Player {

    private static final String TAG = "FlipperAdapter";
    private static final String TRUE_STRING = String.valueOf(true);

    private final com.soundcloud.flippernative.api.Player flipper;
    private final Context context;
    private final EventBus eventBus;
    private final AccountOperations accountOperations;
    private final HlsStreamUrlBuilder hlsStreamUrlBuilder;
    private final StateChangeHandler stateHandler;
    private final CurrentDateProvider dateProvider;
    private final ConnectionHelper connectionHelper;
    private final LockUtil lockUtil;
    private final CryptoOperations cryptoOperations;
    private final PerformanceReporter performanceReporter;

    @Nullable private volatile String currentStreamUrl;
    @Nullable private PlaybackItem currentPlaybackItem;
    private PlayerListener playerListener;

    // Flipper may send past progress events when seeking, leading to UI glitches.
    // This boolean helps us to workaround this.
    private boolean isSeekPending;
    private long duration;
    private long progress;

    @Inject
    FlipperAdapter(AccountOperations accountOperations,
                   HlsStreamUrlBuilder hlsStreamUrlBuilder,
                   ConnectionHelper connectionHelper,
                   LockUtil lockUtil,
                   StateChangeHandler stateChangeHandler,
                   CurrentDateProvider dateProvider,
                   FlipperFactory flipperFactory,
                   EventBus eventBus,
                   CryptoOperations cryptoOperations,
                   Context context,
                   PerformanceReporter performanceReporter) {
        this.accountOperations = accountOperations;
        this.hlsStreamUrlBuilder = hlsStreamUrlBuilder;
        this.stateHandler = stateChangeHandler;
        this.dateProvider = dateProvider;
        this.connectionHelper = connectionHelper;
        this.lockUtil = lockUtil;
        this.eventBus = eventBus;
        this.flipper = flipperFactory.create(this);
        this.context = context;
        this.performanceReporter = performanceReporter;
        this.playerListener = PlayerListener.EMPTY;
        this.isSeekPending = false;
        this.cryptoOperations = cryptoOperations;
    }

    @Override
    public void preload(PreloadItem preloadItem) {
        flipper.prefetch(hlsStreamUrlBuilder.buildStreamUrl(preloadItem));
    }

    @Override
    public void play(PlaybackItem playbackItem) {
        if (!accountOperations.isUserLoggedIn()) {
            throw new IllegalStateException("Cannot play a track if no soundcloud account exists");
        }

        currentPlaybackItem = playbackItem;
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
        startPlayback();
    }

    @Override
    public void pause() {
        flipper.pause();
    }

    @Override
    public long seek(long position) {
        Log.d(TAG, "Seeking to position: " + position);
        setSeekingState(position);
        flipper.seek(position);
        reportProgress(position, duration);
        return position;
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
        return (float) flipper.getVolume();
    }

    @Override
    public void setVolume(float v) {
        flipper.setVolume(v);
    }

    @Override
    public void stop() {
        flipper.pause();
    }

    @Override
    public void stopForTrackTransition() {
        stop();
    }

    @Override
    public void destroy() {
        flipper.destroy();
    }

    @Override
    public void setListener(PlayerListener playerListener) {
        final PlayerListener safeListener = checkNotNull(playerListener, "PlayerListener can't be null");
        this.playerListener = safeListener;
        this.stateHandler.setPlayerListener(safeListener);
    }

    @Override
    public boolean isSeekable() {
        return true;
    }

    @Override
    public PlayerType getPlayerType() {
        return PlayerType.FLIPPER;
    }

    @Override
    public void onProgressChanged(state_change event) {
        try {
            if (isCurrentStreamUrl(event.getUri())) {
                final long position = event.getPosition();

                isSeekPending = isSeekPending && position != progress;
                if (!isSeekPending) {
                    reportProgress(position, event.getDuration());
                }
            }
        } catch (Throwable t) {
            ErrorUtils.handleThrowableOnMainThread(t, getClass(), context);
        }
    }

    private void reportProgress(long position, long duration) {
        setProgress(position);
        playerListener.onProgressEvent(position, duration);
    }

    @Override
    public void onPerformanceEvent(audio_performance event) {
        try {
            performanceReporter.report(currentPlaybackItem, event, getPlayerType());
        } catch (Throwable t) {
            ErrorUtils.handleThrowableOnMainThread(t, getClass(), context);
        }
    }

    @Override
    public void onBufferingChanged(state_change event) {
        try {
            if (isCurrentStreamUrl(event.getUri())) {
                final PlayerState currentState = event.getState();
                if (currentState == PlayerState.Playing) {
                    final PlayStateReason translatedReason = PlayStateReason.NONE;
                    final PlaybackState translatedState = event.getBuffering() ?
                                                          PlaybackState.BUFFERING :
                                                          PlaybackState.PLAYING;
                    reportStateTransition(event, translatedState, translatedReason);
                }
            }
        } catch (Throwable t) {
            ErrorUtils.handleThrowableOnMainThread(t, getClass(), context);
        }
    }

    private boolean isCurrentStreamUrl(String uri) {
        return uri.equals(currentStreamUrl);
    }

    @Override
    public void onStateChanged(state_change event) {
        try {
            handleStateChanged(event);
        } catch (Throwable t) {
            ErrorUtils.handleThrowableOnMainThread(t, getClass(), context);
        }
    }

    @Override
    public void onDurationChanged(state_change event) {
        try {
            if (isCurrentStreamUrl(event.getUri())) {
                duration = event.getDuration();
            }
        } catch (Throwable t) {
            ErrorUtils.handleThrowableOnMainThread(t, getClass(), context);
        }
    }

    @Override
    public void onSeekingStatusChanged(state_change evt) {
        // FIXME DO NOT CALL SUPER AS IT WILL CRASH THE APP WHILE SEEKING
        // FIXME Check JIRA: PLAYBACK-2706
    }

    @Override
    public void onError(error_message message) {
        try {
            ConnectionType currentConnectionType = connectionHelper.getCurrentConnectionType();
            // TODO : remove this check, as Skippy should filter out timeouts. Leaving it for this release as a precaution - JS
            if (!ConnectionType.OFFLINE.equals(currentConnectionType)) {
                // Use Log as Skippy dumps can be rather large
                ErrorUtils.handleSilentExceptionWithLog(new FlipperException(message.getCategory(), message.getLine(), message.getSourceFile()), message.getErrorMessage());
            }

            final PlaybackErrorEvent event = new PlaybackErrorEvent(message.getCategory(), FlipperPlaybackProtocolMapper.map(message.getStreamingProtocol()), message.getCdn(),
                    message.getFormat(), message.getBitRate(), currentConnectionType,
                    getPlayerType());
            eventBus.publish(EventQueue.PLAYBACK_ERROR, event);
        } catch (Throwable t) {
            ErrorUtils.handleThrowableOnMainThread(t, getClass(), context);
        }
    }

    private void handleStateChanged(state_change event) {
        Log.i(TAG, "State = " + event.getState().toString());
        if (isCurrentStreamUrl(event.getUri())) {
            setProgress(event.getPosition());

            final PlaybackState translatedState = playbackState(event);
            final PlayStateReason translatedReason = playStateReason(event);
            reportStateTransition(event, translatedState, translatedReason);
        }
    }

    private void setProgress(long position) {
        if (!isSeekPending) {
            progress = position;
        }
    }

    private void reportStateTransition(state_change event,
                                       PlaybackState translatedState,
                                       PlayStateReason translatedReason) {
        final PlaybackStateTransition transition = new PlaybackStateTransition(translatedState,
                                                                               translatedReason,
                                                                               currentPlaybackItem.getUrn(),
                                                                               progress,
                                                                               event.getDuration(),
                                                                               dateProvider);
        final String connectionType = connectionHelper.getCurrentConnectionType().getValue();

        transition.addExtraAttribute(PlaybackStateTransition.EXTRA_PLAYBACK_PROTOCOL, FlipperPlaybackProtocolMapper.map(event.getStreamingProtocol()).getValue())
                  .addExtraAttribute(PlaybackStateTransition.EXTRA_PLAYER_TYPE, getPlayerType().getValue())
                  .addExtraAttribute(PlaybackStateTransition.EXTRA_CONNECTION_TYPE, connectionType)
                  .addExtraAttribute(PlaybackStateTransition.EXTRA_NETWORK_AND_WAKE_LOCKS_ACTIVE, TRUE_STRING)
                  .addExtraAttribute(PlaybackStateTransition.EXTRA_URI, currentStreamUrl);

        final FlipperAdapter.StateChangeMessage message = new FlipperAdapter.StateChangeMessage(currentPlaybackItem, transition);
        stateHandler.sendMessage(stateHandler.obtainMessage(0, message));
        configureLockBasedOnNewState(transition);

        if (transition.playbackHasStopped()) {
            currentPlaybackItem = null;
            currentStreamUrl = null;
        }
    }

    private PlaybackState playbackState(state_change stateChange) {
        // TODO : waiting for a Flipper update.
        // Use a switch once PlayerState is converted to an enum.
        PlayerState flipperState = stateChange.getState();
        if (PlayerState.Idle.equals(flipperState)) {
            return PlaybackState.IDLE;
        } else if (PlayerState.Preparing.equals(flipperState)) {
            return PlaybackState.BUFFERING;
        } else if (PlayerState.Prepared.equals(flipperState)) {
            return PlaybackState.BUFFERING;
        } else if (PlayerState.Playing.equals(flipperState)) {
            return translatePlayingState(stateChange);
        } else if (PlayerState.Completed.equals(flipperState)) {
            return PlaybackState.IDLE;
        } else if (PlayerState.Error.equals(flipperState)) {
            return PlaybackState.IDLE;
        } else {
            return PlaybackState.IDLE;
        }
    }

    private PlayStateReason playStateReason(state_change flipperState) {
        if (flipperState.getState().equals(PlayerState.Error)) {
            if (flipperState.getReason().equals(ErrorReason.NotFound)) {
                return PlayStateReason.ERROR_NOT_FOUND;
            } else if (flipperState.getReason().equals(ErrorReason.Forbidden)) {
                return PlayStateReason.ERROR_FORBIDDEN;
            } else {
                return PlayStateReason.ERROR_FAILED;
            }
        } else if (flipperState.getState().equals(PlayerState.Completed)) {
            return PlayStateReason.PLAYBACK_COMPLETE;
        } else {
            return PlayStateReason.NONE;
        }
    }

    private PlaybackState translatePlayingState(state_change stateChange) {
        if (!stateChange.getBuffering()) {
            return PlaybackState.PLAYING;
        }
        return PlaybackState.BUFFERING;
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
                flipper.open(currentStreamUrl, playbackItem.getStartPosition());
                break;
            case AUDIO_OFFLINE:
                final DeviceSecret deviceSecret = cryptoOperations.checkAndGetDeviceKey();
                flipper.openEncrypted(currentStreamUrl, deviceSecret.getKey(), deviceSecret.getInitVector(), playbackItem.getStartPosition());
                break;
            case AUDIO_AD:
            case VIDEO_AD:
            default:
                throw new IllegalStateException("Flipper does not accept playback type: " + playbackItem.getPlaybackType());
        }
    }

    private void startPlayback() {
        flipper.play();
    }

    static class StateChangeHandler extends Handler {

        private PlayerListener playerListener;

        @Inject
        StateChangeHandler(@Named(ApplicationModule.MAIN_LOOPER) Looper looper) {
            super(looper);
        }

        void setPlayerListener(PlayerListener playerListener) {
            this.playerListener = playerListener;
        }

        @Override
        public void handleMessage(Message msg) {
            final StateChangeMessage message = (StateChangeMessage) msg.obj;
            playerListener.onPlaystateChanged(message.stateTransition);
        }

    }

    private static class StateChangeMessage {
        final PlaybackItem playbackItem;
        final PlaybackStateTransition stateTransition;

        StateChangeMessage(PlaybackItem item, PlaybackStateTransition transition) {
            this.playbackItem = item;
            this.stateTransition = transition;
        }
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
