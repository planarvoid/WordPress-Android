package com.soundcloud.android.playback.external;

import com.soundcloud.android.SoundCloudApplication;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import javax.inject.Inject;

public class PlaybackActionReceiver extends BroadcastReceiver {
    public static final String EXTRA_EVENT_SOURCE = "play_event_source";
    public static final String SOURCE_REMOTE = "notification";
    public static final String SOURCE_WIDGET = "widget";

    @Inject PlaybackActionController controller;

    @Override
    public void onReceive(Context context, Intent intent) {
        SoundCloudApplication.getObjectGraph().inject(this);

        final String source = intent.getStringExtra(EXTRA_EVENT_SOURCE);
        controller.handleAction(intent.getAction(), source);
    }
}
