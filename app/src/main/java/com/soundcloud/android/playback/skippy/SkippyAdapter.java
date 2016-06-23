package com.soundcloud.android.playback.skippy;

import static com.soundcloud.android.skippy.Skippy.PlaybackMetric;
import static com.soundcloud.android.skippy.Skippy.Reason.BUFFERING;
import static com.soundcloud.android.skippy.Skippy.Reason.COMPLETE;
import static com.soundcloud.android.skippy.Skippy.Reason.ERROR;
import static com.soundcloud.java.checks.Preconditions.checkState;

import com.soundcloud.android.ApplicationModule;
import com.soundcloud.android.accounts.AccountOperations;
import com.soundcloud.android.ads.AdUtils;
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
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.offline.SecureFileStorage;
import com.soundcloud.android.playback.AudioAdPlaybackItem;
import com.soundcloud.android.playback.BufferUnderrunListener;
import com.soundcloud.android.playback.PlayStateReason;
import com.soundcloud.android.playback.PlaybackItem;
import com.soundcloud.android.playback.PlaybackProtocol;
import com.soundcloud.android.playback.PlaybackState;
import com.soundcloud.android.playback.PlaybackStateTransition;
import com.soundcloud.android.playback.PlaybackType;
import com.soundcloud.android.playback.Player;
import com.soundcloud.android.playback.PreloadItem;
import com.soundcloud.android.skippy.Skippy;
import com.soundcloud.android.skippy.SkippyPreloader;
import com.soundcloud.android.utils.CurrentDateProvider;
import com.soundcloud.android.utils.DebugUtils;
import com.soundcloud.android.utils.ErrorUtils;
import com.soundcloud.android.utils.LockUtil;
import com.soundcloud.android.utils.Log;
import com.soundcloud.android.utils.NetworkConnectionHelper;
import com.soundcloud.java.strings.Strings;
import com.soundcloud.rx.eventbus.EventBus;
import org.jetbrains.annotations.Nullable;

import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.support.annotation.VisibleForTesting;

import javax.inject.Inject;
import javax.inject.Named;
import java.util.concurrent.TimeUnit;

public class SkippyAdapter implements Player, Skippy.PlayListener {

    private static final String TAG = "SkippyAdapter";
    @VisibleForTesting
    static final String SKIPPY_INIT_ERROR_COUNT_KEY = "SkippyAdapter.initErrorCount";
    static final String SKIPPY_INIT_SUCCESS_COUNT_KEY = "SkippyAdapter.initSuccessCount";
    static final long PRELOAD_DURATION = TimeUnit.SECONDS.toMillis(10);
    static final int PRELOAD_START_POSITION = 0;
    static final String PARAM_URL = "url";
    static final String PARAM_DURATION = "duration";
    private static final String PARAM_CAN_SNIP = "can_snip";

    private static final int INIT_ERROR_CUSTOM_LOG_LINE_COUNT = 5000;
    private final SkippyFactory skippyFactory;
    private final LockUtil lockUtil;

    private final EventBus eventBus;
    private final Skippy skippy;
    private final SkippyPreloader skippyPreloader;
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
    SkippyAdapter(SkippyFactory skippyFactory,
                  AccountOperations accountOperations,
                  ApiUrlBuilder urlBuilder,
                  StateChangeHandler stateChangeHandler,
                  EventBus eventBus,
                  NetworkConnectionHelper connectionHelper,
                  LockUtil lockUtil,
                  BufferUnderrunListener bufferUnderrunListener,
                  SharedPreferences sharedPreferences,
                  SecureFileStorage secureFileStorage,
                  CryptoOperations cryptoOperations,
                  CurrentDateProvider dateProvider) {
        this.skippyFactory = skippyFactory;
        this.lockUtil = lockUtil;
        this.bufferUnderrunListener = bufferUnderrunListener;
        this.sharedPreferences = sharedPreferences;
        this.secureFileStorage = secureFileStorage;
        this.cryptoOperations = cryptoOperations;
        this.accountOperations = accountOperations;
        this.urlBuilder = urlBuilder;
        this.eventBus = eventBus;
        this.connectionHelper = connectionHelper;
        this.stateHandler = stateChangeHandler;
        this.stateHandler.setBufferUnderrunListener(bufferUnderrunListener);
        this.dateProvider = dateProvider;

        skippy = skippyFactory.create(this);
        skippyPreloader = skippyFactory.createPreloader();
    }

    public boolean init() {
        boolean initSuccess = skippy.init(skippyFactory.createConfiguration());
        if (initSuccess) {
            skippyPreloader.init();
        }
        if (initSuccess) {
            eventBus.publish(EventQueue.TRACKING, new SkippyInitilizationSucceededEvent(
                    getInitializationErrorCount(), getAndIncrementInitilizationSuccesses()));
        }
        return initSuccess;
    }

    public void preload(PreloadItem preloadItem) {
        try {
            skippyPreloader.fetch(buildRemoteUrl(preloadItem.getUrn(), preloadItem.getPlaybackType()),
                                  PRELOAD_START_POSITION, PRELOAD_DURATION);
        } catch (UnsatisfiedLinkError e) {
            // #4454. Remove in upcoming release as instances drop
            ErrorUtils.handleSilentException(e);
        }
    }

    @Override
    public void play(PlaybackItem playbackItem) {
        final long fromPos = playbackItem.getStartPosition();
        currentTrackUrn = playbackItem.getUrn();

        if (!accountOperations.isUserLoggedIn()) {
            throw new IllegalStateException("Cannot play a track if no soundcloud account exists");
        }

        stateHandler.removeMessages(0);
        lastStateChangeProgress = 0;

        final String trackUrl = buildStreamUrl(playbackItem);
        if (trackUrl.equals(currentStreamUrl)) {
            // we are already playing it. seek and resume
            skippy.seek(fromPos);
            skippy.resume();
        } else {
            startPlayback(playbackItem, fromPos);
        }
    }

    private void startPlayback(PlaybackItem playbackItem, long fromPos) {
        currentStreamUrl = buildStreamUrl(playbackItem);
        switch (playbackItem.getPlaybackType()) {
            case AUDIO_OFFLINE:
                final DeviceSecret deviceSecret = cryptoOperations.checkAndGetDeviceKey();
                skippy.playOffline(currentStreamUrl, fromPos, deviceSecret.getKey(), deviceSecret.getInitVector());
                break;
            case AUDIO_AD:
                final boolean shouldCache = !AdUtils.isThirdPartyAudioAd(playbackItem.getUrn());
                skippy.play(currentStreamUrl, fromPos, shouldCache);
                break;
            default:
                skippy.play(currentStreamUrl, fromPos);
                break;
        }
    }

    private String buildStreamUrl(PlaybackItem playbackItem) {
        checkState(accountOperations.isUserLoggedIn(), "SoundCloud User account does not exist");

        final PlaybackType playType = playbackItem.getPlaybackType();
        switch (playbackItem.getPlaybackType()) {
            case AUDIO_OFFLINE:
                return secureFileStorage.getFileUriForOfflineTrack(currentTrackUrn).toString();
            case AUDIO_AD:
                return buildAudioAdUrl((AudioAdPlaybackItem) playbackItem);
            default:
                return buildRemoteUrl(currentTrackUrn, playType);
        }
    }

    private String buildAudioAdUrl(AudioAdPlaybackItem adPlaybackItem) {
        return adPlaybackItem.isThirdParty()
               ? buildThirdPartyAdHlsUrl(adPlaybackItem)
               : buildRemoteUrl(adPlaybackItem.getUrn(), PlaybackType.AUDIO_AD);
    }

    private String buildThirdPartyAdHlsUrl(AudioAdPlaybackItem adPlaybackItem) {
        Token token = accountOperations.getSoundCloudToken();
        ApiUrlBuilder builder = urlBuilder.from(ApiEndpoints.STREAMS_TO_HLS);
        if (token.valid()) {
            builder.withQueryParam(ApiRequest.Param.OAUTH_TOKEN, token.getAccessToken());
        }
        return builder.withQueryParam(PARAM_URL, adPlaybackItem.getThirdPartyStreamUrl())
                      .withQueryParam(PARAM_DURATION, String.valueOf(adPlaybackItem.getDuration()))
                      .build();
    }

    private String buildRemoteUrl(Urn trackUrn, PlaybackType playType) {
        if (playType == PlaybackType.AUDIO_SNIPPET) {
            return getApiUrlBuilder(trackUrn, ApiEndpoints.HLS_SNIPPET_STREAM).build();
        } else {
            return getApiUrlBuilder(trackUrn, ApiEndpoints.HLS_STREAM).withQueryParam(PARAM_CAN_SNIP, false).build();
        }
    }

    private ApiUrlBuilder getApiUrlBuilder(Urn trackUrn, ApiEndpoints endpoint) {
        Token token = accountOperations.getSoundCloudToken();
        ApiUrlBuilder builder = urlBuilder.from(endpoint, trackUrn);
        if (token.valid()) {
            builder.withQueryParam(ApiRequest.Param.OAUTH_TOKEN, token.getAccessToken());
        }
        return builder;
    }

    @Override
    public void resume(PlaybackItem playbackItem) {
        if (playbackItem.getUrn().equals(currentTrackUrn)) {
            if (currentStreamUrl != null) {
                skippy.resume();
            } else {
                startPlayback(playbackItem, lastStateChangeProgress);
            }
        } else {
            play(playbackItem);
        }
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
            if (playerListener != null && duration != 0) {
                playerListener.onProgressEvent(position, duration);
            }
        }
        return position;
    }

    @Override
    public long getProgress() {
        if (currentStreamUrl != null) {
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
    public float getVolume() {
        return skippy.getVolume();
    }

    @Override
    public void stop() {
        skippy.stop();
        currentStreamUrl = null;
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
    public void onStateChanged(Skippy.State state,
                               Skippy.Reason reason,
                               Skippy.Error errorCode,
                               long position,
                               long duration,
                               String uri,
                               Skippy.SkippyMediaType format,
                               int bitrate) {
        try {
            handleStateChanged(state, reason, errorCode, position, duration, uri, format, bitrate);
        } catch (Throwable t) {
            ErrorUtils.handleThrowable(t, getClass());
        }
    }

    private void handleStateChanged(Skippy.State state,
                                    Skippy.Reason reason,
                                    Skippy.Error errorCode,
                                    long position,
                                    long duration,
                                    String uri,
                                    Skippy.SkippyMediaType format,
                                    int bitrate) {
        final long adjustedPosition = fixPosition(position, duration);

        Log.i(TAG, "State = " + state + " : " + reason + " : " + errorCode);
        if (uri.equals(currentStreamUrl)) {
            lastStateChangeProgress = position;

            final PlaybackState translatedState = getTranslatedState(state, reason);
            final PlayStateReason translatedReason = getTranslatedReason(reason, errorCode);
            final PlaybackStateTransition transition = new PlaybackStateTransition(translatedState,
                                                                                   translatedReason,
                                                                                   currentTrackUrn,
                                                                                   adjustedPosition,
                                                                                   duration,
                                                                                   format.name(),
                                                                                   bitrate,
                                                                                   dateProvider);
            transition.addExtraAttribute(PlaybackStateTransition.EXTRA_PLAYBACK_PROTOCOL,
                                         getPlaybackProtocol().getValue());
            transition.addExtraAttribute(PlaybackStateTransition.EXTRA_PLAYER_TYPE, PlayerType.SKIPPY.getValue());
            transition.addExtraAttribute(PlaybackStateTransition.EXTRA_CONNECTION_TYPE,
                                         connectionHelper.getCurrentConnectionType().getValue());
            transition.addExtraAttribute(PlaybackStateTransition.EXTRA_NETWORK_AND_WAKE_LOCKS_ACTIVE,
                                         String.valueOf(true));
            transition.addExtraAttribute(PlaybackStateTransition.EXTRA_URI, uri);

            if (transition.playbackHasStopped()) {
                currentStreamUrl = null;
            }

            Message msg = stateHandler.obtainMessage(0, transition);
            stateHandler.sendMessage(msg);
            configureLockBasedOnNewState(transition);
        }
    }

    private void configureLockBasedOnNewState(PlaybackStateTransition transition) {
        if (transition.isPlayerPlaying() || transition.isBuffering()) {
            lockUtil.lock();
        } else {
            lockUtil.unlock();
        }
    }

    @Override
    public void onPerformanceMeasured(PlaybackMetric metric,
                                      long value,
                                      String uri,
                                      String cdnHost,
                                      Skippy.SkippyMediaType format,
                                      int bitRate) {
        if (!accountOperations.isUserLoggedIn() || metric.equals(PlaybackMetric.TIME_TO_BUFFER)) {
            return;
        }

        if (allowPerformanceMeasureEvent(metric)) {
            eventBus.publish(EventQueue.PLAYBACK_PERFORMANCE,
                             createPerformanceEvent(metric, value, cdnHost, format, bitRate));
        }
    }

    private boolean allowPerformanceMeasureEvent(PlaybackMetric metric) {
        // Time to load library & cache usage events are not specific to the current playing track
        return metric == PlaybackMetric.TIME_TO_LOAD_LIBRARY || metric == PlaybackMetric.CACHE_USAGE_PERCENT || !AdUtils
                .isThirdPartyAudioAd(currentTrackUrn);
    }

    @Override
    public void onDownloadPerformed(long startPosition,
                                    long endPosition,
                                    int bytesLoaded,
                                    int bytesTotal,
                                    String uri,
                                    Skippy.SkippyMediaType format,
                                    int bitRate) {
        //Not implemented yet!
    }

    private PlaybackState getTranslatedState(Skippy.State state, Skippy.Reason reason) {
        switch (state) {
            case IDLE:
                return PlaybackState.IDLE;
            case PLAYING:
                return reason == BUFFERING ? PlaybackState.BUFFERING : PlaybackState.PLAYING;
            default:
                throw new IllegalArgumentException("Unexpected skippy state : " + state);
        }
    }

    private PlayStateReason getTranslatedReason(Skippy.Reason reason, Skippy.Error lastError) {
        if (reason == ERROR) {
            switch (lastError) {
                case FAILED:
                case TIMEOUT:
                    return PlayStateReason.ERROR_FAILED;
                case FORBIDDEN:
                    return PlayStateReason.ERROR_FORBIDDEN;
                case MEDIA_NOT_FOUND:
                    return PlayStateReason.ERROR_NOT_FOUND;
                default:
                    throw new IllegalArgumentException("Unexpected skippy error code : " + lastError);
            }
        } else if (reason == COMPLETE) {
            return PlayStateReason.PLAYBACK_COMPLETE;
        } else {
            return PlayStateReason.NONE;
        }
    }

    private PlaybackProtocol getPlaybackProtocol() {
        return PlaybackProtocol.HLS;
    }

    @Nullable
    private PlaybackPerformanceEvent createPerformanceEvent(PlaybackMetric metric,
                                                            long value,
                                                            String cdnHost,
                                                            Skippy.SkippyMediaType format,
                                                            int bitRate) {
        ConnectionType currentConnectionType = connectionHelper.getCurrentConnectionType();
        Urn userUrn = accountOperations.getLoggedInUserUrn();
        PlaybackProtocol playbackProtocol = getPlaybackProtocol();
        switch (metric) {
            case TIME_TO_PLAY:
                return PlaybackPerformanceEvent.timeToPlay(value,
                                                           playbackProtocol,
                                                           PlayerType.SKIPPY,
                                                           currentConnectionType,
                                                           cdnHost,
                                                           format.name(),
                                                           bitRate,
                                                           userUrn,
                                                           false);
            case TIME_TO_BUFFER:
                return PlaybackPerformanceEvent.timeToBuffer(value,
                                                             playbackProtocol,
                                                             PlayerType.SKIPPY,
                                                             currentConnectionType,
                                                             cdnHost,
                                                             format.name(),
                                                             bitRate,
                                                             userUrn);
            case TIME_TO_GET_PLAYLIST:
                return PlaybackPerformanceEvent.timeToPlaylist(value,
                                                               playbackProtocol,
                                                               PlayerType.SKIPPY,
                                                               currentConnectionType,
                                                               cdnHost,
                                                               format.name(),
                                                               bitRate,
                                                               userUrn);
            case TIME_TO_SEEK:
                return PlaybackPerformanceEvent.timeToSeek(value,
                                                           playbackProtocol,
                                                           PlayerType.SKIPPY,
                                                           currentConnectionType,
                                                           cdnHost,
                                                           format.name(),
                                                           bitRate,
                                                           userUrn);
            case FRAGMENT_DOWNLOAD_RATE:
                return PlaybackPerformanceEvent.fragmentDownloadRate(value,
                                                                     playbackProtocol,
                                                                     PlayerType.SKIPPY,
                                                                     currentConnectionType,
                                                                     cdnHost,
                                                                     format.name(),
                                                                     bitRate,
                                                                     userUrn);
            case TIME_TO_LOAD_LIBRARY:
                return PlaybackPerformanceEvent.timeToLoad(value,
                                                           playbackProtocol,
                                                           PlayerType.SKIPPY,
                                                           currentConnectionType,
                                                           cdnHost,
                                                           format.name(),
                                                           bitRate,
                                                           userUrn);
            case CACHE_USAGE_PERCENT:
                return PlaybackPerformanceEvent.cacheUsagePercent(value,
                                                                  playbackProtocol,
                                                                  PlayerType.SKIPPY,
                                                                  currentConnectionType,
                                                                  cdnHost,
                                                                  format.name(),
                                                                  bitRate);
            case UNINTERRUPTED_PLAYTIME:
                return PlaybackPerformanceEvent.uninterruptedPlaytimeMs(value,
                                                                        playbackProtocol,
                                                                        PlayerType.SKIPPY,
                                                                        currentConnectionType,
                                                                        cdnHost,
                                                                        format.name(),
                                                                        bitRate,
                                                                        false);
            default:
                throw new IllegalArgumentException("Unexpected performance metric : " + metric);
        }
    }

    @Override
    public void onProgressChange(long position, long duration, String uri, Skippy.SkippyMediaType format, int bitRate) {
        final long adjustedPosition = fixPosition(position, duration);
        if (playerListener != null && uri.equals(currentStreamUrl)) {
            playerListener.onProgressEvent(adjustedPosition, duration);
        }
    }

    private long fixPosition(long position, long duration) {
        if (position > duration) {
            ErrorUtils.handleSilentException("track [" + currentTrackUrn + "] : position [" + position + "] > duration [" + duration + "].",
                                             new IllegalStateException("Skippy inconsistent state : position > duration"));
            return duration;
        }
        return position;
    }

    @Override
    public void onErrorMessage(String category,
                               String sourceFile,
                               int line,
                               String errorMsg,
                               String uri,
                               String cdn,
                               Skippy.SkippyMediaType format,
                               int bitRate) {
        ConnectionType currentConnectionType = connectionHelper.getCurrentConnectionType();
        // TODO : remove this check, as Skippy should filter out timeouts. Leaving it for this release as a precaution - JS
        if (!ConnectionType.OFFLINE.equals(currentConnectionType)) {
            // Use Log as Skippy dumps can be rather large
            ErrorUtils.handleSilentExceptionWithLog(new SkippyException(category, line, sourceFile), errorMsg);
        }

        final PlaybackErrorEvent event = new PlaybackErrorEvent(category, getPlaybackProtocol(),
                                                                cdn, format.name(), bitRate, currentConnectionType);
        eventBus.publish(EventQueue.PLAYBACK_ERROR, event);
    }

    @Override
    public void onInitializationError(Throwable throwable, String message) {
        ErrorUtils.handleSilentExceptionWithLog(throwable, DebugUtils.getLogDump(INIT_ERROR_CUSTOM_LOG_LINE_COUNT));
        eventBus.publish(EventQueue.TRACKING, new SkippyInitilizationFailedEvent(throwable,
                                                                                 message,
                                                                                 getAndIncrementInitilizationErrors(),
                                                                                 getInitializationSuccessCount()));
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
        StateChangeHandler(@Named(ApplicationModule.MAIN_LOOPER) Looper looper,
                           NetworkConnectionHelper connectionHelper) {
            super(looper);
            this.connectionHelper = connectionHelper;
        }

        public void setPlayerListener(@Nullable PlayerListener playerListener) {
            this.playerListener = playerListener;
        }

        public void setBufferUnderrunListener(@Nullable BufferUnderrunListener bufferUnderrunListener) {
            this.bufferUnderrunListener = bufferUnderrunListener;
        }

        @Override
        public void handleMessage(Message msg) {
            final PlaybackStateTransition stateTransition = (PlaybackStateTransition) msg.obj;
            if (playerListener != null) {
                playerListener.onPlaystateChanged(stateTransition);
            }
            if (bufferUnderrunListener != null) {
                bufferUnderrunListener.onPlaystateChanged(stateTransition,
                                                          PlaybackProtocol.HLS,
                                                          PlayerType.SKIPPY,
                                                          connectionHelper.getCurrentConnectionType());
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
