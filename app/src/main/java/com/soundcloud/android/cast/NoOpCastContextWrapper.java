package com.soundcloud.android.cast;

import com.google.android.gms.cast.framework.CastSession;
import com.google.android.gms.cast.framework.CastStateListener;
import com.google.android.gms.cast.framework.SessionManagerListener;
import com.soundcloud.java.optional.Optional;

import android.support.v7.app.AppCompatActivity;
import android.view.KeyEvent;

class NoOpCastContextWrapper implements CastContextWrapper {

    @Override
    public Optional<CastSession> getCurrentCastSession() {
        return Optional.absent();
    }

    @Override
    public void addCastStateListener(CastStateListener castStateListener) {
        // no - op
    }

    @Override
    public void onActivityResumed(AppCompatActivity activity) {
        // no - op
    }

    @Override
    public void addSessionManagerListener(SessionManagerListener sessionManagerListener) {
        // no - op
    }

    @Override
    public void onActivityPaused(AppCompatActivity activity) {
        // no - op
    }

    @Override
    public boolean onDispatchVolumeKeyEventBeforeJellyBean(KeyEvent event) {
        return false;
    }

}
