package com.soundcloud.android.cast;

import com.google.android.gms.cast.framework.CastSession;
import com.soundcloud.java.optional.Optional;
import com.soundcloud.java.strings.Strings;
import com.soundcloud.lightcycle.DefaultActivityLightCycle;

import android.content.Context;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.app.MediaRouteButton;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.HashSet;
import java.util.Set;

@Singleton
class DefaultCastConnectionHelper extends DefaultActivityLightCycle<AppCompatActivity> implements CastConnectionHelper {

    private final Set<OnConnectionChangeListener> connectionChangeListeners;
    private final CastContextWrapper castContextWrapper;
    private final DefaultCastButtonInstaller castButtonInstaller;

    private boolean sessionConnected;
    private boolean isCastDeviceAvailable;

    @Inject
    DefaultCastConnectionHelper(CastContextWrapper castContextWrapper, DefaultCastButtonInstaller castButtonInstaller) {
        this.castContextWrapper = castContextWrapper;
        this.castButtonInstaller = castButtonInstaller;
        this.connectionChangeListeners = new HashSet<>();
    }

    @Override
    public void addOnConnectionChangeListener(OnConnectionChangeListener listener) {
        connectionChangeListeners.add(listener);
        notifyListener(listener);
    }

    @Override
    public void removeOnConnectionChangeListener(OnConnectionChangeListener listener) {
        connectionChangeListeners.remove(listener);
    }

    @Override
    public void notifyConnectionChange(boolean sessionConnected, boolean castAvailable) {
        this.sessionConnected = sessionConnected;
        this.isCastDeviceAvailable = castAvailable;
        notifyListeners();
    }

    private void notifyListeners() {
        for (OnConnectionChangeListener listener : connectionChangeListeners) {
            notifyListener(listener);
        }
    }

    private void notifyListener(OnConnectionChangeListener listener) {
        if (isCastAvailable()) {
            listener.onCastAvailable();
        } else {
            listener.onCastUnavailable();
        }
    }

    @Override
    public Optional<MenuItem> addMediaRouterButton(Context context, Menu menu, int itemId) {
        return castButtonInstaller.addMediaRouterButton(context, menu, itemId);
    }

    @Override
    public void removeMediaRouterButton(Context context, MenuItem castMenu) {
        // no-op
    }

    @Override
    public void addMediaRouterButton(MediaRouteButton mediaRouteButton) {
        castButtonInstaller.addMediaRouterButton(mediaRouteButton);
    }

    @Override
    public void removeMediaRouterButton(MediaRouteButton mediaRouteButton) {
        // no-op
    }

    @Override
    public boolean onDispatchVolumeEvent(KeyEvent event) {
        return castContextWrapper.onDispatchVolumeKeyEventBeforeJellyBean(event);
    }

    @Override
    public boolean isCasting() {
        return sessionConnected;
    }

    @Override
    public boolean isCastAvailable() {
        return isCastDeviceAvailable;
    }

    @Override
    public String getDeviceName() {
        Optional<CastSession> castSession = castContextWrapper.getCurrentCastSession();
        if (castSession.isPresent() && castSession.get().getCastDevice() != null) {
            return Optional.fromNullable(castSession.get().getCastDevice().getFriendlyName()).or(Strings.EMPTY);
        } else {
            return Strings.EMPTY;
        }
    }
}

