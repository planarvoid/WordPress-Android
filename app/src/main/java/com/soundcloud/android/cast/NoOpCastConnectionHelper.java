package com.soundcloud.android.cast;

import com.soundcloud.lightcycle.DefaultLightCycleActivity;
import com.soundcloud.android.utils.ScTextUtils;

import android.support.v7.app.AppCompatActivity;
import android.view.KeyEvent;
import android.view.Menu;

public class NoOpCastConnectionHelper extends DefaultLightCycleActivity<AppCompatActivity> implements CastConnectionHelper  {

    @Override
    public void addOnConnectionChangeListener(OnConnectionChangeListener listener) {
        // no-op
    }

    @Override
    public void removeOnConnectionChangeListener(OnConnectionChangeListener listener) {
        // no-op
    }

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
    public boolean isCasting() {
        return false;
    }

    @Override
    public String getDeviceName() {
        return ScTextUtils.EMPTY_STRING;
    }

}
