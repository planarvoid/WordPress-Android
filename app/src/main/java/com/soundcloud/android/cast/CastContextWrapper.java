package com.soundcloud.android.cast;

import com.google.android.gms.cast.framework.CastContext;
import com.google.android.gms.cast.framework.CastSession;
import com.google.android.gms.cast.framework.CastStateListener;
import com.google.android.gms.cast.framework.SessionManagerListener;
import com.soundcloud.java.optional.Optional;

import android.support.v7.app.AppCompatActivity;

public class CastContextWrapper {

    private final CastContext context;

    public CastContextWrapper(CastContext castContext) {
        context = castContext;
    }

    void removeSessionManagerListener(SessionManagerListener<CastSession> sessionManagerListener) {
        context.getSessionManager().removeSessionManagerListener(sessionManagerListener, CastSession.class);
    }

    void addSessionManagerListener(SessionManagerListener<CastSession> sessionManagerListener) {
        context.getSessionManager().addSessionManagerListener(sessionManagerListener, CastSession.class);
    }

    void onActivityPaused(AppCompatActivity activity) {
        context.onActivityPaused(activity);
    }

    void onActivityResumed(AppCompatActivity activity) {
        context.onActivityResumed(activity);
    }

    void addCastStateListener(CastStateListener castStateLister) {
        context.addCastStateListener(castStateLister);
    }

    Optional<CastSession> getCurrentCastSession() {
        return Optional.fromNullable(context.getSessionManager().getCurrentCastSession());
    }

}
