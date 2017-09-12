package com.soundcloud.android.playback.skippy;

import static com.soundcloud.android.skippy.Skippy.PlaybackMetric;
import static com.soundcloud.android.skippy.Skippy.Reason.BUFFERING;
import static com.soundcloud.android.skippy.Skippy.Reason.COMPLETE;
import static com.soundcloud.android.skippy.Skippy.Reason.ERROR;

import com.soundcloud.android.ApplicationModule;
import com.soundcloud.android.accounts.AccountOperations;
import com.soundcloud.android.crypto.CryptoOperations;
import com.soundcloud.android.crypto.DeviceSecret;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.FileAccessEvent;
import com.soundcloud.android.events.PlaybackErrorEvent;
import com.soundcloud.android.events.PlaybackPerformanceEvent;
import com.soundcloud.android.events.PlayerType;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playback.BufferUnderrunListener;
import com.soundcloud.android.playback.HlsStreamUrlBuilder;
import com.soundcloud.android.playback.PlayStateReason;
import com.soundcloud.android.playback.PlaybackConstants;
import com.soundcloud.android.playback.PlaybackItem;
import com.soundcloud.android.playback.PlaybackProtocol;
import com.soundcloud.android.playback.PlaybackState;
import com.soundcloud.android.playback.PlaybackStateTransition;
import com.soundcloud.android.playback.PlaybackType;
import com.soundcloud.android.playback.Player;
import com.soundcloud.android.playback.PreloadItem;
import com.soundcloud.android.skippy.Skippy;
import com.soundcloud.android.skippy.SkippyPreloader;
import com.soundcloud.android.utils.ConnectionHelper;
import com.soundcloud.android.utils.CurrentDateProvider;
import com.soundcloud.android.utils.DebugUtils;
import com.soundcloud.android.utils.ErrorUtils;
import com.soundcloud.android.utils.LockUtil;
import com.soundcloud.android.utils.Log;
import com.soundcloud.rx.eventbus.EventBusV2;
import org.jetbrains.annotations.Nullable;

import android.os.Looper;
import android.os.Message;
import android.support.annotation.NonNull;

import javax.inject.Inject;
import javax.inject.Named;
import java.io.File;
import java.util.concurrent.TimeUnit;

public class SkippyAdapter implements Player, Skippy.PlayListener {

    private static final String TAG = "SkippyAdapter";
    static final long PRELOAD_DURATION = TimeUnit.SECONDS.toMillis(10);
    static final int PRELOAD_START_POSITION = 0;

    private static final int INIT_ERROR_CUSTOM_LOG_LINE_COUNT = 5000;

    private final SkippyFactory skippyFactory;
    private final LockUtil lockUtil;

    private final EventBusV2 eventBus;
    private final Skippy skippy;
    private final SkippyPreloader skippyPreloader;
    private final AccountOperations accountOperations;
    private final BufferUnderrunStateChangeHandler stateHandler;
    private final ProgressChangeHandler progressChangeHandler;
    private final HlsStreamUrlBuilder hlsStreamUrlBuilder;
    private final ConnectionHelper connectionHelper;
    private final BufferUnderrunListener bufferUnderrunListener;
    private final CryptoOperations cryptoOperations;
    private final CurrentDateProvider dateProvider;

    private Urn latestItemUrn;
    private volatile String currentStreamUrl;
    private PlaybackItem currentPlaybackItem;
    private PlayerListener playerListener;
    private long lastStateChangeProgress;

    @Inject
    SkippyAdapter(SkippyFactory skippyFactory,
                  AccountOperations accountOperations,
                  HlsStreamUrlBuilder hlsStreamUrlBuilder,
                  BufferUnderrunStateChangeHandler bufferUnderrunStateChangeHandler,
                  ProgressChangeHandler progressChangeHandler,
                  EventBusV2 eventBus,
                  ConnectionHelper connectionHelper,
                  LockUtil lockUtil,
                  BufferUnderrunListener bufferUnderrunListener,
                  CryptoOperations cryptoOperations,
                  CurrentDateProvider dateProvider) {
        this.skippyFactory = skippyFactory;
        this.hlsStreamUrlBuilder = hlsStreamUrlBuilder;
        this.progressChangeHandler = progressChangeHandler;
        this.lockUtil = lockUtil;
        this.bufferUnderrunListener = bufferUnderrunListener;
        this.cryptoOperations = cryptoOperations;
        this.accountOperations = accountOperations;
        this.eventBus = eventBus;
        this.connectionHelper = connectionHelper;
        this.stateHandler = bufferUnderrunStateChangeHandler;
        this.stateHandler.setBufferUnderrunListener(bufferUnderrunListener);
        this.dateProvider = dateProvider;

        skippy = skippyFactory.create(this);
        skippyPreloader = skippyFactory.createPreloader();
    }

    public boolean init() {
        Skippy.Configuration configuration = skippyFactory.createConfiguration();
        sendFileAccessEvent(configuration);
        boolean initSuccess = skippy.init(configuration);
        if (initSuccess) {
            skippyPreloader.init();
        }
        return initSuccess;
    }

    private void sendFileAccessEvent(Skippy.Configuration configuration) {
        final String cachePath = configuration.getCachePath();
        if (cachePath == null) {
            eventBus.publish(EventQueue.TRACKING, FileAccessEvent.create(false, false, false));
        } else {
            File file = new File(cachePath);
            boolean exists = file.exists();
            boolean canWrite = file.canWrite();
            boolean canRead = file.canRead();
            eventBus.publish(EventQueue.TRACKING, FileAccessEvent.create(exists, canWrite, canRead));
        }

    }

    @Override
    public void preload(@NonNull PreloadItem preloadItem) {
        try {
            skippyPreloader.fetch(hlsStreamUrlBuilder.buildStreamUrl(preloadItem), PRELOAD_START_POSITION, PRELOAD_DURATION);
        } catch (UnsatisfiedLinkError e) {
            // #4454. Remove in upcoming release as instances drop
            ErrorUtils.handleSilentException(e);
        }
    }

    @Override
    public void play(@NonNull PlaybackItem playbackItem) {
        final long fromPos = playbackItem.getStartPosition();

        if (!accountOperations.isUserLoggedIn()) {
            throw new IllegalStateException("Cannot play a track if no soundcloud account exists");
        }

        stateHandler.removeMessages(0);
        lastStateChangeProgress = 0;

        final String trackUrl = hlsStreamUrlBuilder.buildStreamUrl(playbackItem);
        if (trackUrl.equals(currentStreamUrl)) {
            // we are already playing it. seek and resume
            skippy.seek(fromPos);
            skippy.resume();
        } else {
            startPlayback(playbackItem, fromPos);
        }
    }

    private void startPlayback(PlaybackItem playbackItem, long fromPos) {
        latestItemUrn = playbackItem.getUrn();
        currentPlaybackItem = playbackItem;
        currentStreamUrl = hlsStreamUrlBuilder.buildStreamUrl(playbackItem);
        switch (playbackItem.getPlaybackType()) {
            case AUDIO_OFFLINE:
                final DeviceSecret deviceSecret = cryptoOperations.checkAndGetDeviceKey();
                skippy.playOffline(currentStreamUrl, fromPos, deviceSecret.getKey(), deviceSecret.getInitVector());
                break;
            case AUDIO_AD:
                final boolean shouldCache = false;
                skippy.play(currentStreamUrl, fromPos, shouldCache);
                break;
            default:
                skippy.play(currentStreamUrl, fromPos);
                break;
        }
    }

    @Override
    public void resume(@NonNull PlaybackItem playbackItem) {
        if (playbackItem.getUrn().equals(latestItemUrn)) {
            if (currentPlaybackItem != null) {
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
    public void seek(long position) {
        bufferUnderrunListener.onSeek();
        skippy.seek(position);

        long duration = skippy.getDuration();
        if (playerListener != null && duration != 0) {
            playerListener.onProgressEvent(position, duration);
        }
    }

    @Override
    public long getProgress() {
        if (currentPlaybackItem != null) {
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
        currentPlaybackItem = null;
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
    public void setListener(@NonNull PlayerListener playerListener) {
        this.playerListener = playerListener;
        this.stateHandler.setPlayerListener(playerListener);
        this.progressChangeHandler.setPlayerListener(playerListener);
    }

    @Override
    public boolean isSeekable() {
        return true;
    }

    @Override
    public PlayerType getPlayerType() {
        return PlayerType.SKIPPY;
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
            final String translatedFormat = getTranslatedFormat(format);
            final PlaybackStateTransition transition = new PlaybackStateTransition(translatedState,
                                                                                   translatedReason,
                                                                                   currentPlaybackItem.getUrn(),
                                                                                   adjustedPosition,
                                                                                   duration,
                                                                                   translatedFormat,
                                                                                   bitrate,
                                                                                   dateProvider);
            transition.addExtraAttribute(PlaybackStateTransition.EXTRA_PLAYBACK_PROTOCOL,
                                         getPlaybackProtocol().getValue());
            transition.addExtraAttribute(PlaybackStateTransition.EXTRA_PLAYER_TYPE, getPlayerType().getValue());
            transition.addExtraAttribute(PlaybackStateTransition.EXTRA_CONNECTION_TYPE,
                                         connectionHelper.getCurrentConnectionType().getValue());
            transition.addExtraAttribute(PlaybackStateTransition.EXTRA_NETWORK_AND_WAKE_LOCKS_ACTIVE,
                                         String.valueOf(true));
            transition.addExtraAttribute(PlaybackStateTransition.EXTRA_URI, uri);

            stateHandler.report(currentPlaybackItem, transition);
            configureLockBasedOnNewState(transition);

            if (transition.playbackHasStopped()) {
                currentPlaybackItem = null;
                currentStreamUrl = null;
            }
        }
    }

    private String getTranslatedFormat(Skippy.SkippyMediaType format) {
        switch (format) {
            case OPUS:
                return PlaybackConstants.MediaType.OPUS;
            case MP3:
                return PlaybackConstants.MediaType.MP3;
            default:
                return PlaybackConstants.MediaType.UNKNOWN;
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
        return metric == PlaybackMetric.TIME_TO_LOAD_LIBRARY
                || metric == PlaybackMetric.CACHE_USAGE_PERCENT
                || metric == PlaybackMetric.TIME_TO_PLAY
                || metric == PlaybackMetric.UNINTERRUPTED_PLAYTIME
                || !isCurrentItemAd();
    }

    private boolean isCurrentItemAd() {
        return currentPlaybackItem != null && currentPlaybackItem.getPlaybackType() == PlaybackType.AUDIO_AD;
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
        PlaybackPerformanceEvent.Builder builder;

        switch (metric) {
            case TIME_TO_PLAY:
                builder = PlaybackPerformanceEvent.timeToPlay(currentPlaybackItem.getPlaybackType());
                break;
            case TIME_TO_BUFFER:
                builder = PlaybackPerformanceEvent.timeToBuffer();
                break;
            case TIME_TO_GET_PLAYLIST:
                builder = PlaybackPerformanceEvent.timeToPlaylist();
                break;
            case TIME_TO_SEEK:
                builder = PlaybackPerformanceEvent.timeToSeek();
                break;
            case FRAGMENT_DOWNLOAD_RATE:
                builder = PlaybackPerformanceEvent.fragmentDownloadRate();
                break;
            case TIME_TO_LOAD_LIBRARY:
                builder = PlaybackPerformanceEvent.timeToLoad();
                break;
            case CACHE_USAGE_PERCENT:
                builder = PlaybackPerformanceEvent.cacheUsagePercent();
                break;
            case UNINTERRUPTED_PLAYTIME:
                builder = PlaybackPerformanceEvent.uninterruptedPlaytimeMs(currentPlaybackItem.getPlaybackType());
                break;
            default:
                throw new IllegalArgumentException("Unexpected performance metric : " + metric);
        }

        return builder.metricValue(value)
                      .protocol(getPlaybackProtocol())
                      .playerType(getPlayerType())
                      .connectionType(connectionHelper.getCurrentConnectionType())
                      .cdnHost(cdnHost)
                      .format(format.name())
                      .bitrate(bitRate)
                      .userUrn(accountOperations.getLoggedInUserUrn())
                      .build();
    }

    @Override
    public void onProgressChange(long position, long duration, String uri, Skippy.SkippyMediaType format, int bitRate) {
        final long adjustedPosition = fixPosition(position, duration);
        if (uri.equals(currentStreamUrl)) {
            progressChangeHandler.report(adjustedPosition, duration);
        }
    }

    private long fixPosition(long position, long duration) {
        return position > duration ? duration : position;
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
        eventBus.publish(EventQueue.PLAYBACK_ERROR, new PlaybackErrorEvent(category, getPlaybackProtocol(), cdn, format.name(), bitRate, getPlayerType()));
    }

    @Override
    public void onInitializationError(Throwable throwable, String message) {
        ErrorUtils.handleSilentExceptionWithLog(throwable, DebugUtils.getLogDump(INIT_ERROR_CUSTOM_LOG_LINE_COUNT));
    }

    public static class BufferUnderrunStateChangeHandler extends StateChangeHandler {

        @Nullable private BufferUnderrunListener bufferUnderrunListener;
        private final ConnectionHelper connectionHelper;

        @Inject
        BufferUnderrunStateChangeHandler(@Named(ApplicationModule.MAIN_LOOPER) Looper looper,
                                         ConnectionHelper connectionHelper) {
            super(looper);
            this.connectionHelper = connectionHelper;
        }

        void setBufferUnderrunListener(@Nullable BufferUnderrunListener bufferUnderrunListener) {
            this.bufferUnderrunListener = bufferUnderrunListener;
        }

        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);

            final StateChangeMessage message = (StateChangeMessage) msg.obj;
            if (bufferUnderrunListener != null) {
                bufferUnderrunListener.onPlaystateChanged(message.playbackItem,
                                                          message.stateTransition,
                                                          PlaybackProtocol.HLS,
                                                          PlayerType.SKIPPY,
                                                          connectionHelper.getCurrentConnectionType());
            }
        }
    }
}
