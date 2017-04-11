package com.soundcloud.android.cast;

import com.soundcloud.java.optional.Optional;
import com.soundcloud.java.strings.Strings;
import com.soundcloud.lightcycle.DefaultActivityLightCycle;

import android.content.Context;
import android.support.v7.app.AppCompatActivity;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;

class NoOpCastConnectionHelper extends DefaultActivityLightCycle<AppCompatActivity> implements CastConnectionHelper {

    @Override
    public void notifyConnectionChange(boolean sessionConnected, boolean castAvailable) {
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
    public Optional<MenuItem> addMediaRouterButton(Context context, Menu menu, int itemId) {
        return Optional.absent();
    }

    @Override
    public void addMediaRouterButton(android.support.v7.app.MediaRouteButton mediaRouteButton) {
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
    public boolean isCastAvailable() {
        return false;
    }

    @Override
    public String getDeviceName() {
        return Strings.EMPTY;
    }

}
