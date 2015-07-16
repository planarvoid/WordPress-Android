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

    public void togglePlayback() {
        context.startService(createExplicitServiceIntent(PlaybackService.Actions.TOGGLEPLAYBACK_ACTION));
    }

    public void resume() {
        context.startService(createExplicitServiceIntent(PlaybackService.Actions.PLAY_ACTION));
    }

    public void pause() {
        context.startService(createExplicitServiceIntent(PlaybackService.Actions.PAUSE_ACTION));
    }

    public void playCurrent() {
        context.startService(createExplicitServiceIntent(PlaybackService.Actions.PLAY_CURRENT));
    }

    public void seek(long position) {
        Intent intent = createExplicitServiceIntent(PlaybackService.Actions.SEEK);
        intent.putExtra(PlaybackService.ActionsExtras.SEEK_POSITION, position);
        context.startService(intent);
    }

    private Intent createExplicitServiceIntent(String action) {
        Intent intent = new Intent(context, PlaybackService.class);
        intent.setAction(action);
        return intent;
    }
}
