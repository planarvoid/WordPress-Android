package com.soundcloud.android.cast;

import com.soundcloud.java.optional.Optional;
import com.soundcloud.java.strings.Strings;
import com.soundcloud.lightcycle.DefaultActivityLightCycle;

import android.content.Context;
import android.support.v7.app.AppCompatActivity;
import android.view.KeyEvent;
import android.view.Menu;

class NoOpCastConnectionHelper extends DefaultActivityLightCycle<AppCompatActivity>
        implements CastConnectionHelper {

    @Override
    public void notifyConnectionChange(boolean castAvailable, Optional<String> deviceName) {
        // no-op
    }

    @Override
    public void addOnConnectionChangeListener(OnConnectionChangeListener listener) {
        // no-op
    }

    @Override
    public void removeOnConnectionChangeListener(OnConnectionChangeListener listener) {
        // no-op
    }

    @Override
    public void addMediaRouterButton(Context context, Menu menu, int itemId) {
        // no-op
    }

    @Override
    public void addMediaRouterButton(android.support.v7.app.MediaRouteButton mediaRouteButton) {
        // no-op
    }

    @Override
    public void removeMediaRouterButton(android.support.v7.app.MediaRouteButton mediaRouteButton) {
        // no-op
    }

    @Override
    public boolean onDispatchVolumeEvent(KeyEvent event) {
        return false;
    }

    @Override
    public boolean isCasting() {
        return false;
    }

    @Override
    public String getDeviceName() {
        return Strings.EMPTY;
    }

}
