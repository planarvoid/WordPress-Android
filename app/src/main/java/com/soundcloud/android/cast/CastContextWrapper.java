package com.soundcloud.android.cast;

import com.google.android.gms.cast.framework.CastSession;
import com.google.android.gms.cast.framework.CastStateListener;
import com.google.android.gms.cast.framework.SessionManagerListener;
import com.soundcloud.java.optional.Optional;

import android.view.KeyEvent;

public interface CastContextWrapper {
    Optional<CastSession> getCurrentCastSession();

    void addCastStateListener(CastStateListener castStateListener);

    void addSessionManagerListener(SessionManagerListener<CastSession> sessionManagerListener);

    boolean onDispatchVolumeKeyEventBeforeJellyBean(KeyEvent event);
}
