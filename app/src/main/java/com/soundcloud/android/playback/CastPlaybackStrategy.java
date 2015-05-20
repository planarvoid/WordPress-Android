package com.soundcloud.android.playback;

import com.soundcloud.android.cast.CastPlayer;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playback.service.PlaySessionSource;
import rx.Observable;

import java.util.List;

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
    public void playCurrent() {
        castPlayer.playCurrent();
    }

    @Override
    public Observable<PlaybackResult> playNewQueue(List<Urn> playQueueTracks,
                                                   Urn initialTrackUrn,
                                                   int initialTrackPosition,
                                                   boolean loadRelated,
                                                   PlaySessionSource playSessionSource) {
        return castPlayer.playNewQueue(playQueueTracks, initialTrackUrn, 0L, playSessionSource);
    }

    @Override
    public void seek(long position) {
        castPlayer.seek(position);
    }
}
