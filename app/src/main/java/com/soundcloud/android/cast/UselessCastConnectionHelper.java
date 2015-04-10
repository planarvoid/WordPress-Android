package com.soundcloud.android.cast;

import com.soundcloud.android.lightcycle.DefaultLightCycleActivity;

import android.support.v7.app.ActionBarActivity;
import android.view.KeyEvent;
import android.view.Menu;

public class UselessCastConnectionHelper extends DefaultLightCycleActivity<ActionBarActivity> implements CastConnectionHelper  {

    @Override
    public void addMediaRouterButton(Menu menu, int itemId) {
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
    public void reconnectSessionIfPossible() {
        // no-op
    }

    @Override
    public boolean onDispatchVolumeEvent(KeyEvent event) {
        return false;
    }

    @Override
    public boolean isConnected() {
        return false;
    }
}
