package com.soundcloud.android.playback.service.skippy;

import static com.soundcloud.android.events.PlaybackPerformanceEvent.ConnectionType;
import static com.soundcloud.android.events.PlaybackPerformanceEvent.PlayerType;
import static com.soundcloud.android.skippy.Skippy.PlaybackMetric;
import static com.soundcloud.android.skippy.Skippy.Reason.BUFFERING;
import static com.soundcloud.android.skippy.Skippy.Reason.COMPLETE;
import static com.soundcloud.android.skippy.Skippy.Reason.ERROR;

import com.google.common.annotations.VisibleForTesting;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.accounts.AccountOperations;
import com.soundcloud.android.rx.eventbus.EventBus;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.PlaybackErrorEvent;
import com.soundcloud.android.events.PlaybackPerformanceEvent;
import com.soundcloud.android.model.Track;
import com.soundcloud.android.model.TrackUrn;
import com.soundcloud.android.model.UserUrn;
import com.soundcloud.android.playback.PlaybackProtocol;
import com.soundcloud.android.playback.service.Playa;
import com.soundcloud.android.playback.service.PlaybackServiceOperations;
import com.soundcloud.android.properties.ApplicationProperties;
import com.soundcloud.android.rx.observers.DefaultSubscriber;
import com.soundcloud.android.skippy.Skippy;
import com.soundcloud.android.utils.Log;
import com.soundcloud.android.utils.NetworkConnectionHelper;
import org.jetbrains.annotations.Nullable;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;

import javax.inject.Inject;
import javax.inject.Named;


public class SkippyAdapter implements Playa, Skippy.PlayListener {

    private static final String TAG = "SkippyAdapter";
    private static final String DEBUG_EXTRA = "Experimental Player";

    private final EventBus eventBus;
    private final Skippy skippy;
    private final AccountOperations accountOperations;
    private final StateChangeHandler stateHandler;
    private final PlaybackServiceOperations playbackOperations;
    private final NetworkConnectionHelper connectionHelper;
    private final ApplicationProperties applicationProperties;

    private volatile String currentStreamUrl;
    private PlayaListener playaListener;
    private long lastStateChangeProgress;

    @Inject
    SkippyAdapter(SkippyFactory skippyFactory, AccountOperations accountOperations, PlaybackServiceOperations playbackOperations,
                  StateChangeHandler stateChangeHandler, EventBus eventBus, NetworkConnectionHelper connectionHelper,
                  ApplicationProperties applicationProperties) {
        skippy = skippyFactory.create(this);
        this.accountOperations = accountOperations;
        this.playbackOperations = playbackOperations;
        stateHandler = stateChangeHandler;
        this.eventBus = eventBus;
        this.connectionHelper = connectionHelper;
        this.applicationProperties = applicationProperties;
    }

    public boolean init(Context context) {
        return skippy.init(context);
    }

    @Override
    public void play(Track track) {
        play(track, 0);
    }

    @Override
    public void play(Track track, long fromPos) {
        if (!accountOperations.isUserLoggedIn()) {
            throw new IllegalStateException("Cannot play a track if no soundcloud account exists");
        }

        // TODO : move audiofocus requesting into PlaybackService when we kill MediaPlayer
        if (playaListener == null){
            Log.e(TAG,"No Player Listener, unable to request audio focus");
            return;
        }

        if (!playaListener.requestAudioFocus()){
            Log.e(TAG,"Unable to acquire audio focus, aborting playback");
            playaListener.onPlaystateChanged(new StateTransition(PlayaState.IDLE, Reason.ERROR_FAILED, fromPos, track.duration));
            return;
        }

        stateHandler.removeMessages(0);
        lastStateChangeProgress = 0;

        final String trackUrl = playbackOperations.buildHLSUrlForTrack(track);
        if (trackUrl.equals(currentStreamUrl)) {
            // we are already playing it. seek and resume
            skippy.seek(fromPos);
            skippy.resume();
        } else {
            logPlayCount(track);

            currentStreamUrl = trackUrl;
            skippy.play(currentStreamUrl, fromPos);
        }
    }

    protected void logPlayCount(Track track) {
        playbackOperations.logPlay(track.getUrn()).subscribe(new DefaultSubscriber<TrackUrn>() {
            @Override
            public void onNext(TrackUrn trackUrn) {
                Log.d(TAG, "Play count logged successfully for track " + trackUrn);
            }
        });
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
    public void onStateChanged(Skippy.State state, Skippy.Reason reason, Skippy.Error errorcode, long position, long duration, String uri) {
        Log.i(TAG, "State = " + state + " : " + reason + " : " + errorcode);

        if (uri.equals(currentStreamUrl)) {
            lastStateChangeProgress = position;

            final PlayaState translatedState = getTranslatedState(state, reason);
            final Reason translatedReason = getTranslatedReason(reason, errorcode);
            final StateTransition transition = new StateTransition(translatedState, translatedReason, position, duration);

            if (transition.playbackHasStopped()){
                currentStreamUrl = null;
            }

            if (!applicationProperties.isReleaseBuild()){
                transition.setDebugExtra(DEBUG_EXTRA);
            }

            Message msg = stateHandler.obtainMessage(0, transition);
            stateHandler.sendMessage(msg);
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

    @Nullable
    private PlaybackPerformanceEvent createPerformanceEvent(PlaybackMetric metric, long value, String cdnHost) {
        ConnectionType currentConnectionType = connectionHelper.getCurrentConnectionType();
        UserUrn userUrn = accountOperations.getLoggedInUserUrn();
        switch (metric) {
            case TIME_TO_PLAY:
                return PlaybackPerformanceEvent.timeToPlay(value, PlaybackProtocol.HLS, PlayerType.SKIPPY, currentConnectionType, cdnHost,
                        userUrn);
            case TIME_TO_BUFFER:
                return PlaybackPerformanceEvent.timeToBuffer(value, PlaybackProtocol.HLS, PlayerType.SKIPPY, currentConnectionType, cdnHost,
                        userUrn);
            case TIME_TO_GET_PLAYLIST:
                return PlaybackPerformanceEvent.timeToPlaylist(value, PlaybackProtocol.HLS, PlayerType.SKIPPY, currentConnectionType, cdnHost,
                        userUrn);
            case TIME_TO_SEEK:
                return PlaybackPerformanceEvent.timeToSeek(value, PlaybackProtocol.HLS, PlayerType.SKIPPY, currentConnectionType, cdnHost,
                        userUrn);
            case FRAGMENT_DOWNLOAD_RATE:
                return PlaybackPerformanceEvent.fragmentDownloadRate(value, PlaybackProtocol.HLS, PlayerType.SKIPPY, currentConnectionType, cdnHost,
                        userUrn);
            default:
                throw new IllegalArgumentException("Unexpected performance metric : " + metric);
        }
    }

    @Override
    public void onProgressChange(long position, long duration, String uri) {
        if (playaListener != null && uri.equals(currentStreamUrl)){
            playaListener.onProgressEvent(position, duration);
        }
    }

    @Override
    public void onErrorMessage(String category, String errorMsg, String uri, String cdn) {
        SoundCloudApplication.handleSilentException(errorMsg, new SkippyException(category));

        final PlaybackErrorEvent event = new PlaybackErrorEvent(category, PlaybackProtocol.HLS, cdn);
        eventBus.publish(EventQueue.PLAYBACK_ERROR, event);
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

    @VisibleForTesting
    static class SkippyFactory {

        @Inject
        SkippyFactory() {
        }

        public Skippy create(Skippy.PlayListener listener) {
            return new Skippy(listener);
        }
    }


    private class SkippyException extends Exception {
        private String category;

        public SkippyException(String category) {
            this.category = category;
        }

        @Override
        public String getMessage() {
            return this.category;

        }

        @Override
        public StackTraceElement[] getStackTrace() {
            StackTraceElement[] stack = new StackTraceElement[]{new StackTraceElement(this.category.replace("/", "."), "", "skippy.c", 1)};
            return stack;
        }
    }
}
