package com.soundcloud.android.cast;

import static com.soundcloud.android.cast.api.CastProtocol.TAG;

import com.google.android.gms.cast.framework.CastContext;
import com.google.android.gms.cast.framework.CastSession;
import com.google.android.gms.cast.framework.CastStateListener;
import com.google.android.gms.cast.framework.SessionManagerListener;
import com.soundcloud.android.utils.Log;
import com.soundcloud.java.optional.Optional;

import android.support.v7.app.AppCompatActivity;
import android.view.KeyEvent;

public class DefaultCastContextWrapper implements CastContextWrapper {

    private final CastContext context;

    DefaultCastContextWrapper(CastContext castContext) {
        context = castContext;
    }

    @Override
    public void addSessionManagerListener(SessionManagerListener<CastSession> sessionManagerListener) {
        context.getSessionManager().addSessionManagerListener(sessionManagerListener, CastSession.class);
    }

    @Override
    public void onActivityPaused(AppCompatActivity activity) {
        context.onActivityPaused(activity);
    }

    @Override
    public boolean onDispatchVolumeKeyEventBeforeJellyBean(KeyEvent event) {
        return context.onDispatchVolumeKeyEventBeforeJellyBean(event);
    }

    @Override
    public void onActivityResumed(AppCompatActivity activity) {
        context.onActivityResumed(activity);
    }

    @Override
    public void addCastStateListener(CastStateListener castStateLister) {
        context.addCastStateListener(castStateLister);
    }

    @Override
    public Optional<CastSession> getCurrentCastSession() {
        try {
            return Optional.fromNullable(context.getSessionManager().getCurrentCastSession());
        } catch (RuntimeException ex) {
            // in case the device has no Google Play Services installed or if it's an emulator
            Log.e(TAG, "Unable to get current cast session", ex);
            return Optional.absent();
        }
    }

}
