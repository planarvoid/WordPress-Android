package com.soundcloud.android.collection.playhistory;

import static com.soundcloud.android.ApplicationModule.RX_HIGH_PRIORITY;

import com.soundcloud.android.main.Screen;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playback.PlaySessionSource;
import com.soundcloud.android.playback.PlaybackInitiator;
import com.soundcloud.android.playback.PlaybackResult;
import com.soundcloud.android.presentation.EntityItemCreator;
import com.soundcloud.android.sync.NewSyncOperations;
import com.soundcloud.android.sync.SyncResult;
import com.soundcloud.android.sync.Syncable;
import com.soundcloud.android.tracks.TrackItem;
import com.soundcloud.android.tracks.TrackRepository;
import com.soundcloud.java.collections.Lists;
import io.reactivex.Scheduler;
import io.reactivex.Single;

import javax.inject.Inject;
import javax.inject.Named;
import java.util.List;

public class PlayHistoryOperations {

    static final int MAX_HISTORY_ITEMS = 1000;

    private final PlaybackInitiator playbackInitiator;
    private final PlayHistoryStorage playHistoryStorage;
    private final Scheduler scheduler;
    private final NewSyncOperations syncOperations;
    private final ClearPlayHistoryCommand clearPlayHistoryCommand;
    private final EntityItemCreator entityItemCreator;
    private final TrackRepository trackRepository;

    @Inject
    public PlayHistoryOperations(PlaybackInitiator playbackInitiator,
                                 PlayHistoryStorage playHistoryStorage,
                                 @Named(RX_HIGH_PRIORITY) Scheduler scheduler,
                                 NewSyncOperations syncOperations,
                                 ClearPlayHistoryCommand clearPlayHistoryCommand,
                                 EntityItemCreator entityItemCreator,
                                 TrackRepository trackRepository) {
        this.playbackInitiator = playbackInitiator;
        this.playHistoryStorage = playHistoryStorage;
        this.scheduler = scheduler;
        this.syncOperations = syncOperations;
        this.clearPlayHistoryCommand = clearPlayHistoryCommand;
        this.entityItemCreator = entityItemCreator;
        this.trackRepository = trackRepository;
    }

    Single<List<TrackItem>> playHistory() {
        return playHistory(MAX_HISTORY_ITEMS);
    }

    public Single<List<TrackItem>> playHistory(final int limit) {
        return syncOperations.lazySyncIfStale(Syncable.PLAY_HISTORY)
                             .observeOn(scheduler)
                             .onErrorResumeNext(Single.just(SyncResult.noOp()))
                             .flatMap(__ -> tracks(limit));
    }

    Single<List<TrackItem>> refreshPlayHistory() {
        return refreshPlayHistory(MAX_HISTORY_ITEMS);
    }

    public Single<List<TrackItem>> refreshPlayHistory(final int limit) {
        return syncOperations.failSafeSync(Syncable.PLAY_HISTORY)
                             .observeOn(scheduler)
                             .flatMap(__ -> tracks(limit));
    }

    private Single<List<TrackItem>> tracks(int limit) {
        return playHistoryStorage.loadTracks(limit)
                                 .flatMap(trackRepository::trackListFromUrns)
                                 .map(tracks -> Lists.transform(tracks, entityItemCreator::trackItem));
    }

    public Single<PlaybackResult> startPlaybackFrom(Urn trackUrn, Screen screen) {
        return playbackInitiator.playTracks(getAllTracksForPlayback(), trackUrn, 0,
                                            PlaySessionSource.forHistory(screen));
    }

    Single<Boolean> clearHistory() {
        return clearPlayHistoryCommand.toSingle()
                     .subscribeOn(scheduler);
    }

    private Single<List<Urn>> getAllTracksForPlayback() {
        return playHistoryStorage.loadPlayHistoryForPlayback()
                                 //to filter out missing tracks
                                 .flatMap(trackRepository::availableTracks)
                                 .subscribeOn(scheduler);
    }
}
