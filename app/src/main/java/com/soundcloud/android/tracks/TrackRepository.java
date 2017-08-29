package com.soundcloud.android.tracks;

import static com.soundcloud.android.utils.DiffUtils.minus;
import static java.util.Collections.singletonList;

import com.soundcloud.android.ApplicationModule;
import com.soundcloud.android.api.ApiRequestException;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playlists.LoadPlaylistTracksCommand;
import com.soundcloud.android.storage.RepositoryMissedSyncEvent;
import com.soundcloud.android.sync.EntitySyncStateStorage;
import com.soundcloud.android.sync.SyncInitiator;
import com.soundcloud.android.sync.SyncJobResult;
import com.soundcloud.android.utils.CurrentDateProvider;
import com.soundcloud.android.utils.Urns;
import com.soundcloud.java.collections.Iterables;
import com.soundcloud.java.collections.Iterators;
import com.soundcloud.java.collections.Lists;
import com.soundcloud.rx.eventbus.EventBusV2;
import io.reactivex.Maybe;
import io.reactivex.MaybeSource;
import io.reactivex.Observable;
import io.reactivex.Scheduler;
import io.reactivex.Single;
import io.reactivex.SingleTransformer;
import io.reactivex.functions.Function;

import javax.inject.Inject;
import javax.inject.Named;
import java.util.Collection;
import java.util.List;
import java.util.Map;

public class TrackRepository {

    private final TrackStorage trackStorage;
    private final LoadPlaylistTracksCommand loadPlaylistTracksCommand;
    private final SyncInitiator syncInitiator;
    private final Scheduler scheduler;
    private final EntitySyncStateStorage entitySyncStateStorage;
    private final CurrentDateProvider currentDateProvider;
    private final EventBusV2 eventBus;

    @Inject
    public TrackRepository(TrackStorage trackStorage,
                           LoadPlaylistTracksCommand loadPlaylistTracksCommand,
                           SyncInitiator syncInitiator,
                           @Named(ApplicationModule.RX_HIGH_PRIORITY) Scheduler scheduler,
                           EntitySyncStateStorage entitySyncStateStorage,
                           CurrentDateProvider currentDateProvider,
                           EventBusV2 eventBus) {
        this.trackStorage = trackStorage;
        this.loadPlaylistTracksCommand = loadPlaylistTracksCommand;
        this.syncInitiator = syncInitiator;
        this.scheduler = scheduler;
        this.entitySyncStateStorage = entitySyncStateStorage;
        this.currentDateProvider = currentDateProvider;
        this.eventBus = eventBus;
    }

    public Maybe<Track> track(final Urn trackUrn) {
        return fromUrns(singletonList(trackUrn))
                .filter(urnTrackMap -> !urnTrackMap.isEmpty())
                .map(urnTrackMap -> urnTrackMap.values().iterator().next());
    }

    public Single<Map<Urn, Track>> fromUrns(final List<Urn> requestedTracks) {
        checkTracksUrn(requestedTracks);
        return trackStorage
                .availableTracks(requestedTracks)
                .flatMap(syncMissingTracks(requestedTracks))
                .flatMap(success -> trackStorage.loadTracks(requestedTracks))
                .doOnSuccess(loadedTracks -> reportMissingAfterSync(loadedTracks.size(), requestedTracks.size()))
                .subscribeOn(scheduler);
    }

    public Single<List<Urn>> availableTracks(List<Urn> requestedTracks) {
        return trackStorage.availableTracks(requestedTracks);
    }

    public Single<List<Track>> trackListFromUrns(List<Urn> requestedTracks) {
        return fromUrns(requestedTracks)
                .map(urnTrackMap -> Lists.newArrayList(Iterables.transform(Iterables.filter(requestedTracks, urnTrackMap::containsKey), urnTrackMap::get)));
    }

    public Single<List<Track>> forPlaylist(Urn playlistUrn) {
        if (entitySyncStateStorage.hasSyncedBefore(playlistUrn)) {
            return loadPlaylistTracks(playlistUrn);
        } else {
            return syncAndLoadPlaylistTracks(playlistUrn);
        }
    }

    public Single<List<Track>> forPlaylist(Urn playlistUrn, long staleTimeMillis) {
        if (currentDateProvider.getCurrentTime() - staleTimeMillis > entitySyncStateStorage.lastSyncTime(playlistUrn)) {
            return syncAndLoadPlaylistTracks(playlistUrn);
        } else {
            return loadPlaylistTracks(playlistUrn);
        }
    }

    private Single<List<Track>> loadPlaylistTracks(Urn playlistUrn) {
        return loadPlaylistTracksCommand
                .toSingle(playlistUrn)
                .subscribeOn(scheduler);
    }

    private Single<List<Track>> syncAndLoadPlaylistTracks(Urn playlistUrn) {
        return syncInitiator.syncPlaylist(playlistUrn)
                            .compose(convertApiRequestExceptionToSyncFailure())
                            .map(SyncJobResult::wasSuccess)
                            .observeOn(scheduler)
                            .flatMap(__ -> loadPlaylistTracksCommand.toSingle(playlistUrn));
    }

    private static SingleTransformer<SyncJobResult, SyncJobResult> convertApiRequestExceptionToSyncFailure() {
        return observable -> observable.onErrorResumeNext(
                throwable -> throwable instanceof ApiRequestException
                             ? Single.just(SyncJobResult.failure("unknown", (ApiRequestException) throwable))
                             : Single.error(throwable)
        );
    }

    private Function<List<Urn>, Single<Boolean>> syncMissingTracks(final List<Urn> requestedTracks) {
        return tracksAvailable -> {
            final List<Urn> missingTracks = minus(requestedTracks, tracksAvailable);
            if (missingTracks.isEmpty()) {
                return Single.just(false);
            } else {
                return syncInitiator.batchSyncTracks(missingTracks)
                                    .observeOn(scheduler)
                                    .map(SyncJobResult::wasSuccess);
            }
        };
    }

    Observable<Track> fullTrackWithUpdate(final Urn trackUrn) {
        checkTrackUrn(trackUrn);
        return Maybe.concat(fullTrackFromStorage(trackUrn), syncThenLoadTrack(trackUrn, fullTrackFromStorage(trackUrn))).toObservable();
    }

    private void checkTrackUrn(Urn trackUrn) {
        if (!trackUrn.isTrack()) {
            throw new IllegalArgumentException("Trying to sync track without a valid track urn");
        }
    }

    private void checkTracksUrn(Collection<Urn> trackUrns) {
        final boolean hasOnlyTracks = !Iterators.tryFind(trackUrns.iterator(), Urns.IS_NOT_TRACK).isPresent();
        if (!hasOnlyTracks) {
            throw new IllegalArgumentException("Trying to sync track without a valid track urn. trackUrns = [" + trackUrns + "]");
        }
    }

    private Maybe<Track> trackFromStorage(Urn trackUrn) {
        return trackStorage.loadTrack(trackUrn).subscribeOn(scheduler);
    }

    private Maybe<Track> fullTrackFromStorage(Urn trackUrn) {
        return trackFromStorage(trackUrn).flatMap(track -> trackStorage.loadTrackDescription(trackUrn)
                                                                       .map(description -> Track.copyWithDescription(track, description))
                                                                       .toMaybe())
                                         .subscribeOn(scheduler);
    }

    private Maybe<Track> syncThenLoadTrack(final Urn trackUrn,
                                           final Maybe<Track> loadObservable) {
        return syncInitiator.syncTrack(trackUrn).flatMapMaybe(o -> loadObservable).switchIfEmpty(logEmpty());
    }

    private <T> MaybeSource<? extends T> logEmpty() {
        return Maybe.<T>empty().doOnComplete(() -> logMissing(1));
    }

    private void reportMissingAfterSync(int loadedCount, int requestedCount) {
        if (requestedCount != loadedCount) {
            logMissing(requestedCount - loadedCount);
        }
    }

    private void logMissing(int missingCount) {
        eventBus.publish(EventQueue.TRACKING, RepositoryMissedSyncEvent.Companion.fromTracksMissing(missingCount));
    }
}
