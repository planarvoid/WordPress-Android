package com.soundcloud.android.cast;

import android.view.Menu;

public class UselessCastConnectionHelper implements CastConnectionHelper {

    @Override
    public void addMediaRouterButton(Menu menu, int itemId) {
        // no-op
    }

    @Override
    public void startDeviceDiscovery() {
        // no-op
    }

    @Override
    public void stopDeviceDiscovery() {
        // no-op
    }

    @Override
    public void reconnectSessionIfPossible() {
        // no-op
    }

    @Override
    public void addConnectionListener(CastConnectionListener listener) {
        // no-op
    }

    @Override
    public void removeConnectionListener(CastConnectionListener listener) {
        // no-op
    }
}
