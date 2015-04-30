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

//    @Override
//    public void playNewQueue(List<Urn> playQueueTracks, Urn initialTrackUrnCandidate, int initialTrackPosition, PlaySessionSource playSessionSource) {
//        castPlayer.playNewQueue(0L, playQueueTracks, initialTrackUrnCandidate, playSessionSource);
//    }

    @Override
    public Observable<PlaybackResult> playNewQueue(Observable<List<Urn>> playQueueTracks,
                                                   Urn initialTrackUrn,
                                                   int initialTrackPosition,
                                                   boolean loadRelated,
                                                   PlaySessionSource playSessionSource) {
        return castPlayer.playNewQueueOBS(playQueueTracks, initialTrackUrn, 0L, playSessionSource);
    }

    @Override
    public void reloadAndPlayCurrentQueue(long withProgressPosition) {
        castPlayer.reloadAndPlayCurrentQueue(withProgressPosition);
    }

    @Override
    public void seek(long position) {
        castPlayer.seek(position);
    }
}
