package com.soundcloud.android.playback.playqueue;

import static com.soundcloud.java.collections.Lists.transform;

import com.soundcloud.android.analytics.ScreenProvider;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playback.ExpandPlayerCommand;
import com.soundcloud.android.playback.ExpandPlayerSingleObserver;
import com.soundcloud.android.playback.PlayQueueManager;
import com.soundcloud.android.playback.PlaySessionSource;
import com.soundcloud.android.playback.PlaybackInitiator;
import com.soundcloud.android.playlists.PlaylistOperations;
import com.soundcloud.android.rx.observers.DefaultSingleObserver;
import com.soundcloud.android.tracks.Track;
import com.soundcloud.android.tracks.TrackRepository;
import io.reactivex.android.schedulers.AndroidSchedulers;

import javax.inject.Inject;
import java.util.List;

public class PlayQueueHelper {

    private final PlayQueueManager playQueueManager;
    private final PlaylistOperations playlistOperations;
    private final TrackRepository trackRepository;
    private final PlaybackInitiator playbackInitiator;
    private final ScreenProvider screenProvider;
    private final ExpandPlayerCommand expandPlayerCommand;

    @Inject
    public PlayQueueHelper(PlayQueueManager playQueueManager,
                           PlaylistOperations playlistOperations,
                           TrackRepository trackRepository,
                           PlaybackInitiator playbackInitiator,
                           ScreenProvider screenProvider,
                           ExpandPlayerCommand expandPlayerCommand) {
        this.playQueueManager = playQueueManager;
        this.playlistOperations = playlistOperations;
        this.trackRepository = trackRepository;
        this.playbackInitiator = playbackInitiator;
        this.screenProvider = screenProvider;
        this.expandPlayerCommand = expandPlayerCommand;
    }

    public void playNext(Urn playlistUrn) {
        if (playQueueManager.isQueueEmpty()) {
            playlistOperations.trackUrnsForPlayback(playlistUrn)
                              .flatMap(tracks -> playbackInitiator.playTracks(tracks, 0, PlaySessionSource.forPlayNext(screenProvider.getLastScreenTag())))
                              .observeOn(AndroidSchedulers.mainThread())
                              .subscribeWith(new ExpandPlayerSingleObserver(expandPlayerCommand));
        } else {
            trackRepository.forPlaylist(playlistUrn)
                  .observeOn(AndroidSchedulers.mainThread())
                  .subscribe(new InsertObserver());
        }
    }

    private class InsertObserver extends DefaultSingleObserver<List<Track>> {

        @Override
        public void onSuccess(List<Track> trackItems) {
            playQueueManager.insertNext(transform(trackItems, Track::urn));
            super.onSuccess(trackItems);
        }
    }

}
