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
import rx.functions.Func1;

import javax.inject.Inject;
import javax.inject.Named;
import java.util.List;

public class PlayHistoryOperations {

    private static final int MAX_HISTORY_ITEMS = 1000;
    @VisibleForTesting  public static final int MAX_RECENTLY_PLAYED = 500;

    private static final Func1<TrackItem, Long> BY_TRACK_ID =
            new Func1<TrackItem, Long>() {
                @Override
                public Long call(TrackItem trackItem) {
                    return trackItem.getUrn().getNumericId();
                }
            };

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

    // We preload up to 2 x limit and remove duplicate tracks by track id
    // rather than having a distinct on all columns or a complex group by
    public Observable<List<TrackItem>> playHistory(int limit) {
        return playHistoryStorage.fetchTracks(limit * 2)
                                 .distinct(BY_TRACK_ID)
                                 .take(limit)
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
