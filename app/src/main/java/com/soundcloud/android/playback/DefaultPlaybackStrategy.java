package com.soundcloud.android.playback;

import static com.soundcloud.android.playback.PlaybackUtils.correctStartPositionAndDeduplicateList;

import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playback.service.PlayQueue;
import com.soundcloud.android.playback.service.PlayQueueManager;
import com.soundcloud.android.playback.service.PlaySessionSource;
import com.soundcloud.android.playback.service.PlaybackService;

import android.content.Context;
import android.content.Intent;

import java.util.List;

public class DefaultPlaybackStrategy implements PlaybackStrategy {

    private final Context context;
    private final PlayQueueManager playQueueManager;

    public DefaultPlaybackStrategy(Context context,
                                   PlayQueueManager playQueueManager) {
        this.context = context;
        this.playQueueManager = playQueueManager;
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
    public void playNewQueue(List<Urn> playQueueTracks, Urn initialTrackUrn, int initialTrackPosition, PlaySessionSource playSessionSource) {
        final int updatedPosition = PlaybackUtils.correctStartPositionAndDeduplicateList(playQueueTracks, initialTrackPosition, initialTrackUrn);
        final PlayQueue playQueue = PlayQueue.fromTrackUrnList(playQueueTracks, playSessionSource);
        playQueueManager.setNewPlayQueue(playQueue, updatedPosition, playSessionSource);
        playCurrent();
    }

    @Override
    public void reloadAndPlayCurrentQueue(long withProgressPosition) {
        throw new IllegalStateException("Reloading current queue and playing track from position not yet supported when not casting");
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
