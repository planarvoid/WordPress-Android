package com.soundcloud.android.utils;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.BatteryManager;

public class BatteryListener {

    private int batteryLevel;
    private boolean isPluggedIn;

    private Context context;
    private final BroadcastReceiver broadcastReceiver;

    public BatteryListener(Context context) {
        this.context = context;
        broadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                int rawlevel = intent.getIntExtra("level", -1);
                int scale = intent.getIntExtra("scale", -1);
                int level = -1;
                if (rawlevel >= 0 && scale > 0) {
                    level = (rawlevel * 100) / scale;
                }
                batteryLevel = level;
                isPluggedIn = intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0) != 0;
            }
        };
        context.registerReceiver(broadcastReceiver, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
    }

    public void stopListening() {
        context.unregisterReceiver(broadcastReceiver);
    }

    public boolean isOK() {
        return batteryLevel > (isPluggedIn ? 25 : 50);
    }
}
