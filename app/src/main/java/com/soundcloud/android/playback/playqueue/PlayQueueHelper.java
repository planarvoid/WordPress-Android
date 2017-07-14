package com.soundcloud.android.playback.playqueue;

import static com.soundcloud.java.collections.Lists.transform;

import com.soundcloud.android.analytics.ScreenProvider;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playback.PlayQueueManager;
import com.soundcloud.android.playback.PlaySessionSource;
import com.soundcloud.android.playback.PlaybackInitiator;
import com.soundcloud.android.playback.ShowPlayerSubscriber;
import com.soundcloud.android.playback.ui.view.PlaybackFeedbackHelper;
import com.soundcloud.android.playlists.PlaylistOperations;
import com.soundcloud.android.rx.RxJava;
import com.soundcloud.android.rx.observers.DefaultSubscriber;
import com.soundcloud.android.tracks.Track;
import com.soundcloud.android.tracks.TrackRepository;
import com.soundcloud.rx.eventbus.EventBus;
import rx.android.schedulers.AndroidSchedulers;

import javax.inject.Inject;
import java.util.List;

public class PlayQueueHelper {

    private final PlayQueueManager playQueueManager;
    private final PlaylistOperations playlistOperations;
    private final TrackRepository trackRepository;
    private final PlaybackFeedbackHelper playbackFeedbackHelper;
    private final EventBus eventBus;
    private final PlaybackInitiator playbackInitiator;
    private final ScreenProvider screenProvider;

    @Inject
    public PlayQueueHelper(PlayQueueManager playQueueManager,
                           PlaylistOperations playlistOperations,
                           TrackRepository trackRepository, PlaybackFeedbackHelper playbackFeedbackHelper,
                           EventBus eventBus,
                           PlaybackInitiator playbackInitiator,
                           ScreenProvider screenProvider) {
        this.playQueueManager = playQueueManager;
        this.playlistOperations = playlistOperations;
        this.trackRepository = trackRepository;
        this.playbackFeedbackHelper = playbackFeedbackHelper;
        this.eventBus = eventBus;
        this.playbackInitiator = playbackInitiator;
        this.screenProvider = screenProvider;
    }

    public void playNext(Urn playlistUrn) {
        if (playQueueManager.isQueueEmpty()) {
            playlistOperations.trackUrnsForPlayback(playlistUrn)
                              .flatMap(tracks -> RxJava.toV1Observable(playbackInitiator.playTracks(tracks, 0, PlaySessionSource.forPlayNext(screenProvider.getLastScreenTag()))))
                              .observeOn(AndroidSchedulers.mainThread())
                              .subscribe(new ShowPlayerSubscriber(eventBus, playbackFeedbackHelper));
        } else {
            RxJava.toV1Observable(trackRepository.forPlaylist(playlistUrn))
                  .observeOn(AndroidSchedulers.mainThread())
                  .subscribe(new InsertSubscriber());
        }
    }

    private class InsertSubscriber extends DefaultSubscriber<List<Track>> {

        @Override
        public void onNext(List<Track> trackItems) {
            playQueueManager.insertNext(transform(trackItems, Track::urn));
        }
    }

}
