package com.soundcloud.android.playback.skippy;

import static com.soundcloud.android.skippy.Skippy.PlaybackMetric;
import static com.soundcloud.android.skippy.Skippy.Reason.BUFFERING;
import static com.soundcloud.android.skippy.Skippy.Reason.COMPLETE;
import static com.soundcloud.android.skippy.Skippy.Reason.ERROR;
import static com.soundcloud.java.checks.Preconditions.checkState;

import com.soundcloud.android.ApplicationModule;
import com.soundcloud.android.Consts;
import com.soundcloud.android.accounts.AccountOperations;
import com.soundcloud.android.api.ApiEndpoints;
import com.soundcloud.android.api.ApiRequest;
import com.soundcloud.android.api.ApiUrlBuilder;
import com.soundcloud.android.api.oauth.Token;
import com.soundcloud.android.crypto.CryptoOperations;
import com.soundcloud.android.crypto.DeviceSecret;
import com.soundcloud.android.events.ConnectionType;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.PlaybackErrorEvent;
import com.soundcloud.android.events.PlaybackPerformanceEvent;
import com.soundcloud.android.events.PlayerType;
import com.soundcloud.android.events.SkippyInitilizationFailedEvent;
import com.soundcloud.android.events.SkippyInitilizationSucceededEvent;
import com.soundcloud.android.events.SkippyPlayEvent;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.offline.SecureFileStorage;
import com.soundcloud.android.playback.BufferUnderrunListener;
import com.soundcloud.android.playback.PlaybackProtocol;
import com.soundcloud.android.playback.Player;
import com.soundcloud.android.skippy.Skippy;
import com.soundcloud.android.utils.CurrentDateProvider;
import com.soundcloud.android.utils.DebugUtils;
import com.soundcloud.android.utils.ErrorUtils;
import com.soundcloud.android.utils.LockUtil;
import com.soundcloud.android.utils.Log;
import com.soundcloud.android.utils.NetworkConnectionHelper;
import com.soundcloud.android.utils.ScTextUtils;
import com.soundcloud.java.strings.Strings;
import com.soundcloud.rx.eventbus.EventBus;
import org.jetbrains.annotations.Nullable;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.support.annotation.VisibleForTesting;

import javax.inject.Inject;
import javax.inject.Named;

public class SkippyAdapter implements Player, Skippy.PlayListener {

    private static final String TAG = "SkippyAdapter";
    @VisibleForTesting
    static final String SKIPPY_INIT_ERROR_COUNT_KEY = "SkippyAdapter.initErrorCount";
    static final String SKIPPY_INIT_SUCCESS_COUNT_KEY = "SkippyAdapter.initSuccessCount";

    private static final int PLAY_TYPE_DEFAULT = 0;
    private static final int PLAY_TYPE_STREAM_UNINTERRUPTED = 1; // for ads
    private static final int PLAY_TYPE_OFFLINE = 2;

    private static final long POSITION_START = 0L;
    private static final int INIT_ERROR_CUSTOM_LOG_LINE_COUNT = 5000;
    private final SkippyFactory skippyFactory;
    private final LockUtil lockUtil;

    private final EventBus eventBus;
    private final Skippy skippy;
    private final AccountOperations accountOperations;
    private final StateChangeHandler stateHandler;
    private final ApiUrlBuilder urlBuilder;
    private final NetworkConnectionHelper connectionHelper;
    private final BufferUnderrunListener bufferUnderrunListener;
    private final SharedPreferences sharedPreferences;
    private final SecureFileStorage secureFileStorage;
    private final CryptoOperations cryptoOperations;
    private final CurrentDateProvider dateProvider;

    private volatile String currentStreamUrl;
    private Urn currentTrackUrn;
    private PlayerListener playerListener;
    private long lastStateChangeProgress;

    @Inject
    SkippyAdapter(SkippyFactory skippyFactory, AccountOperations accountOperations, ApiUrlBuilder urlBuilder,
                  StateChangeHandler stateChangeHandler, EventBus eventBus, NetworkConnectionHelper connectionHelper,
                  LockUtil lockUtil, BufferUnderrunListener bufferUnderrunListener,
                  SharedPreferences sharedPreferences, SecureFileStorage secureFileStorage, CryptoOperations cryptoOperations,
                  CurrentDateProvider dateProvider) {
        this.skippyFactory = skippyFactory;
        this.lockUtil = lockUtil;
        this.bufferUnderrunListener = bufferUnderrunListener;
        this.sharedPreferences = sharedPreferences;
        this.secureFileStorage = secureFileStorage;
        this.cryptoOperations = cryptoOperations;
        this.skippy = skippyFactory.create(this);
        this.accountOperations = accountOperations;
        this.urlBuilder = urlBuilder;
        this.eventBus = eventBus;
        this.connectionHelper = connectionHelper;
        this.stateHandler = stateChangeHandler;
        this.stateHandler.setBufferUnderrunListener(bufferUnderrunListener);
        this.dateProvider = dateProvider;
    }

    public boolean init(Context context) {
        boolean initSuccess = skippy.init(context, skippyFactory.createConfiguration());
        if (initSuccess) {
            eventBus.publish(EventQueue.TRACKING, new SkippyInitilizationSucceededEvent(
                    getInitializationErrorCount(), getAndIncrementInitilizationSuccesses()));
        }
        return initSuccess;
    }

    @Override
    public void play(Urn track) {
        play(track, POSITION_START);
    }

    @Override
    public void play(Urn track, long fromPos) {
        play(track, fromPos, PLAY_TYPE_DEFAULT);
    }

    @Override
    public void playUninterrupted(Urn track) {
        play(track, POSITION_START, PLAY_TYPE_STREAM_UNINTERRUPTED);
    }

    @Override
    public void playOffline(Urn track, long fromPos) {
        play(track, fromPos, PLAY_TYPE_OFFLINE);
    }

    private void play(Urn track, long fromPos, int playType) {
        currentTrackUrn = track;

        if (!accountOperations.isUserLoggedIn()) {
            throw new IllegalStateException("Cannot play a track if no soundcloud account exists");
        }

        // TODO : move audiofocus requesting into PlaybackService when we kill MediaPlayer
        if (playerListener == null){
            Log.e(TAG, "No Player Listener, unable to request audio focus");
            return;
        }

        if (!playerListener.requestAudioFocus()){
            Log.e(TAG,"Unable to acquire audio focus, aborting playback");
            final StateTransition stateTransition = new StateTransition(PlayerState.IDLE, Reason.ERROR_FAILED, currentTrackUrn, fromPos, Consts.NOT_SET, dateProvider);
            playerListener.onPlaystateChanged(stateTransition);
            bufferUnderrunListener.onPlaystateChanged(stateTransition, getPlaybackProtocol(), PlayerType.SKIPPY, connectionHelper.getCurrentConnectionType());
            return;
        }

        sendSkippyPlayEvent();

        stateHandler.removeMessages(0);
        lastStateChangeProgress = 0;

        final String trackUrl = buildStreamUrl(playType);
        if (trackUrl.equals(currentStreamUrl)) {
            // we are already playing it. seek and resume
            skippy.seek(fromPos);
            skippy.resume();
        } else {
            currentStreamUrl = trackUrl;

            switch (playType){
                case PLAY_TYPE_STREAM_UNINTERRUPTED :
                    skippy.playAd(currentStreamUrl, fromPos);
                    break;

                case PLAY_TYPE_OFFLINE :
                    final DeviceSecret deviceSecret = cryptoOperations.checkAndGetDeviceKey();
                    skippy.playOffline(currentStreamUrl, fromPos, deviceSecret.getKey(), deviceSecret.getInitVector());
                    break;

                default :
                    skippy.play(currentStreamUrl, fromPos);
                    break;
            }

        }
    }

    private void sendSkippyPlayEvent() {
        // we can get rid of this after 100 percent launch. This is to help determind effectiveness of wakelocks
        ConnectionType currentConnectionType = connectionHelper.getCurrentConnectionType();
        eventBus.publish(EventQueue.TRACKING, new SkippyPlayEvent(currentConnectionType, true));
    }

    private String buildStreamUrl(int playType) {
        checkState(accountOperations.isUserLoggedIn(), "SoundCloud User account does not exist");

        if (playType == PLAY_TYPE_OFFLINE){
            return secureFileStorage.getFileUriForOfflineTrack(currentTrackUrn).toString();
        } else {
            Token token = accountOperations.getSoundCloudToken();

            ApiUrlBuilder builder = urlBuilder.from(ApiEndpoints.HLS_STREAM, currentTrackUrn);

            if (token.valid()) {
                builder.withQueryParam(ApiRequest.Param.OAUTH_TOKEN, token.getAccessToken());
            }

            return builder.build();
        }
    }

    @Override
    public void resume() {
        skippy.resume();
    }

    @Override
    public void pause() {
        skippy.pause();
    }

    @Override
    public long seek(long position, boolean performSeek) {
        if (performSeek) {
            bufferUnderrunListener.onSeek();
            skippy.seek(position);

            long duration = skippy.getDuration();
            if (playerListener != null && duration != 0){
                playerListener.onProgressEvent(position, duration);
            }
        }
        return position;
    }

    @Override
    public long getProgress() {
        if (currentStreamUrl != null){
            return skippy.getPosition();
        } else {
            return lastStateChangeProgress;
        }
    }

    @Override
    public void setVolume(float level) {
        skippy.setVolume(level);
    }

    @Override
    public void stop() {
        skippy.pause();
    }

    @Override
    public void stopForTrackTransition() {
        stop();
    }

    @Override
    public void destroy() {
        skippy.destroy();
    }

    @Override
    public void setListener(PlayerListener playerListener) {
        this.playerListener = playerListener;
        this.stateHandler.setPlayerListener(playerListener);
    }

    @Override
    public boolean isSeekable() {
        return true;
    }

    @Override
    public void onStateChanged(Skippy.State state, Skippy.Reason reason, Skippy.Error errorCode, long position, long duration, String uri) {
        try {
            handleStateChanged(state, reason, errorCode, position, duration, uri);
        } catch (Throwable t) {
            ErrorUtils.handleThrowable(t, getClass());
        }
    }

    private void handleStateChanged(Skippy.State state, Skippy.Reason reason, Skippy.Error errorCode, long position, long duration, String uri) {
        final long adjustedPosition = fixPosition(position, duration);

        Log.i(TAG, "State = " + state + " : " + reason + " : " + errorCode);
        if (uri.equals(currentStreamUrl)) {
            lastStateChangeProgress = position;

            final PlayerState translatedState = getTranslatedState(state, reason);
            final Reason translatedReason = getTranslatedReason(reason, errorCode);
            final StateTransition transition = new StateTransition(translatedState, translatedReason, currentTrackUrn, adjustedPosition, duration, dateProvider);
            transition.addExtraAttribute(StateTransition.EXTRA_PLAYBACK_PROTOCOL, getPlaybackProtocol().getValue());
            transition.addExtraAttribute(StateTransition.EXTRA_PLAYER_TYPE, PlayerType.SKIPPY.getValue());
            transition.addExtraAttribute(StateTransition.EXTRA_CONNECTION_TYPE, connectionHelper.getCurrentConnectionType().getValue());
            transition.addExtraAttribute(StateTransition.EXTRA_NETWORK_AND_WAKE_LOCKS_ACTIVE, String.valueOf(true));
            transition.addExtraAttribute(StateTransition.EXTRA_URI, uri);

            if (transition.playbackHasStopped()){
                currentStreamUrl = null;
            }

            Message msg = stateHandler.obtainMessage(0, transition);
            stateHandler.sendMessage(msg);
            configureLockBasedOnNewState(transition);
        }
    }

    private void configureLockBasedOnNewState(StateTransition transition) {
        if (transition.isPlayerPlaying() || transition.isBuffering()){
            lockUtil.lock();
        } else {
            lockUtil.unlock();
        }
    }

    @Override
    public void onPerformanceMeasured(PlaybackMetric metric, long value, String uri, String cdnHost) {
        if (!accountOperations.isUserLoggedIn() || metric.equals(PlaybackMetric.TIME_TO_BUFFER)) {
            return;
        }
        eventBus.publish(EventQueue.PLAYBACK_PERFORMANCE, createPerformanceEvent(metric, value, cdnHost));
    }

    @Override
    public void onDownloadPerformed(long startPosition, long endPosition, int bytesLoaded, int bytesTotal, String uri) {
        //Not implemented yet!
    }

    private PlayerState getTranslatedState(Skippy.State state, Skippy.Reason reason) {
        switch (state) {
            case IDLE:
                return PlayerState.IDLE;
            case PLAYING:
                return reason == BUFFERING ? PlayerState.BUFFERING : PlayerState.PLAYING;
            default:
                throw new IllegalArgumentException("Unexpected skippy state : " + state);
        }
    }

    private Reason getTranslatedReason(Skippy.Reason reason, Skippy.Error lastError) {
        if (reason == ERROR) {
            switch (lastError) {
                case FAILED:
                case TIMEOUT:
                    return Reason.ERROR_FAILED;
                case FORBIDDEN:
                    return Reason.ERROR_FORBIDDEN;
                case MEDIA_NOT_FOUND:
                    return Reason.ERROR_NOT_FOUND;
                default:
                    throw new IllegalArgumentException("Unexpected skippy error code : " + lastError);
            }
        } else if (reason == COMPLETE) {
            return Reason.TRACK_COMPLETE;
        } else {
            return Reason.NONE;
        }
    }

    private PlaybackProtocol getPlaybackProtocol() {
        return PlaybackProtocol.HLS;
    }

    @Nullable
    private PlaybackPerformanceEvent createPerformanceEvent(PlaybackMetric metric, long value, String cdnHost) {
        ConnectionType currentConnectionType = connectionHelper.getCurrentConnectionType();
        Urn userUrn = accountOperations.getLoggedInUserUrn();
        PlaybackProtocol playbackProtocol = getPlaybackProtocol();
        switch (metric) {
            case TIME_TO_PLAY:
                return PlaybackPerformanceEvent.timeToPlay(value, playbackProtocol, PlayerType.SKIPPY, currentConnectionType, cdnHost,
                        userUrn);
            case TIME_TO_BUFFER:
                return PlaybackPerformanceEvent.timeToBuffer(value, playbackProtocol, PlayerType.SKIPPY, currentConnectionType, cdnHost,
                        userUrn);
            case TIME_TO_GET_PLAYLIST:
                return PlaybackPerformanceEvent.timeToPlaylist(value, playbackProtocol, PlayerType.SKIPPY, currentConnectionType, cdnHost,
                        userUrn);
            case TIME_TO_SEEK:
                return PlaybackPerformanceEvent.timeToSeek(value, playbackProtocol, PlayerType.SKIPPY, currentConnectionType, cdnHost,
                        userUrn);
            case FRAGMENT_DOWNLOAD_RATE:
                return PlaybackPerformanceEvent.fragmentDownloadRate(value, playbackProtocol, PlayerType.SKIPPY, currentConnectionType, cdnHost,
                        userUrn);
            case TIME_TO_LOAD_LIBRARY:
                return PlaybackPerformanceEvent.timeToLoad(value, playbackProtocol, PlayerType.SKIPPY, currentConnectionType, cdnHost,
                        userUrn);
            case CACHE_USAGE_PERCENT:
                return PlaybackPerformanceEvent.cacheUsagePercent(value, playbackProtocol, PlayerType.SKIPPY, currentConnectionType, cdnHost);
            case UNINTERRUPTED_PLAYTIME:
                return PlaybackPerformanceEvent.uninterruptedPlaytimeMs(value, playbackProtocol, PlayerType.SKIPPY, currentConnectionType, cdnHost);
            default:
                throw new IllegalArgumentException("Unexpected performance metric : " + metric);
        }
    }

    @Override
    public void onProgressChange(long position, long duration, String uri) {
        final long adjustedPosition = fixPosition(position, duration);
        if (playerListener != null && uri.equals(currentStreamUrl)){
            playerListener.onProgressEvent(adjustedPosition, duration);
        }
    }

    private long fixPosition(long position, long duration) {
        if (position > duration){
            ErrorUtils.handleSilentException("track ["+ currentTrackUrn + "] : position [" + position + "] > duration [" + duration + "].",
                    new IllegalStateException("Skippy inconsistent state : position > duration"));
            return duration;
        }
        return position;
    }

    @Override
    public void onErrorMessage(String category, String sourceFile, int line, String errorMsg, String uri, String cdn) {
        ConnectionType currentConnectionType = connectionHelper.getCurrentConnectionType();
        // TODO : remove this check, as Skippy should filter out timeouts. Leaving it for this release as a precaution - JS
        if (!ConnectionType.OFFLINE.equals(currentConnectionType)){
            // Use Log as Skippy dumps can be rather large
            ErrorUtils.handleSilentExceptionWithLog(new SkippyException(category, line, sourceFile), errorMsg);
        }

        final PlaybackErrorEvent event = new PlaybackErrorEvent(category, getPlaybackProtocol(),
                cdn, currentConnectionType);
        eventBus.publish(EventQueue.PLAYBACK_ERROR, event);
    }

    @Override
    public void onInitializationError(Throwable throwable, String message) {
        ErrorUtils.handleSilentExceptionWithLog(throwable, DebugUtils.getLogDump(INIT_ERROR_CUSTOM_LOG_LINE_COUNT));
        eventBus.publish(EventQueue.TRACKING, new SkippyInitilizationFailedEvent(throwable, message,
                getAndIncrementInitilizationErrors(), getInitializationSuccessCount()));
    }

    private int getAndIncrementInitilizationErrors() {
        int errors = getInitializationErrorCount() + 1;
        sharedPreferences.edit().putInt(SKIPPY_INIT_ERROR_COUNT_KEY, errors).apply();
        return errors;
    }

    private int getInitializationErrorCount() {
        return sharedPreferences.getInt(SKIPPY_INIT_ERROR_COUNT_KEY, 0);
    }

    private int getAndIncrementInitilizationSuccesses() {
        int successes = getInitializationSuccessCount() + 1;
        sharedPreferences.edit().putInt(SKIPPY_INIT_SUCCESS_COUNT_KEY, successes).apply();
        return successes;
    }

    private int getInitializationSuccessCount() {
        return sharedPreferences.getInt(SKIPPY_INIT_SUCCESS_COUNT_KEY, 0);
    }

    static class StateChangeHandler extends Handler {

        @Nullable private PlayerListener playerListener;
        @Nullable private BufferUnderrunListener bufferUnderrunListener;
        private final NetworkConnectionHelper connectionHelper;

        @Inject
        StateChangeHandler(@Named(ApplicationModule.MAIN_LOOPER) Looper looper, NetworkConnectionHelper connectionHelper) {
            super(looper);
            this.connectionHelper = connectionHelper;
        }

        public void setPlayerListener(@Nullable PlayerListener playerListener) {
            this.playerListener = playerListener;
        }

        public void setBufferUnderrunListener(BufferUnderrunListener bufferUnderrunListener) {
            this.bufferUnderrunListener = bufferUnderrunListener;
        }

        @Override
        public void handleMessage(Message msg) {
            final StateTransition stateTransition = (StateTransition) msg.obj;
            if (playerListener != null) {
                playerListener.onPlaystateChanged(stateTransition);
            }
            if (bufferUnderrunListener != null) {
                bufferUnderrunListener.onPlaystateChanged(stateTransition, PlaybackProtocol.HLS, PlayerType.SKIPPY, connectionHelper.getCurrentConnectionType());
            }
        }
    }


    private static class SkippyException extends Exception {
        private final String errorCategory;
        private final int line;
        private final String sourceFile;

        private SkippyException(String category, int line, String sourceFile) {
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
