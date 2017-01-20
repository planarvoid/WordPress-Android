package com.soundcloud.android.playback.playqueue;

import static com.soundcloud.java.collections.Lists.transform;

import com.soundcloud.android.analytics.ScreenProvider;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playback.PlayQueueManager;
import com.soundcloud.android.playback.PlaySessionSource;
import com.soundcloud.android.playback.PlaybackInitiator;
import com.soundcloud.android.playback.PlaybackResult;
import com.soundcloud.android.playback.ShowPlayerSubscriber;
import com.soundcloud.android.playback.ui.view.PlaybackToastHelper;
import com.soundcloud.android.playlists.PlaylistOperations;
import com.soundcloud.android.presentation.PlayableItem;
import com.soundcloud.android.rx.observers.DefaultSubscriber;
import com.soundcloud.android.tracks.TrackItem;
import com.soundcloud.android.tracks.TrackRepository;
import com.soundcloud.rx.eventbus.EventBus;
import rx.Observable;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Func1;

import javax.inject.Inject;
import java.util.List;

public class PlayQueueHelper {

    private final Func1<List<Urn>, Observable<PlaybackResult>> playTracks = new Func1<List<Urn>, Observable<PlaybackResult>>() {
        @Override
        public Observable<PlaybackResult> call(List<Urn> tracks) {
            final String screen = screenProvider.getLastScreenTag();
            return playbackInitiator.playTracks(tracks, 0, PlaySessionSource.forPlayNext(screen));
        }
    };

    private final PlayQueueManager playQueueManager;
    private final PlaylistOperations playlistOperations;
    private final TrackRepository trackRepository;
    private final PlaybackToastHelper playbackToastHelper;
    private final EventBus eventBus;
    private final PlaybackInitiator playbackInitiator;
    private final ScreenProvider screenProvider;

    @Inject
    public PlayQueueHelper(PlayQueueManager playQueueManager,
                           PlaylistOperations playlistOperations,
                           TrackRepository trackRepository, PlaybackToastHelper playbackToastHelper,
                           EventBus eventBus,
                           PlaybackInitiator playbackInitiator,
                           ScreenProvider screenProvider) {
        this.playQueueManager = playQueueManager;
        this.playlistOperations = playlistOperations;
        this.trackRepository = trackRepository;
        this.playbackToastHelper = playbackToastHelper;
        this.eventBus = eventBus;
        this.playbackInitiator = playbackInitiator;
        this.screenProvider = screenProvider;
    }

    public void playNext(Urn playlistUrn) {
        if (playQueueManager.isQueueEmpty()) {
            playlistOperations.trackUrnsForPlayback(playlistUrn)
                              .flatMap(playTracks)
                              .observeOn(AndroidSchedulers.mainThread())
                              .subscribe(new ShowPlayerSubscriber(eventBus, playbackToastHelper));
        } else {
            trackRepository.forPlaylist(playlistUrn)
                              .observeOn(AndroidSchedulers.mainThread())
                              .subscribe(new InsertSubscriber());
        }
    }

    private class InsertSubscriber extends DefaultSubscriber<List<TrackItem>> {

        @Override
        public void onNext(List<TrackItem> trackItems) {
            playQueueManager.insertNext(transform(trackItems, PlayableItem::getUrn));
        }
    }

}
