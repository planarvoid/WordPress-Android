package com.soundcloud.android;

import com.soundcloud.android.playback.PlaybackService;

import android.content.Context;
import android.content.Intent;

import javax.inject.Inject;

public class ServiceInitiator {

    private final Context context;

    @Inject
    public ServiceInitiator(Context context) {
        this.context = context;
    }

    public void stopPlaybackService() {
        context.startService(createExplicitServiceIntent(PlaybackService.Actions.STOP_ACTION));
    }

    public void resetPlaybackService() {
        context.startService(createExplicitServiceIntent(PlaybackService.Actions.RESET_ALL));
    }

    private Intent createExplicitServiceIntent(String action) {
        Intent intent = new Intent(context, PlaybackService.class);
        intent.setAction(action);
        return intent;
    }
}
