package com.soundcloud.android.playback.flipper;

import static com.soundcloud.java.checks.Preconditions.checkNotNull;
import static com.soundcloud.java.checks.Preconditions.checkState;

import com.soundcloud.android.ApplicationModule;
import com.soundcloud.android.accounts.AccountOperations;
import com.soundcloud.android.ads.AudioAdSource;
import com.soundcloud.android.api.ApiEndpoints;
import com.soundcloud.android.api.ApiRequest;
import com.soundcloud.android.api.ApiUrlBuilder;
import com.soundcloud.android.api.oauth.Token;
import com.soundcloud.android.events.ConnectionType;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.PlaybackErrorEvent;
import com.soundcloud.android.events.PlaybackPerformanceEvent;
import com.soundcloud.android.events.PlayerType;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.offline.SecureFileStorage;
import com.soundcloud.android.playback.AudioAdPlaybackItem;
import com.soundcloud.android.playback.PlayStateReason;
import com.soundcloud.android.playback.PlaybackItem;
import com.soundcloud.android.playback.PlaybackProtocol;
import com.soundcloud.android.playback.PlaybackState;
import com.soundcloud.android.playback.PlaybackStateTransition;
import com.soundcloud.android.playback.PlaybackType;
import com.soundcloud.android.playback.Player;
import com.soundcloud.android.playback.PreloadItem;
import com.soundcloud.android.utils.CurrentDateProvider;
import com.soundcloud.android.utils.ErrorUtils;
import com.soundcloud.android.utils.LockUtil;
import com.soundcloud.android.utils.Log;
import com.soundcloud.android.utils.NetworkConnectionHelper;
import com.soundcloud.flippernative.api.ErrorReason;
import com.soundcloud.flippernative.api.PlayerState;
import com.soundcloud.flippernative.api.audio_performance;
import com.soundcloud.flippernative.api.error_message;
import com.soundcloud.flippernative.api.state_change;
import com.soundcloud.java.collections.Iterables;
import com.soundcloud.java.strings.Strings;
import com.soundcloud.rx.eventbus.EventBus;
import org.jetbrains.annotations.Nullable;

import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;

import javax.inject.Inject;
import javax.inject.Named;

public class FlipperAdapter extends com.soundcloud.flippernative.api.PlayerListener implements Player {

    private static final String TAG = "FlipperAdapter";
    private static final String TRUE_STRING = String.valueOf(true);
    private static final String PARAM_CAN_SNIP = "can_snip";

    private final com.soundcloud.flippernative.api.Player flipper;
    private final EventBus eventBus;
    private final AccountOperations accountOperations;
    private final StateChangeHandler stateHandler;
    private final SecureFileStorage secureFileStorage;
    private final ApiUrlBuilder urlBuilder;
    private final CurrentDateProvider dateProvider;
    private final NetworkConnectionHelper connectionHelper;
    private final LockUtil lockUtil;


    @Nullable private volatile String currentStreamUrl;
    @Nullable private PlaybackItem currentPlaybackItem;
    private PlayerListener playerListener;

    // Flipper may send past progress events when seeking, leading to UI glitches.
    // This boolean helps us to workaround this.
    private boolean isSeekPending;
    private boolean isPlayerSeeking;
    private long duration;
    private long progress;

    @Inject
    FlipperAdapter(AccountOperations accountOperations,
                   NetworkConnectionHelper connectionHelper,
                   LockUtil lockUtil,
                   StateChangeHandler stateChangeHandler,
                   SecureFileStorage secureFileStorage,
                   ApiUrlBuilder urlBuilder,
                   CurrentDateProvider dateProvider,
                   FlipperFactory flipperFactory,
                   EventBus eventBus) {
        this.accountOperations = accountOperations;
        this.stateHandler = stateChangeHandler;
        this.secureFileStorage = secureFileStorage;
        this.urlBuilder = urlBuilder;
        this.dateProvider = dateProvider;
        this.connectionHelper = connectionHelper;
        this.lockUtil = lockUtil;
        this.eventBus = eventBus;
        this.flipper = flipperFactory.create(this);
        this.playerListener = PlayerListener.EMPTY;
        this.isSeekPending = false;
    }

    @Override
    public void preload(PreloadItem preloadItem) {
        // Preload is not implemented, yet.
    }

    @Override
    public void play(PlaybackItem playbackItem) {
        final long fromPos = playbackItem.getStartPosition();

        if (!accountOperations.isUserLoggedIn()) {
            throw new IllegalStateException("Cannot play a track if no soundcloud account exists");
        }

        stateHandler.removeMessages(0);
        isSeekPending = isPlayerSeeking = false;
        progress = 0;

        final String trackUrl = buildStreamUrl(playbackItem);
        if (trackUrl.equals(currentStreamUrl)) {
            seek(fromPos, true);
            startPlayback(playbackItem);
        } else {
            initializePlayback(playbackItem, fromPos);
            startPlayback(playbackItem);
        }
    }

    @Override
    public void resume(PlaybackItem playbackItem) {
        startPlayback(playbackItem);
    }

    @Override
    public void pause() {
        flipper.pause();
    }

    @Override
    public long seek(long position, boolean performSeek) {
        if (performSeek) {
            Log.d(TAG, "Seeking to position: " + position);
            setSeekingState(position);
            flipper.seek(position);
            reportProgress(position, duration);
        }
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
    public void onProgressChanged(state_change event) {
        if (event.getUri().equals(currentStreamUrl)) {
            isSeekPending = isSeekPending && event.getPosition() != progress;
            if (!isSeekPending) {
                reportProgress(event.getPosition(), event.getDuration());
            }
        }
    }

    private void reportProgress(long position, long duration) {
        setProgress(position);
        playerListener.onProgressEvent(position, duration);
    }

    @Override
    public void onPerformanceEvent(audio_performance event) {
        if (allowPerformanceMeasureEvent()) {
            reportPerformanceEvent(createPlaybackPerformanceEvent(event));
        }
    }

    @Override
    public void onBufferUnderrunChanged(state_change event) {
        if (event.getUri().equals(currentStreamUrl)) {
            final PlayStateReason translatedReason = PlayStateReason.NONE;
            final PlaybackState translatedState = event.getBufferUnderrun() ?
                                                  PlaybackState.BUFFERING :
                                                  PlaybackState.PLAYING;
            reportStateTransition(event, translatedState, translatedReason);
        }
    }

    @Override
    public void onSeekingStatusChanged(state_change event) {
        if (event.getUri().equals(currentStreamUrl)) {
            isPlayerSeeking = event.getSeekingInProgress();
            if (event.getState().equals(PlayerState.Playing)) {
                final PlayStateReason translatedReason = PlayStateReason.NONE;
                final PlaybackState translatedState = translatePlayingState();
                reportStateTransition(event, translatedState, translatedReason);
            }
        }
    }

    // TODO: waiting for a Skipper update go provide getHost and getFormat
    //    private PlaybackPerformanceEvent createPlaybackPerformanceEventFromBufferUnderrun(state_change event) {
    //        final ConnectionType currentConnectionType = connectionHelper.getCurrentConnectionType();
    //        final PlaybackProtocol playbackProtocol = getPlaybackProtocol();
    //
    //        return PlaybackPerformanceEvent.uninterruptedPlaytimeMs(
    //                uninterruptedPlayTime,
    //                playbackProtocol,
    //                PlayerType.FLIPPER,
    //                currentConnectionType,
    //                event.getHost().const_get_value(),
    //                event.getFormat().const_get_value());
    //    }

    private void reportPerformanceEvent(PlaybackPerformanceEvent event) {
        eventBus.publish(EventQueue.PLAYBACK_PERFORMANCE, event);
    }

    @Override
    public void onStateChanged(state_change event) {
        try {
            handleStateChanged(event);
        } catch (Throwable t) {
            ErrorUtils.handleThrowable(t, getClass());
        }
    }

    @Override
    public void onDurationChanged(state_change event) {
        if (event.getUri().equalsIgnoreCase(currentStreamUrl)) {
            duration = event.getDuration();
        }
    }

    @Override
    public void onError(error_message message) {
        ConnectionType currentConnectionType = connectionHelper.getCurrentConnectionType();
        // TODO : remove this check, as Skippy should filter out timeouts. Leaving it for this release as a precaution - JS
        if (!ConnectionType.OFFLINE.equals(currentConnectionType)) {
            // Use Log as Skippy dumps can be rather large
            ErrorUtils.handleSilentExceptionWithLog(new FlipperException(message.getCategory(), message.getLine(), message.getSourceFile()), message.getErrorMessage());
        }

        final PlaybackErrorEvent event = new PlaybackErrorEvent(message.getCategory(), getPlaybackProtocol(),
                                                                message.getCdn(), message.getFormat(), message.getBitRate(), currentConnectionType);
        eventBus.publish(EventQueue.PLAYBACK_ERROR, event);
    }

    private PlaybackPerformanceEvent createPlaybackPerformanceEvent(audio_performance event) {
        ConnectionType currentConnectionType = connectionHelper.getCurrentConnectionType();
        Urn userUrn = accountOperations.getLoggedInUserUrn();
        PlaybackProtocol playbackProtocol = getPlaybackProtocol();


        // TODO : Waiting for a Flipper update.
        //
        // Flipper does not provide strong typing when it comes to eventType.
        // It expects the application to enrich the event (setClientId(), ...) and
        // directly send it to EventGateway using the toJson() method it provides.
        final String eventType = event.getType().const_get_value();
        switch (eventType) {
            case "play":
                return PlaybackPerformanceEvent.timeToPlay(event.getLatency().const_get_value(),
                                                           playbackProtocol,
                                                           PlayerType.FLIPPER,
                                                           currentConnectionType,
                                                           event.getHost().const_get_value(),
                                                           event.getFormat().const_get_value(),
                                                           (int) event.getBitrate().const_get_value(),
                                                           userUrn,
                                                           currentPlaybackItem.getPlaybackType());
            case "seek":
                return PlaybackPerformanceEvent.timeToSeek(event.getLatency().const_get_value(),
                                                           playbackProtocol,
                                                           PlayerType.FLIPPER,
                                                           currentConnectionType,
                                                           event.getHost().const_get_value(),
                                                           event.getFormat().const_get_value(),
                                                           (int) event.getBitrate().const_get_value(),
                                                           userUrn);
            case "cacheUsage":
                return PlaybackPerformanceEvent.cacheUsagePercent(event.getLatency().const_get_value(),
                                                                  playbackProtocol,
                                                                  PlayerType.FLIPPER,
                                                                  currentConnectionType,
                                                                  event.getHost().const_get_value(),
                                                                  event.getFormat().const_get_value(),
                                                                  (int) event.getBitrate().const_get_value());
            case "playlist":
                return PlaybackPerformanceEvent.timeToPlaylist(event.getLatency().const_get_value(),
                                                               playbackProtocol,
                                                               PlayerType.FLIPPER,
                                                               currentConnectionType,
                                                               event.getHost().const_get_value(),
                                                               event.getFormat().const_get_value(),
                                                               (int) event.getBitrate().const_get_value(),
                                                               userUrn);
            default:
                throw new IllegalArgumentException("Unexpected performance metric : " + eventType);
        }
    }

    private PlaybackProtocol getPlaybackProtocol() {
        return PlaybackProtocol.HLS;
    }

    private void handleStateChanged(state_change event) {
        Log.i(TAG, "State = " + event.getState().toString());
        if (event.getUri().equals(currentStreamUrl)) {
            setProgress(event.getPosition());

            final PlaybackState translatedState = playbackState(event.getState());
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

        transition.addExtraAttribute(PlaybackStateTransition.EXTRA_PLAYBACK_PROTOCOL, getPlaybackProtocol().getValue())
                  .addExtraAttribute(PlaybackStateTransition.EXTRA_PLAYER_TYPE, PlayerType.FLIPPER.getValue())
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

    private PlaybackState playbackState(PlayerState flipperState) {
        // TODO : waiting for a Flipper update.
        // Use a switch once PlayerState is converted to an enum.
        if (PlayerState.Idle.equals(flipperState)) {
            return PlaybackState.IDLE;
        } else if (PlayerState.Preparing.equals(flipperState)) {
            return PlaybackState.BUFFERING;
        } else if (PlayerState.Prepared.equals(flipperState)) {
            return PlaybackState.BUFFERING;
        } else if (PlayerState.Playing.equals(flipperState)) {
            return translatePlayingState();
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

    private PlaybackState translatePlayingState() {
        if (!isPlayerSeeking) {
            return PlaybackState.PLAYING;
        }
        return PlaybackState.BUFFERING;
    }

    private String buildStreamUrl(PlaybackItem playbackItem) {
        checkState(accountOperations.isUserLoggedIn(), "SoundCloud User account does not exist");

        final PlaybackType playType = playbackItem.getPlaybackType();
        final Urn urn = playbackItem.getUrn();
        switch (playbackItem.getPlaybackType()) {
            case AUDIO_OFFLINE:
                return secureFileStorage.getFileUriForOfflineTrack(urn).toString();
            case AUDIO_AD:
                return buildAudioAdUrl((AudioAdPlaybackItem) playbackItem);
            default:
                return buildRemoteUrl(urn, playType);
        }
    }

    private String buildAudioAdUrl(AudioAdPlaybackItem adPlaybackItem) {
        final AudioAdSource source = Iterables.find(adPlaybackItem.getSources(), AudioAdSource::isHls);
        return source.requiresAuth() ? buildAdHlsUrlWithAuth(source) : source.getUrl();
    }

    private String buildRemoteUrl(Urn trackUrn, PlaybackType playType) {
        if (playType == PlaybackType.AUDIO_SNIPPET) {
            return getApiUrlBuilder(trackUrn, ApiEndpoints.HLS_SNIPPET_STREAM).build();
        } else {
            return getApiUrlBuilder(trackUrn, ApiEndpoints.HLS_STREAM).withQueryParam(PARAM_CAN_SNIP, false).build();
        }
    }

    private String buildAdHlsUrlWithAuth(AudioAdSource source) {
        Uri.Builder builder = Uri.parse(source.getUrl()).buildUpon();

        Token token = accountOperations.getSoundCloudToken();
        if (token.valid()) {
            builder.appendQueryParameter(ApiRequest.Param.OAUTH_TOKEN.toString(), token.getAccessToken());
        }

        return builder.build().toString();
    }

    private ApiUrlBuilder getApiUrlBuilder(Urn trackUrn, ApiEndpoints endpoint) {
        Token token = accountOperations.getSoundCloudToken();
        ApiUrlBuilder builder = urlBuilder.from(endpoint, trackUrn);
        if (token.valid()) {
            builder.withQueryParam(ApiRequest.Param.OAUTH_TOKEN, token.getAccessToken());
        }
        return builder;
    }

    private void configureLockBasedOnNewState(PlaybackStateTransition transition) {
        if (transition.isPlayerPlaying() || transition.isBuffering()) {
            lockUtil.lock();
        } else {
            lockUtil.unlock();
        }
    }

    private void initializePlayback(PlaybackItem playbackItem, long fromPos) {
        currentStreamUrl = buildStreamUrl(playbackItem);

        switch (playbackItem.getPlaybackType()) {
            case AUDIO_DEFAULT:
            case AUDIO_SNIPPET:
            case AUDIO_AD:
                currentStreamUrl = currentStreamUrl + "&format=hls_opus_64_url&format=hls_mp3_128_url";
                flipper.open(currentStreamUrl, fromPos);
                break;
            case AUDIO_OFFLINE:
            case VIDEO_AD:
            default:
                throw new IllegalStateException("Flipper does not accept playback type: " + playbackItem.getPlaybackType());
        }
    }

    private void startPlayback(PlaybackItem playbackItem) {
        currentPlaybackItem = playbackItem;
        flipper.play();
    }

    private boolean allowPerformanceMeasureEvent() {
        return !isCurrentItemAd();
    }


    private boolean isCurrentItemAd() {
        return currentPlaybackItem != null && currentPlaybackItem.getPlaybackType() == PlaybackType.AUDIO_AD;
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

    static class StateChangeMessage {
        final PlaybackItem playbackItem;
        final PlaybackStateTransition stateTransition;

        StateChangeMessage(PlaybackItem item, PlaybackStateTransition transition) {
            this.playbackItem = item;
            this.stateTransition = transition;
        }
    }

    static class FlipperException extends Exception {
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
