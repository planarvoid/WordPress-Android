package com.soundcloud.android;

import com.soundcloud.android.gcm.GcmRegistrationService;
import com.soundcloud.android.playback.PlaybackItem;
import com.soundcloud.android.playback.PlaybackService;

import android.content.Context;
import android.content.Intent;
import android.os.Parcelable;

import javax.inject.Inject;

public class ServiceInitiator {

    private final Context context;

    @Inject
    public ServiceInitiator(Context context) {
        this.context = context;
    }

    public void startGcmService(Context context){
        context.startService(new Intent(context, GcmRegistrationService.class));
    }

    public void stopPlaybackService() {
        playerAction(PlaybackService.Action.STOP);
    }

    public void resetPlaybackService() {
        playerAction(PlaybackService.Action.RESET_ALL);
    }

    public void togglePlayback() {
        playerAction(PlaybackService.Action.TOGGLE_PLAYBACK);
    }

    public void resume() {
        playerAction(PlaybackService.Action.RESUME);
    }

    public void pause() {
        playerAction(PlaybackService.Action.PAUSE);
    }

    public void play(PlaybackItem playbackItem) {
        startPlayback(playbackItem, PlaybackService.Action.PLAY);
    }

    private void startPlayback(PlaybackItem playbackItem, String action) {
        Intent intent = new Intent(context, PlaybackService.class);
        intent.setAction(action);
        intent.putExtra(PlaybackService.ActionExtras.PLAYBACK_ITEM, (Parcelable) playbackItem);
        context.startService(intent);
    }

    private void playerAction(String action) {
        context.startService(createExplicitServiceIntent(action));
    }

    public void seek(long position) {
        Intent intent = createExplicitServiceIntent(PlaybackService.Action.SEEK);
        intent.putExtra(PlaybackService.ActionExtras.POSITION, position);
        context.startService(intent);
    }

    private Intent createExplicitServiceIntent(String action) {
        Intent intent = new Intent(context, PlaybackService.class);
        intent.setAction(action);
        return intent;
    }

}
