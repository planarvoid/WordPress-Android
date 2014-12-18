package com.soundcloud.android.cast;

import com.soundcloud.android.model.Urn;

import android.app.Activity;
import android.support.v7.app.MediaRouteButton;
import android.view.KeyEvent;
import android.view.Menu;


public interface CastConnectionHelper {

    void addMediaRouterButton(Menu menu, int itemId);

    void addMediaRouterButton(MediaRouteButton mediaRouteButton);

    void removeMediaRouterButton(MediaRouteButton mediaRouteButton);

    void onActivityResume(Activity activity);

    void onActivityPause();

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
