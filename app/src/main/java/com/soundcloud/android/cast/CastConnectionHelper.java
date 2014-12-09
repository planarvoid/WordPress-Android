package com.soundcloud.android.cast;

import android.view.Menu;

public interface CastConnectionHelper {
    void addMediaRouterButton(Menu menu, int itemId);

    void startDeviceDiscovery();

    void stopDeviceDiscovery();

    void reconnectSessionIfPossible();

    void addConnectionListener(final CastConnectionListener listener);

    void removeConnectionListener(final CastConnectionListener listener);

    public static interface CastConnectionListener {
        void onConnectedToReceiverApp();
    }
}
