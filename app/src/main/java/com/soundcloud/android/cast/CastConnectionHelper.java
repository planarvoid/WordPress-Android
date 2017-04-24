package com.soundcloud.android.cast;

import com.soundcloud.lightcycle.ActivityLightCycle;

import android.support.v7.app.AppCompatActivity;
import android.view.KeyEvent;

public interface CastConnectionHelper extends ActivityLightCycle<AppCompatActivity> {

    interface OnConnectionChangeListener {
        void onCastUnavailable();

        void onCastAvailable();
    }

    void notifyConnectionChange(boolean sessionConnected, boolean castAvailable);

    void addOnConnectionChangeListener(OnConnectionChangeListener listener);

    void removeOnConnectionChangeListener(OnConnectionChangeListener listener);

    boolean onDispatchVolumeEvent(KeyEvent event);

    boolean isCasting();

    boolean isCastAvailable();

    String getDeviceName();

}
