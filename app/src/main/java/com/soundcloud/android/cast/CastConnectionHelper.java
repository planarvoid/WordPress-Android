package com.soundcloud.android.cast;

import com.soundcloud.android.lightcycle.ActivityLightCycle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.app.MediaRouteButton;
import android.view.KeyEvent;
import android.view.Menu;

public interface CastConnectionHelper extends ActivityLightCycle<AppCompatActivity> {

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
