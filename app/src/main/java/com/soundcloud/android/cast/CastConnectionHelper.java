package com.soundcloud.android.cast;

import com.soundcloud.lightcycle.ActivityLightCycle;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.app.MediaRouteButton;
import android.view.KeyEvent;
import android.view.Menu;

public interface CastConnectionHelper extends ActivityLightCycle<ActionBarActivity> {

    interface OnConnectionChangeListener {
        void onCastConnectionChange();
    }

    void addOnConnectionChangeListener(OnConnectionChangeListener listener);

    void removeOnConnectionChangeListener(OnConnectionChangeListener listener);

    void addMediaRouterButton(Menu menu, int itemId);

    void addMediaRouterButton(MediaRouteButton mediaRouteButton);

    void removeMediaRouterButton(MediaRouteButton mediaRouteButton);

    void reconnectSessionIfPossible();

    boolean onDispatchVolumeEvent(KeyEvent event);

    boolean isConnected();

    String getDeviceName();

}
