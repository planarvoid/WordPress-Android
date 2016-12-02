package com.soundcloud.android.cast;

import static com.soundcloud.android.cast.CastProtocol.*;

import com.google.android.gms.cast.framework.CastSession;
import com.google.android.gms.cast.framework.CastState;
import com.google.android.gms.cast.framework.CastStateListener;
import com.google.android.gms.cast.framework.SessionManagerListener;
import com.soundcloud.android.PlaybackServiceController;
import com.soundcloud.android.playback.PlaySessionController;
import com.soundcloud.android.utils.Log;
import com.soundcloud.java.optional.Optional;

import android.support.v7.app.AppCompatActivity;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class DefaultCastSessionController
        implements CastStateListener, SessionManagerListener<CastSession> {

    private final PlaybackServiceController serviceController;
    private final CastPlayer castPlayer;
    private final CastContextWrapper castContext;
    private final PlaySessionController playSessionController;
    private final CastConnectionHelper castConnectionHelper;

    private Optional<CastSession> currentCastSession;

    @Inject
    public DefaultCastSessionController(PlaybackServiceController serviceController,
                                        CastPlayer castPlayer,
                                        CastContextWrapper castContext,
                                        PlaySessionController playSessionController,
                                        CastConnectionHelper castConnectionHelper) {
        this.serviceController = serviceController;
        this.castPlayer = castPlayer;
        this.castContext = castContext;
        this.playSessionController = playSessionController;

        this.currentCastSession = castContext.getCurrentCastSession();
        this.castConnectionHelper = castConnectionHelper;
    }

    public void startListening() {
        castContext.addCastStateListener(this);
        castContext.addSessionManagerListener(this);
    }

    public void onResume(AppCompatActivity activity) {
        castContext.onActivityResumed(activity);
    }

    public void onPause(AppCompatActivity activity) {
        castContext.onActivityPaused(activity);
    }

    @Override
    public void onSessionStarting(CastSession castSession) {
    }

    @Override
    public void onSessionStarted(CastSession castSession, String sessionId) {
        Log.d(TAG, "OnSessionStarted");
        onApplicationConnected(Optional.fromNullable(castSession));
    }

    @Override
    public void onSessionStartFailed(CastSession castSession, int i) {
        onApplicationDisconnected();
    }

    @Override
    public void onSessionEnding(CastSession castSession) {
    }

    @Override
    public void onSessionEnded(CastSession castSession, int i) {
        onApplicationDisconnected();
    }

    @Override
    public void onSessionResuming(CastSession castSession, String sessionId) {
    }

    @Override
    public void onSessionResumed(CastSession castSession, boolean b) {
        Log.d(TAG, "OnSessionResumed");
        currentCastSession = Optional.of(castSession);
        notifyConnectionChange(true, getDeviceName());

        castPlayer.onConnected(castSession.getRemoteMediaClient());
        castPlayer.pullRemotePlayQueueAndUpdateLocalState();
    }

    @Override
    public void onSessionResumeFailed(CastSession castSession, int i) {
    }

    @Override
    public void onSessionSuspended(CastSession castSession, int i) {
    }

    private void onApplicationDisconnected() {
        castPlayer.onDisconnected();
    }

    private void onApplicationConnected(Optional<CastSession> session) {
        currentCastSession = session;
        serviceController.stopPlaybackService();
        castPlayer.onConnected(session.get().getRemoteMediaClient());

        notifyConnectionChange(true, getDeviceName());
        if (playSessionController.isPlayingCurrentPlayQueueItem()) {
            castPlayer.playLocalPlayQueueOnRemote();
        }
    }

    private void notifyConnectionChange(boolean castAvailable, Optional<String> deviceName) {
        castConnectionHelper.notifyConnectionChange(castAvailable, deviceName);
    }

    private Optional<String> getDeviceName() {
        return currentCastSession.isPresent() && currentCastSession.get().getCastDevice() != null ?
               Optional.fromNullable(currentCastSession.get().getCastDevice().getFriendlyName()) :
               Optional.<String>absent();
    }

    @Override
    public void onCastStateChanged(int castState) {
        Log.d(TAG, "Cast state changed: " + castState);
        switch (castState) {
            case CastState.CONNECTED:
                // Notify done in on Session connected
                break;
            case CastState.NOT_CONNECTED:
                notifyConnectionChange(true, Optional.<String>absent());
                break;
            case CastState.CONNECTING:
                notifyConnectionChange(true, getDeviceName());
                break;
            case CastState.NO_DEVICES_AVAILABLE:
                notifyConnectionChange(false, Optional.<String>absent());
                break;
        }
    }
}
