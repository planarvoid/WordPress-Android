package com.soundcloud.android.cast;

import com.google.android.gms.cast.framework.CastSession;
import com.google.android.gms.cast.framework.SessionManagerListener;

public class SimpleCastSessionManagerListener implements SessionManagerListener<CastSession> {

    @Override
    public void onSessionStarting(CastSession castSession) {
        // default as no-op
    }

    @Override
    public void onSessionStarted(CastSession castSession, String sessionId) {
        // default as no-op
    }

    @Override
    public void onSessionStartFailed(CastSession castSession, int error) {
        // default as no-op
    }

    @Override
    public void onSessionEnding(CastSession castSession) {
        // default as no-op
    }

    @Override
    public void onSessionEnded(CastSession castSession, int error) {
        // default as no-op
    }

    @Override
    public void onSessionResuming(CastSession castSession, String sessionId) {
        // default as no-op
    }

    @Override
    public void onSessionResumed(CastSession castSession, boolean wasSuspended) {
        // default as no-op
    }

    @Override
    public void onSessionResumeFailed(CastSession castSession, int error) {
        // default as no-op
    }

    @Override
    public void onSessionSuspended(CastSession castSession, int reason) {
        // default as no-op
    }
}
