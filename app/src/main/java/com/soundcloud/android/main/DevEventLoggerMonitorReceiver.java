package com.soundcloud.android.main;

import com.soundcloud.android.SoundCloudApplication;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import javax.inject.Inject;

public class DevEventLoggerMonitorReceiver extends BroadcastReceiver {
    public static final String EXTRA_MONITOR_MUTE = DevEventLoggerMonitorReceiver.class.getSimpleName() + "extra_monitor_mute";

    @Inject DevEventLoggerMonitorNotificationController controller;

    @Override
    public void onReceive(Context context, Intent intent) {
        SoundCloudApplication.getObjectGraph().inject(this);
        toggleMonitorMute(intent);
    }

    private void toggleMonitorMute(Intent intent) {
        final boolean toggled = !intent.getBooleanExtra(EXTRA_MONITOR_MUTE, true);
        controller.setMonitorMuteAction(toggled);
    }
}
