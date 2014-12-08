package com.soundcloud.android.playback.service.skippy;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;
import static com.soundcloud.android.events.PlaybackPerformanceEvent.ConnectionType;
import static com.soundcloud.android.events.PlaybackPerformanceEvent.PlayerType;
import static com.soundcloud.android.skippy.Skippy.ErrorCategory;
import static com.soundcloud.android.skippy.Skippy.PlaybackMetric;
import static com.soundcloud.android.skippy.Skippy.Reason.BUFFERING;
import static com.soundcloud.android.skippy.Skippy.Reason.COMPLETE;
import static com.soundcloud.android.skippy.Skippy.Reason.ERROR;

import com.soundcloud.android.accounts.AccountOperations;
import com.soundcloud.android.api.ApiEndpoints;
import com.soundcloud.android.api.ApiRequest;
import com.soundcloud.android.api.ApiUrlBuilder;
import com.soundcloud.android.events.BufferUnderrunEvent;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.PlaybackErrorEvent;
import com.soundcloud.android.events.PlaybackPerformanceEvent;
import com.soundcloud.android.events.SkippyPlayEvent;
import com.soundcloud.android.model.PlayableProperty;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playback.PlaybackProtocol;
import com.soundcloud.android.playback.service.Playa;
import com.soundcloud.android.rx.eventbus.EventBus;
import com.soundcloud.android.skippy.Skippy;
import com.soundcloud.android.tracks.TrackProperty;
import com.soundcloud.android.utils.DeviceHelper;
import com.soundcloud.android.utils.ErrorUtils;
import com.soundcloud.android.utils.LockUtil;
import com.soundcloud.android.utils.Log;
import com.soundcloud.android.utils.NetworkConnectionHelper;
import com.soundcloud.android.utils.ScTextUtils;
import com.soundcloud.android.api.oauth.Token;
import com.soundcloud.propeller.PropertySet;
import org.jetbrains.annotations.Nullable;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;

import javax.inject.Inject;
import javax.inject.Named;


public class SkippyAdapter implements Playa, Skippy.PlayListener {

    private static final String TAG = "SkippyAdapter";
    private static final long POSITION_START = 0L;
    private static final String PLAYER_NAME = "Skippy";
    private final SkippyFactory skippyFactory;
    private final LockUtil lockUtil;

    private final EventBus eventBus;
    private final Skippy skippy;
    private final AccountOperations accountOperations;
    private final StateChangeHandler stateHandler;
    private final ApiUrlBuilder urlBuilder;
    private final NetworkConnectionHelper connectionHelper;
    private final DeviceHelper deviceHelper;

    private volatile String currentStreamUrl;
    private Urn currentTrackUrn;
    private PlayaListener playaListener;
    private long lastStateChangeProgress;

    @Inject
    SkippyAdapter(SkippyFactory skippyFactory, AccountOperations accountOperations, ApiUrlBuilder urlBuilder,
                  StateChangeHandler stateChangeHandler, EventBus eventBus, NetworkConnectionHelper connectionHelper,
                  LockUtil lockUtil, DeviceHelper deviceHelper) {
        this.skippyFactory = skippyFactory;
        this.lockUtil = lockUtil;
        skippy = skippyFactory.create(this);
        this.accountOperations = accountOperations;
        this.urlBuilder = urlBuilder;
        stateHandler = stateChangeHandler;
        this.eventBus = eventBus;
        this.connectionHelper = connectionHelper;
        this.deviceHelper = deviceHelper;
    }

    public boolean init(Context context) {
        return skippy.init(context,skippyFactory.createConfiguration());
    }

    @Override
    public void play(PropertySet track) {
        play(track, POSITION_START);
    }

    @Override
    public void play(PropertySet track, long fromPos) {
        play(track, fromPos, false);
    }

    @Override
    public void playUninterrupted(PropertySet track) {
        play(track, POSITION_START, true);
    }

    private void play(PropertySet track, long fromPos, boolean uninterrupted) {
        currentTrackUrn = track.get(TrackProperty.URN);

        if (!accountOperations.isUserLoggedIn()) {
            throw new IllegalStateException("Cannot play a track if no soundcloud account exists");
        }

        // TODO : move audiofocus requesting into PlaybackService when we kill MediaPlayer
        if (playaListener == null){
            Log.e(TAG, "No Player Listener, unable to request audio focus");
            return;
        }

        if (!playaListener.requestAudioFocus()){
            Log.e(TAG,"Unable to acquire audio focus, aborting playback");
            playaListener.onPlaystateChanged(new StateTransition(PlayaState.IDLE, Reason.ERROR_FAILED, currentTrackUrn, fromPos, track.get(PlayableProperty.DURATION)));
            return;
        }

        sendSkippyPlayEvent();

        stateHandler.removeMessages(0);
        lastStateChangeProgress = 0;

        final String trackUrl = buildStreamUrl();
        if (trackUrl.equals(currentStreamUrl)) {
            // we are already playing it. seek and resume
            skippy.seek(fromPos);
            skippy.resume();
        } else {
            currentStreamUrl = trackUrl;

            if (uninterrupted){
                skippy.playAd(currentStreamUrl, fromPos);
            } else {
                skippy.play(currentStreamUrl, fromPos);
            }

        }
    }

    private void sendSkippyPlayEvent() {
        // we can get rid of this after 100 percent launch. This is to help determind effectiveness of wakelocks
        final boolean shouldUseLocks = shouldUseLocks();
        ConnectionType currentConnectionType = connectionHelper.getCurrentConnectionType();
        eventBus.publish(EventQueue.TRACKING, new SkippyPlayEvent(currentConnectionType, shouldUseLocks));
    }

    private String buildStreamUrl() {
        checkState(accountOperations.isUserLoggedIn(), "SoundCloud User account does not exist");
        Token token = checkNotNull(accountOperations.getSoundCloudToken(), "The SoundCloud token should not be null");
        return urlBuilder.from(ApiEndpoints.HLS_STREAM, currentTrackUrn)
                .withQueryParam(ApiRequest.Param.OAUTH_TOKEN, token.getAccessToken())
                .build();
    }

    @Override
    public boolean resume() {
        skippy.resume();
        // skippy is always resumeable, where as mediaplayer is not
        return true;
    }

    @Override
    public void pause() {
        skippy.pause();
    }

    @Override
    public long seek(long position, boolean performSeek) {
        if (performSeek) {
            skippy.seek(position);
            if (playaListener != null){
                playaListener.onProgressEvent(position, skippy.getDuration());
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
    public void destroy() {
        skippy.destroy();
    }

    @Override
    public void setListener(PlayaListener playaListener) {
        this.playaListener = playaListener;
        stateHandler.setPlayaListener(playaListener);
    }

    @Override
    public boolean isSeekable() {
        return true;
    }

    @Override
    public boolean isNotSeekablePastBuffer() {
        return false;
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

            final PlayaState translatedState = getTranslatedState(state, reason);
            final Reason translatedReason = getTranslatedReason(reason, errorCode);
            final StateTransition transition = new StateTransition(translatedState, translatedReason, currentTrackUrn, adjustedPosition, duration);
            transition.addExtraAttribute(StateTransition.EXTRA_PLAYBACK_PROTOCOL, getPlaybackProtocol().getValue());
            transition.addExtraAttribute(StateTransition.EXTRA_PLAYER_TYPE, PLAYER_NAME);
            transition.addExtraAttribute(StateTransition.EXTRA_CONNECTION_TYPE, connectionHelper.getCurrentConnectionType().getValue());

            if (transition.playbackHasStopped()){
                currentStreamUrl = null;
            }

            Message msg = stateHandler.obtainMessage(0, transition);
            stateHandler.sendMessage(msg);

            final boolean shouldUseLocks = shouldUseLocks();
            if (shouldUseLocks){
                configureLockBasedOnNewState(transition);
            }

            if (transition.isBuffering() && position > 0){
                ConnectionType currentConnectionType = connectionHelper.getCurrentConnectionType();
                eventBus.publish(EventQueue.TRACKING, new BufferUnderrunEvent(currentConnectionType, shouldUseLocks));
            }
        }
    }

    private boolean shouldUseLocks() {
        return deviceHelper.inSplitTestGroup();
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
        if(!accountOperations.isUserLoggedIn()){
            return;
        }
        eventBus.publish(EventQueue.PLAYBACK_PERFORMANCE, createPerformanceEvent(metric, value, cdnHost));
    }

    private PlayaState getTranslatedState(Skippy.State state, Skippy.Reason reason) {
        switch (state) {
            case IDLE:
                return PlayaState.IDLE;
            case PLAYING:
                return reason == BUFFERING ? PlayaState.BUFFERING : PlayaState.PLAYING;
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
            default:
                throw new IllegalArgumentException("Unexpected performance metric : " + metric);
        }
    }

    @Override
    public void onProgressChange(long position, long duration, String uri) {
        final long adjustedPosition = fixPosition(position, duration);
        if (playaListener != null && uri.equals(currentStreamUrl)){
            playaListener.onProgressEvent(adjustedPosition, duration);
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
    public void onErrorMessage(ErrorCategory category, String sourceFile, int line, String errorMsg, String uri, String cdn) {

        ErrorUtils.handleSilentException(errorMsg, new SkippyException(category, line, sourceFile));

        final PlaybackErrorEvent event = new PlaybackErrorEvent(category.getCategory().name(), getPlaybackProtocol(), cdn);
        eventBus.publish(EventQueue.PLAYBACK_ERROR, event);
    }

    @Override
    public void onInitializationError(Throwable throwable, String message) {
        ErrorUtils.handleSilentException(message, throwable);
    }


    static class StateChangeHandler extends Handler {

        @Nullable
        private PlayaListener playaListener;

        @Inject
        StateChangeHandler(@Named("MainLooper") Looper looper) {
            super(looper);
        }

        public void setPlayaListener(@Nullable PlayaListener playaListener) {
            this.playaListener = playaListener;
        }

        @Override
        public void handleMessage(Message msg) {
            if (playaListener != null) {
                playaListener.onPlaystateChanged((StateTransition) msg.obj);
            }
        }
    }


    private static class SkippyException extends Exception {
        private final ErrorCategory errorCategory;
        private final int line;
        private final String sourceFile;

        private SkippyException(ErrorCategory category, int line, String sourceFile) {
            this.errorCategory = category;
            this.line = line;
            this.sourceFile = sourceFile;
        }

        @Override
        public String getMessage() {
            return errorCategory.getCategory().name();

        }

        @Override
        public StackTraceElement[] getStackTrace() {
            StackTraceElement[] stack = new StackTraceElement[]{new StackTraceElement(errorCategory.getCategory().name(), ScTextUtils.EMPTY_STRING, sourceFile, line)};
            return stack;
        }
    }
}
