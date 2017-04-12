package com.soundcloud.android.cast;

import com.google.android.gms.cast.CastStatusCodes;
import com.google.android.gms.cast.framework.CastSession;
import com.google.android.gms.cast.framework.SessionManagerListener;
import com.google.android.gms.common.api.GoogleApiClient;
import com.soundcloud.android.utils.Log;

import android.support.annotation.CallSuper;

public class SimpleCastSessionManagerListener implements SessionManagerListener<CastSession> {

    private static final String TAG = "CastSessionManager";

    @CallSuper
    @Override
    public void onSessionStarting(CastSession castSession) {
        Log.d(TAG, "onSessionStarting() called with: castSession = [" + castSession + "]");
    }

    @CallSuper
    @Override
    public void onSessionStarted(CastSession castSession, String sessionId) {
        Log.d(TAG, "onSessionStarted() called with: castSession = [" + castSession + "], sessionId = [" + sessionId + "]");
    }

    @CallSuper
    @Override
    public void onSessionStartFailed(CastSession castSession, int error) {
        Log.d(TAG, "onSessionStartFailed() called with: castSession = [" + castSession + "], error = [" + error + ": " + CastStatusCodes.getStatusCodeString(error) + "]");
    }

    @CallSuper
    @Override
    public void onSessionEnding(CastSession castSession) {
        Log.d(TAG, "onSessionEnding() called with: castSession = [" + castSession + "]");
    }

    @CallSuper
    @Override
    public void onSessionEnded(CastSession castSession, int error) {
        Log.d(TAG, "onSessionEnded() called with: castSession = [" + castSession + "], error = [" + error + ": " + CastStatusCodes.getStatusCodeString(error) + "]");
    }

    @CallSuper
    @Override
    public void onSessionResuming(CastSession castSession, String sessionId) {
        Log.d(TAG, "onSessionResuming() called with: castSession = [" + castSession + "], sessionId = [" + sessionId + "]");
    }

    @CallSuper
    @Override
    public void onSessionResumed(CastSession castSession, boolean wasSuspended) {
        Log.d(TAG, "onSessionResumed() called with: castSession = [" + castSession + "], wasSuspended = [" + wasSuspended + "]");
    }

    @CallSuper
    @Override
    public void onSessionResumeFailed(CastSession castSession, int error) {
        Log.d(TAG, "onSessionResumeFailed() called with: castSession = [" + castSession + "], error = [" + error + ": " + CastStatusCodes.getStatusCodeString(error) + "]");
    }

    @CallSuper
    @Override
    public void onSessionSuspended(CastSession castSession, int reason) {
        Log.d(TAG, "onSessionSuspended() called with: castSession = [" + castSession + "], reason = [" + reason + ": " + getSuspensionReasonString(reason) + "]");
    }

    private static String getSuspensionReasonString(int reason) {
        switch (reason) {
            case GoogleApiClient.ConnectionCallbacks.CAUSE_SERVICE_DISCONNECTED:
                return "CAUSE_SERVICE_DISCONNECTED";
            case GoogleApiClient.ConnectionCallbacks.CAUSE_NETWORK_LOST:
                return "CAUSE_NETWORK_LOST";
            default:
                return "UNKNOWN REASON";
        }
    }
}
