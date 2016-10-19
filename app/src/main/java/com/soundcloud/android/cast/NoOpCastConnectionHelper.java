package com.soundcloud.android.cast;

import com.soundcloud.java.optional.Optional;
import com.soundcloud.java.strings.Strings;
import com.soundcloud.lightcycle.DefaultActivityLightCycle;

import android.content.Context;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.app.MediaRouteButton;
import android.view.Menu;

public class NoOpCastConnectionHelper extends DefaultActivityLightCycle<AppCompatActivity>
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
    public void addMediaRouterButton(MediaRouteButton mediaRouteButton) {
        // no-op
    }

    @Override
    public void removeMediaRouterButton(MediaRouteButton mediaRouteButton) {
        // no-op
    }

    @Override
    public String getDeviceName() {
        return Strings.EMPTY;
    }
}
