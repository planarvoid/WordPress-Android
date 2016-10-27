package com.soundcloud.android.cast;

import com.soundcloud.java.optional.Optional;
import com.soundcloud.lightcycle.ActivityLightCycle;

import android.content.Context;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.app.MediaRouteButton;
import android.view.Menu;

public interface CastConnectionHelper extends ActivityLightCycle<AppCompatActivity> {

    interface OnConnectionChangeListener {
        void onCastConnectionChange(String deviceName);
    }

    void notifyConnectionChange(boolean castAvailable, Optional<String> deviceName);

    void addOnConnectionChangeListener(OnConnectionChangeListener listener);

    void removeOnConnectionChangeListener(OnConnectionChangeListener listener);

    @SuppressWarnings("unused")
    void addMediaRouterButton(Context context, Menu menu, int itemId);

    void addMediaRouterButton(MediaRouteButton mediaRouteButton);

    void removeMediaRouterButton(MediaRouteButton mediaRouteButton);

    String getDeviceName();

}
