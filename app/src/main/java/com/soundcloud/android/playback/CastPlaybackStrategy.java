package com.soundcloud.android.playback;

import com.soundcloud.android.cast.CastPlayer;
import com.soundcloud.android.model.Urn;
import com.soundcloud.java.collections.Iterables;
import com.soundcloud.java.collections.Lists;
import com.soundcloud.java.functions.Function;
import rx.Observable;

import java.util.List;

public class CastPlaybackStrategy implements PlaybackStrategy {

    private final CastPlayer castPlayer;
    private final Function<PlayQueueItem, Urn> toUrn = new Function<PlayQueueItem, Urn>() {
        @Override
        public Urn apply(PlayQueueItem playQueueItem) {
            return playQueueItem.getTrackUrn();
        }
    };

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
    public Observable<PlaybackResult> playNewQueue(final PlayQueue playQueue,
                                                   Urn initialTrackUrn,
                                                   int initialTrackPosition,
                                                   boolean loadRelated,
                                                   PlaySessionSource playSessionSource) {

        // TODO: Should eventually refactor to use the playQueue instead of a list of Urn
        List<Urn> tracks = Lists.newArrayList(Iterables.transform(playQueue, toUrn));

        return castPlayer.playNewQueue(tracks, initialTrackUrn, 0L, playSessionSource);
    }

    @Override
    public void seek(long position) {
        castPlayer.seek(position);
    }
}
