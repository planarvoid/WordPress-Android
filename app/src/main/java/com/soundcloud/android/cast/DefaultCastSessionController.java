package com.soundcloud.android.cast;

import static com.soundcloud.android.cast.CastProtocol.TAG;

import com.google.android.gms.cast.framework.CastSession;
import com.google.android.gms.cast.framework.CastState;
import com.google.android.gms.cast.framework.CastStateListener;
import com.soundcloud.android.PlaybackServiceController;
import com.soundcloud.android.ads.AdsOperations;
import com.soundcloud.android.utils.Log;

import android.support.v7.app.AppCompatActivity;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class DefaultCastSessionController extends SimpleCastSessionManagerListener implements CastStateListener {

    private final PlaybackServiceController serviceController;
    private final AdsOperations adsOperations;
    private final DefaultCastPlayer castPlayer;
    private final CastContextWrapper castContext;
    private final CastConnectionHelper castConnectionHelper;
    private final CastProtocol castProtocol;

    @Inject
    public DefaultCastSessionController(PlaybackServiceController serviceController,
                                        AdsOperations adsOperations, DefaultCastPlayer castPlayer,
                                        CastContextWrapper castContext,
                                        CastConnectionHelper castConnectionHelper,
                                        CastProtocol castProtocol) {
        this.serviceController = serviceController;
        this.adsOperations = adsOperations;
        this.castPlayer = castPlayer;
        this.castContext = castContext;

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
        adsOperations.clearAllAdsFromQueue();

        onSessionUpdated(castSession);
    }

    @Override
    public void onSessionResumed(CastSession castSession, boolean wasSuspended) {
        Log.d(TAG, "DefaultCastSessionController::onSessionResumed() called with: castSession = [" + castSession + "], wasSuspended = [" + wasSuspended + "]");

        onSessionUpdated(castSession);
    }

    private void onSessionUpdated(CastSession castSession) {
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

    @Override
    public void onCastStateChanged(int castState) {
        final boolean sessionConnected = castState == CastState.CONNECTED;
        final boolean castAvailable = castState != CastState.NO_DEVICES_AVAILABLE;
        Log.d(TAG, "DefaultCastSessionController::notifyConnectionChange() for " + CastState.toString(castState) + " with: " +
                "sessionConnected = [" + sessionConnected + "], castAvailable = [" + castAvailable + "]");
        castConnectionHelper.notifyConnectionChange(sessionConnected, castAvailable);
    }
}
