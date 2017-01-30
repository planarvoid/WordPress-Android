package com.soundcloud.android.cast;

import com.soundcloud.java.optional.Optional;
import com.soundcloud.lightcycle.ActivityLightCycle;

import android.content.Context;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.app.MediaRouteButton;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;

public interface CastConnectionHelper extends ActivityLightCycle<AppCompatActivity> {

    interface OnConnectionChangeListener {
        void onCastUnavailable();
        void onCastAvailable();
    }

    void notifyConnectionChange(boolean castAvailable, Optional<String> deviceName);

    void addOnConnectionChangeListener(OnConnectionChangeListener listener);

    void removeOnConnectionChangeListener(OnConnectionChangeListener listener);

    MenuItem addMediaRouterButton(Context context, Menu menu, int itemId);

    void removeMediaRouterButton(Context context, MenuItem castMenu);

    void addMediaRouterButton(MediaRouteButton mediaRouteButton);

    void removeMediaRouterButton(MediaRouteButton mediaRouteButton);

    boolean onDispatchVolumeEvent(KeyEvent event);

    boolean isCasting();

    boolean isCastAvailable();

    String getDeviceName();

}
