package com.soundcloud.android.playback;

import com.soundcloud.android.cast.CastPlayer;
import com.soundcloud.android.model.Urn;
import rx.Observable;

public class CastPlaybackStrategy implements PlaybackStrategy {

    private final CastPlayer castPlayer;

    public CastPlaybackStrategy(CastPlayer castPlayer) {
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
    public Observable<Void> playCurrent() {
        castPlayer.playCurrent();
        return Observable.just(null);
    }

    @Override
    public Observable<PlaybackResult> setNewQueue(final PlayQueue playQueue,
                                                  Urn initialTrackUrn,
                                                  int initialTrackPosition,
                                                  PlaySessionSource playSessionSource) {
        // TODO: Should eventually refactor to use the playQueue instead of a list of Urn
        return castPlayer.setNewQueue(playQueue.getTrackItemUrns(), initialTrackUrn, playSessionSource);
    }

    @Override
    public void seek(long position) {
        castPlayer.seek(position);
    }
}
