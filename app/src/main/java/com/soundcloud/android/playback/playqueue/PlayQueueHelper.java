package com.soundcloud.android.playback.playqueue;

import static com.soundcloud.java.collections.Lists.transform;

import com.soundcloud.android.analytics.ScreenProvider;
import com.soundcloud.android.analytics.performance.PerformanceMetricsEngine;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playback.ExpandPlayerObserver;
import com.soundcloud.android.playback.PlayQueueManager;
import com.soundcloud.android.playback.PlaySessionSource;
import com.soundcloud.android.playback.PlaybackInitiator;
import com.soundcloud.android.playback.ui.view.PlaybackFeedbackHelper;
import com.soundcloud.android.playlists.PlaylistOperations;
import com.soundcloud.android.rx.RxJava;
import com.soundcloud.android.rx.observers.DefaultSingleObserver;
import com.soundcloud.android.tracks.Track;
import com.soundcloud.android.tracks.TrackRepository;
import com.soundcloud.rx.eventbus.EventBusV2;
import io.reactivex.android.schedulers.AndroidSchedulers;

import javax.inject.Inject;
import java.util.List;

public class PlayQueueHelper {

    private final PlayQueueManager playQueueManager;
    private final PlaylistOperations playlistOperations;
    private final TrackRepository trackRepository;
    private final PlaybackFeedbackHelper playbackFeedbackHelper;
    private final EventBusV2 eventBus;
    private final PlaybackInitiator playbackInitiator;
    private final ScreenProvider screenProvider;
    private final PerformanceMetricsEngine performanceMetricsEngine;

    @Inject
    public PlayQueueHelper(PlayQueueManager playQueueManager,
                           PlaylistOperations playlistOperations,
                           TrackRepository trackRepository, PlaybackFeedbackHelper playbackFeedbackHelper,
                           EventBusV2 eventBus,
                           PlaybackInitiator playbackInitiator,
                           ScreenProvider screenProvider,
                           PerformanceMetricsEngine performanceMetricsEngine) {
        this.playQueueManager = playQueueManager;
        this.playlistOperations = playlistOperations;
        this.trackRepository = trackRepository;
        this.playbackFeedbackHelper = playbackFeedbackHelper;
        this.eventBus = eventBus;
        this.playbackInitiator = playbackInitiator;
        this.screenProvider = screenProvider;
        this.performanceMetricsEngine = performanceMetricsEngine;
    }

    public void playNext(Urn playlistUrn) {
        if (playQueueManager.isQueueEmpty()) {
            RxJava.toV2Observable(playlistOperations.trackUrnsForPlayback(playlistUrn))
                              .flatMapSingle(tracks -> playbackInitiator.playTracks(tracks, 0, PlaySessionSource.forPlayNext(screenProvider.getLastScreenTag())))
                              .observeOn(AndroidSchedulers.mainThread())
                              .subscribeWith(new ExpandPlayerObserver(eventBus, playbackFeedbackHelper, performanceMetricsEngine));
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
