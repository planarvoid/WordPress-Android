package com.soundcloud.android.collection.playhistory;

import static com.soundcloud.android.ApplicationModule.HIGH_PRIORITY;

import com.soundcloud.android.collection.recentlyplayed.RecentlyPlayedItem;
import com.soundcloud.android.main.Screen;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playback.PlaySessionSource;
import com.soundcloud.android.playback.PlaybackInitiator;
import com.soundcloud.android.playback.PlaybackResult;
import com.soundcloud.android.tracks.TrackItem;
import com.soundcloud.annotations.VisibleForTesting;
import rx.Observable;
import rx.Scheduler;

import javax.inject.Inject;
import javax.inject.Named;
import java.util.List;

public class PlayHistoryOperations {

    public static final int CAROUSEL_ITEMS = 10;
    private static final int MAX_HISTORY_ITEMS = 1000;
    @VisibleForTesting  public static final int MAX_RECENTLY_PLAYED = 500;

    private final PlaybackInitiator playbackInitiator;
    private final PlayHistoryStorage playHistoryStorage;
    private final Scheduler scheduler;

    @Inject
    public PlayHistoryOperations(PlaybackInitiator playbackInitiator,
                          PlayHistoryStorage playHistoryStorage,
                          @Named(HIGH_PRIORITY) Scheduler scheduler) {
        this.playbackInitiator = playbackInitiator;
        this.playHistoryStorage = playHistoryStorage;
        this.scheduler = scheduler;
    }

    public Observable<List<TrackItem>> playHistory() {
        return playHistory(MAX_HISTORY_ITEMS);
    }

    public Observable<List<TrackItem>> playHistory(int limit) {
        return playHistoryStorage.fetchTracks(limit)
                                 .toList()
                                 .subscribeOn(scheduler);
    }

    Observable<PlaybackResult> startPlaybackFrom(Urn trackUrn, Screen screen) {
        return playbackInitiator.playTracks(getAllTracksForPlayback(), trackUrn, 0,
                                            new PlaySessionSource(screen));
    }

    public Observable<List<RecentlyPlayedItem>> recentlyPlayed(int limit) {
        return playHistoryStorage.fetchContexts(limit)
                                 .toList()
                                 .subscribeOn(scheduler);
    }

    public Observable<RecentlyPlayedItem> recentlyPlayed() {
        return playHistoryStorage
                .fetchContexts(MAX_RECENTLY_PLAYED)
                .subscribeOn(scheduler);
    }

    private Observable<List<Urn>> getAllTracksForPlayback() {
        return playHistoryStorage.fetchPlayHistoryForPlayback()
                                 .toList()
                                 .subscribeOn(scheduler);
    }
}