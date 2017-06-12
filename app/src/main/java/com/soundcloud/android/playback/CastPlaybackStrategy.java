package com.soundcloud.android.playback;

import com.soundcloud.android.cast.CastPlayer;
import com.soundcloud.android.model.Urn;
import io.reactivex.Completable;
import io.reactivex.Single;

public class CastPlaybackStrategy implements PlaybackStrategy {

    private final PlayQueueManager playQueueManager;
    private final CastPlayer castPlayer;

    public CastPlaybackStrategy(PlayQueueManager playQueueManager, CastPlayer castPlayer) {
        this.playQueueManager = playQueueManager;
        this.castPlayer = castPlayer;
    }

    @Override
    public void togglePlayback() {
        castPlayer.togglePlayback();
    }

    @Override
    public void resume() {
        castPlayer.resume();
    }

    @Override
    public void pause() {
        castPlayer.pause();
    }

    @Override
    public void fadeAndPause() {
        castPlayer.pause();
    }

    @Override
    public Completable playCurrent() {
        final PlayQueueItem currentPlayQueueItem = playQueueManager.getCurrentPlayQueueItem();
        if (currentPlayQueueItem.isPlayable()) {
            castPlayer.playCurrent();
        }
        return Completable.complete();
    }

    @Override
    public Single<PlaybackResult> setNewQueue(final PlayQueue playQueue,
                                              Urn initialTrackUrn,
                                              int initialTrackPosition,
                                              PlaySessionSource playSessionSource) {
        return castPlayer.setNewQueue(playQueue, initialTrackUrn, playSessionSource);
    }

    @Override
    public void seek(long position) {
        castPlayer.seek(position);
    }
}
