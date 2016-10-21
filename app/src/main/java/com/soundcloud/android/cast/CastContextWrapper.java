package com.soundcloud.android.cast;

import com.google.android.gms.cast.framework.CastSession;
import com.google.android.gms.cast.framework.CastStateListener;
import com.google.android.gms.cast.framework.SessionManagerListener;
import com.soundcloud.java.optional.Optional;

import android.support.v7.app.AppCompatActivity;

public interface CastContextWrapper {
    Optional<CastSession> getCurrentCastSession();

    void addCastStateListener(CastStateListener castStateListener);

    void onActivityResumed(AppCompatActivity activity);

    void addSessionManagerListener(SessionManagerListener<CastSession> sessionManagerListener);

    void onActivityPaused(AppCompatActivity activity);

    void removeSessionManagerListener(SessionManagerListener<CastSession>  castSessionController);
}
