package com.soundcloud.android.cast;

import static com.soundcloud.android.cast.CastProtocol.TAG;

import com.google.android.gms.cast.framework.CastSession;
import com.google.android.gms.cast.framework.CastState;
import com.google.android.gms.cast.framework.CastStateListener;
import com.soundcloud.android.PlaybackServiceController;
import com.soundcloud.android.utils.Log;
import com.soundcloud.java.optional.Optional;

import android.support.v7.app.AppCompatActivity;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class DefaultCastSessionController extends SimpleCastSessionManagerListener implements CastStateListener {

    private final PlaybackServiceController serviceController;
    private final DefaultCastPlayer castPlayer;
    private final CastContextWrapper castContext;
    private final CastConnectionHelper castConnectionHelper;
    private CastProtocol castProtocol;

    private Optional<CastSession> currentCastSession;

    @Inject
    public DefaultCastSessionController(PlaybackServiceController serviceController,
                                        DefaultCastPlayer castPlayer,
                                        CastContextWrapper castContext,
                                        CastConnectionHelper castConnectionHelper,
                                        CastProtocol castProtocol) {
        this.serviceController = serviceController;
        this.castPlayer = castPlayer;
        this.castContext = castContext;

        this.currentCastSession = castContext.getCurrentCastSession();
        this.castConnectionHelper = castConnectionHelper;
        this.castProtocol = castProtocol;
    }

    public void onResume(AppCompatActivity activity) {
        castContext.onActivityResumed(activity);
    }

    public void onPause(AppCompatActivity activity) {
        castContext.onActivityPaused(activity);
    }

    public void startListening() {
        castContext.addCastStateListener(this);
        castContext.addSessionManagerListener(this);
    }

    @Override
    public void onSessionStarted(CastSession castSession, String sessionId) {
        Log.d(TAG, "DefaultCastSessionController::onSessionStarted() for id " + sessionId);
        serviceController.stopPlaybackService();

        onSessionUpdated(castSession);
    }

    @Override
    public void onSessionResumed(CastSession castSession, boolean wasSuspended) {
        Log.d(TAG, "DefaultCastSessionController::onSessionResumed() called with: castSession = [" + castSession + "], wasSuspended = [" + wasSuspended + "]");

        onSessionUpdated(castSession);
    }

    private void onSessionUpdated(CastSession castSession) {
        currentCastSession = Optional.of(castSession);
        notifyConnectionChange(true, getDeviceName());

        castProtocol.registerCastSession(castSession);
        castProtocol.setListener(castPlayer);
        castPlayer.onConnected();
    }

    @Override
    public void onSessionStartFailed(CastSession castSession, int error) {
        onApplicationDisconnected();
    }

    @Override
    public void onSessionEnded(CastSession castSession, int error) {
        onApplicationDisconnected();
    }

    private void onApplicationDisconnected() {
        castPlayer.onDisconnected();
        castProtocol.removeListener(castPlayer);
        castProtocol.unregisterCastSession();
    }

    private void notifyConnectionChange(boolean castAvailable, Optional<String> deviceName) {
        castConnectionHelper.notifyConnectionChange(castAvailable, deviceName);
    }

    private Optional<String> getDeviceName() {
        return currentCastSession.isPresent() && currentCastSession.get().getCastDevice() != null ?
               Optional.fromNullable(currentCastSession.get().getCastDevice().getFriendlyName()) :
               Optional.absent();
    }

    @Override
    public void onCastStateChanged(int castState) {
        switch (castState) {
            case CastState.CONNECTED:
                // Notify done in on Session connected
                Log.d(TAG, "DefaultCastSessionController::onCastStateChanged() = CONNECTED");
                break;
            case CastState.NOT_CONNECTED:
                Log.d(TAG, "DefaultCastSessionController::onCastStateChanged() = NOT_CONNECTED");
                notifyConnectionChange(true, Optional.<String>absent());
                break;
            case CastState.CONNECTING:
                Log.d(TAG, "DefaultCastSessionController::onCastStateChanged() = CONNECTING");
                notifyConnectionChange(true, getDeviceName());
                break;
            case CastState.NO_DEVICES_AVAILABLE:
                Log.d(TAG, "DefaultCastSessionController::onCastStateChanged() = NO_DEVICES_AVAILABLE");
                notifyConnectionChange(false, Optional.<String>absent());
                break;
        }
    }

}
