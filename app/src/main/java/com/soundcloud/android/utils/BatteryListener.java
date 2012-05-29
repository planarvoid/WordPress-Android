package com.soundcloud.android.utils;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.BatteryManager;

public class BatteryListener {
    private int mBatteryLevel;
    private boolean mPluggedIn;

    private Context mContext;
    private final BroadcastReceiver mBroadcastReceiver;

    public BatteryListener(Context context) {
        mContext = context;
        mBroadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                int rawlevel = intent.getIntExtra("level", -1);
                int scale = intent.getIntExtra("scale", -1);
                int level = -1;
                if (rawlevel >= 0 && scale > 0) {
                    level = (rawlevel * 100) / scale;
                }
                mBatteryLevel = level;
                mPluggedIn = intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0) != 0;
            }
        };
        context.registerReceiver(mBroadcastReceiver, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
    }

    public void stopListening() {
        mContext.unregisterReceiver(mBroadcastReceiver);
    }

    public boolean isOK() {
        return mBatteryLevel > (mPluggedIn ? 25 : 50);
    }
}
