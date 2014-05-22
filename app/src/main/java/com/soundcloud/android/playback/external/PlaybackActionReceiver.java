package com.soundcloud.android.playback.external;

import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.events.PlayControlEvent;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import javax.inject.Inject;

public class PlaybackActionReceiver extends BroadcastReceiver {
    @Inject
    PlaybackActionController controller;

    @Override
    public void onReceive(Context context, Intent intent) {
        SoundCloudApplication.getObjectGraph().inject(this);

        final String source = intent.getStringExtra(PlayControlEvent.EXTRA_EVENT_SOURCE);
        controller.handleAction(context, intent.getAction(), source);
    }
}
