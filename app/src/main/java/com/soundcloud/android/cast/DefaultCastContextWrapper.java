package com.soundcloud.android.cast;

import com.google.android.gms.cast.framework.CastContext;
import com.google.android.gms.cast.framework.CastSession;
import com.google.android.gms.cast.framework.CastStateListener;
import com.google.android.gms.cast.framework.SessionManagerListener;
import com.soundcloud.java.optional.Optional;

import android.support.v7.app.AppCompatActivity;

public class DefaultCastContextWrapper implements CastContextWrapper {

    private final CastContext context;

    public DefaultCastContextWrapper(CastContext castContext) {
        context = castContext;
    }

    public void removeSessionManagerListener(SessionManagerListener<CastSession> sessionManagerListener) {
        context.getSessionManager().removeSessionManagerListener(sessionManagerListener, CastSession.class);
    }

    public void addSessionManagerListener(SessionManagerListener<CastSession> sessionManagerListener) {
        context.getSessionManager().addSessionManagerListener(sessionManagerListener, CastSession.class);
    }

    public void onActivityPaused(AppCompatActivity activity) {
        context.onActivityPaused(activity);
    }

    public void onActivityResumed(AppCompatActivity activity) {
        context.onActivityResumed(activity);
    }

    public void addCastStateListener(CastStateListener castStateLister) {
        context.addCastStateListener(castStateLister);
    }

    public Optional<CastSession> getCurrentCastSession() {
        return Optional.fromNullable(context.getSessionManager().getCurrentCastSession());
    }

}
