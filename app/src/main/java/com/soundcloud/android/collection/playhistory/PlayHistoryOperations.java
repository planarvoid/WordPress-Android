package com.soundcloud.android.collection.playhistory;

import static com.soundcloud.android.ApplicationModule.HIGH_PRIORITY;

import com.soundcloud.android.main.Screen;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playback.PlaySessionSource;
import com.soundcloud.android.playback.PlaybackInitiator;
import com.soundcloud.android.playback.PlaybackResult;
import com.soundcloud.android.presentation.EntityItemCreator;
import com.soundcloud.android.sync.SyncOperations;
import com.soundcloud.android.sync.SyncOperations.Result;
import com.soundcloud.android.sync.Syncable;
import com.soundcloud.android.tracks.TrackItem;
import rx.Observable;
import rx.Scheduler;

import javax.inject.Inject;
import javax.inject.Named;
import java.util.List;

public class PlayHistoryOperations {

    static final int MAX_HISTORY_ITEMS = 1000;

    private final PlaybackInitiator playbackInitiator;
    private final PlayHistoryStorage playHistoryStorage;
    private final Scheduler scheduler;
    private final SyncOperations syncOperations;
    private final ClearPlayHistoryCommand clearPlayHistoryCommand;
    private final EntityItemCreator entityItemCreator;

    @Inject
    public PlayHistoryOperations(PlaybackInitiator playbackInitiator,
                                 PlayHistoryStorage playHistoryStorage,
                                 @Named(HIGH_PRIORITY) Scheduler scheduler,
                                 SyncOperations syncOperations,
                                 ClearPlayHistoryCommand clearPlayHistoryCommand,
                                 EntityItemCreator entityItemCreator) {
        this.playbackInitiator = playbackInitiator;
        this.playHistoryStorage = playHistoryStorage;
        this.scheduler = scheduler;
        this.syncOperations = syncOperations;
        this.clearPlayHistoryCommand = clearPlayHistoryCommand;
        this.entityItemCreator = entityItemCreator;
    }

    Observable<List<TrackItem>> playHistory() {
        return playHistory(MAX_HISTORY_ITEMS);
    }

    public Observable<List<TrackItem>> playHistory(final int limit) {
        return syncOperations.lazySyncIfStale(Syncable.PLAY_HISTORY)
                             .observeOn(scheduler)
                             .onErrorResumeNext(Observable.just(Result.NO_OP))
                             .flatMap(o -> tracks(limit));
    }

    Observable<List<TrackItem>> refreshPlayHistory() {
        return refreshPlayHistory(MAX_HISTORY_ITEMS);
    }

    public Observable<List<TrackItem>> refreshPlayHistory(final int limit) {
        return syncOperations.failSafeSync(Syncable.PLAY_HISTORY)
                             .observeOn(scheduler)
                             .flatMap(o -> tracks(limit));
    }

    private Observable<List<TrackItem>> tracks(int limit) {
        return playHistoryStorage.loadTracks(limit).map(entityItemCreator::trackItem).toList();
    }

    public Observable<PlaybackResult> startPlaybackFrom(Urn trackUrn, Screen screen) {
        return playbackInitiator.playTracks(getAllTracksForPlayback(), trackUrn, 0,
                                            PlaySessionSource.forHistory(screen));
    }

    Observable<Boolean> clearHistory() {
        return clearPlayHistoryCommand.toObservable(null)
                                      .subscribeOn(scheduler);
    }

    private Observable<List<Urn>> getAllTracksForPlayback() {
        return playHistoryStorage.loadPlayHistoryForPlayback()
                                 .toList()
                                 .subscribeOn(scheduler);
    }
}
