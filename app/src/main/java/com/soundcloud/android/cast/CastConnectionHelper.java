package com.soundcloud.android.cast;

import com.soundcloud.android.lightcycle.ActivityLightCycle;
import com.soundcloud.android.model.Urn;

import android.support.v7.app.MediaRouteButton;
import android.view.KeyEvent;
import android.view.Menu;


public interface CastConnectionHelper extends ActivityLightCycle {

    void addMediaRouterButton(Menu menu, int itemId);

    void addMediaRouterButton(MediaRouteButton mediaRouteButton);

    void removeMediaRouterButton(MediaRouteButton mediaRouteButton);

    void reconnectSessionIfPossible();

    boolean onDispatchVolumeEvent(KeyEvent event);

    void addConnectionListener(final CastConnectionListener listener);

    void removeConnectionListener(final CastConnectionListener listener);

    boolean isConnected();

    public static interface CastConnectionListener {
        void onConnectedToReceiverApp();
        void onMetaDataUpdated(Urn currentUrn);
    }
}
