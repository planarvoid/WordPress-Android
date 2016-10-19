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
public class CastSessionController implements CastStateListener, SessionManagerListener<CastSession> {

    private final PlaybackServiceController serviceController;
    private final CastPlayer castPlayer;
    private final CastContextWrapper castContext;
    private final PlaySessionController playSessionController;

    private Optional<CastConnectionHelper> castConnectionHelper;
    private Optional<CastSession> currentCastSession;

    @Inject
    public CastSessionController(PlaybackServiceController serviceController,
                                 CastPlayer castPlayer,
                                 CastContextWrapper castContext,
                                 PlaySessionController playSessionController) {
        this.serviceController = serviceController;
        this.castPlayer = castPlayer;
        this.castContext = castContext;
        this.playSessionController = playSessionController;

        this.currentCastSession = castContext.getCurrentCastSession();
        this.castConnectionHelper = Optional.absent();
    }

    public void startListening() {
        castContext.addCastStateListener(this);
    }

    public void onResume(AppCompatActivity activity) {
        castContext.onActivityResumed(activity);
        castContext.addSessionManagerListener(this);
    }

    public void onPause(AppCompatActivity activity) {
        castContext.onActivityPaused(activity);
        castContext.removeSessionManagerListener(this);
    }

    void setCastConnectionListener(CastConnectionHelper castConnectionHelper) {
        this.castConnectionHelper = Optional.of(castConnectionHelper);
    }

    @Override
    public void onSessionStarting(CastSession castSession) {
    }

    @Override
    public void onSessionStarted(CastSession castSession, String sessionId) {
        onApplicationConnected(Optional.fromNullable(castSession));
    }

    @Override
    public void onSessionStartFailed(CastSession castSession, int i) {
        castPlayer.onDisconnected();
    }

    @Override
    public void onSessionEnding(CastSession castSession) {
    }

    @Override
    public void onSessionEnded(CastSession castSession, int i) {
        castPlayer.onDisconnected();
    }

    @Override
    public void onSessionResuming(CastSession castSession, String sessionId) {
    }

    @Override
    public void onSessionResumed(CastSession castSession, boolean b) {
        castPlayer.onConnected(castSession.getRemoteMediaClient());
        castPlayer.updateLocalPlayQueueAndPlayState();
    }

    @Override
    public void onSessionResumeFailed(CastSession castSession, int i) {
    }

    @Override
    public void onSessionSuspended(CastSession castSession, int i) {
    }

    private void onApplicationConnected(Optional<CastSession> session) {
        currentCastSession = session;
        serviceController.stopPlaybackService();
        castPlayer.onConnected(session.get().getRemoteMediaClient());
        notifyConnectionChange(true, Optional.of(session.get().getCastDevice().getFriendlyName()));

        if (playSessionController.isPlayingCurrentPlayQueueItem()) {
            castPlayer.playLocalPlayQueueOnRemote();
        }
    }

    private void notifyConnectionChange(boolean castAvailable, Optional<String> deviceName) {
        if (castConnectionHelper.isPresent()) {
            castConnectionHelper.get().notifyConnectionChange(castAvailable, deviceName);
        }
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
                onApplicationConnected(castContext.getCurrentCastSession());
                break;
            case CastState.NOT_CONNECTED:
            case CastState.CONNECTING:
                notifyConnectionChange(true, getDeviceName());
                break;
            case CastState.NO_DEVICES_AVAILABLE:
                notifyConnectionChange(false, Optional.<String>absent());
                break;
        }
    }

    public boolean isCasting() {
        return currentCastSession.isPresent() && currentCastSession.get().isConnected();
    }

}
