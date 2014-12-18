package com.soundcloud.android.playback;

import com.soundcloud.android.playback.service.PlaybackService;

import android.content.Context;
import android.content.Intent;

public class DefaultPlaybackStrategy implements PlaybackStrategy {

    private final Context context;

    public DefaultPlaybackStrategy(Context context) {
        this.context = context;
    }

    @Override
    public void togglePlayback() {
        context.startService(createExplicitServiceIntent(PlaybackService.Actions.TOGGLEPLAYBACK_ACTION));
    }

    @Override
    public void resume() {
        context.startService(createExplicitServiceIntent(PlaybackService.Actions.PLAY_ACTION));
    }

    @Override
    public void pause() {
        context.startService(createExplicitServiceIntent(PlaybackService.Actions.PAUSE_ACTION));
    }

    @Override
    public void playCurrent() {
        context.startService(createExplicitServiceIntent(PlaybackService.Actions.PLAY_CURRENT));
    }

    @Override
    public void playCurrent(long fromPosition) {
        throw new IllegalStateException("Playing current track from position not yet supported when not casting");
    }

    @Override
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
