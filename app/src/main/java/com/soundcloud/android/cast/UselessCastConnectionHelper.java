package com.soundcloud.android.cast;

import android.app.Activity;
import android.view.KeyEvent;
import android.view.Menu;

public class UselessCastConnectionHelper implements CastConnectionHelper {

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
    public void onActivityResume(Activity activity) {
        // no-op
    }

    @Override
    public void onActivityPause() {
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
    public void addConnectionListener(CastConnectionListener listener) {
        // no-op
    }

    @Override
    public void removeConnectionListener(CastConnectionListener listener) {
        // no-op
    }

    @Override
    public boolean isConnected() {
        return false;
    }
}
