package com.soundcloud.android.tracks;

import static com.soundcloud.android.utils.DiffUtils.minus;
import static com.soundcloud.java.checks.Preconditions.checkArgument;
import static java.util.Collections.singletonList;

import com.soundcloud.android.ApplicationModule;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playlists.LoadPlaylistTracksCommand;
import com.soundcloud.android.sync.EntitySyncStateStorage;
import com.soundcloud.android.sync.SyncInitiator;
import com.soundcloud.android.utils.CurrentDateProvider;
import com.soundcloud.android.utils.Urns;
import com.soundcloud.java.collections.Iterables;
import com.soundcloud.java.collections.Iterators;
import com.soundcloud.java.collections.Lists;
import com.soundcloud.java.optional.Optional;
import rx.Observable;
import rx.Scheduler;
import rx.functions.Func1;

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

    @Inject
    public TrackRepository(TrackStorage trackStorage,
                           LoadPlaylistTracksCommand loadPlaylistTracksCommand,
                           SyncInitiator syncInitiator,
                           @Named(ApplicationModule.HIGH_PRIORITY) Scheduler scheduler,
                           EntitySyncStateStorage entitySyncStateStorage,
                           CurrentDateProvider currentDateProvider) {
        this.trackStorage = trackStorage;
        this.loadPlaylistTracksCommand = loadPlaylistTracksCommand;
        this.syncInitiator = syncInitiator;
        this.scheduler = scheduler;
        this.entitySyncStateStorage = entitySyncStateStorage;
        this.currentDateProvider = currentDateProvider;
    }

    public Observable<Track> track(final Urn trackUrn) {
        return fromUrns(singletonList(trackUrn)).flatMap(new Func1<Map<Urn, Track>, Observable<Track>>() {
            @Override
            public Observable<Track> call(Map<Urn, Track> urnTrackMap) {
                return urnTrackMap.isEmpty() ? Observable.empty() : Observable.just(urnTrackMap.values().iterator().next());
            }
        });
    }

    public Observable<Map<Urn, Track>> fromUrns(final List<Urn> requestedTracks) {
        checkTracksUrn(requestedTracks);
        return trackStorage
                .availableTracks(requestedTracks)
                .flatMap(syncMissingTracks(requestedTracks))
                .flatMap(o -> trackStorage.loadTracks(requestedTracks))
                .subscribeOn(scheduler);
    }

    public Observable<List<Track>> trackListFromUrns(List<Urn> requestedTracks) {
        return fromUrns(requestedTracks)
                .map(urnTrackMap -> Lists.newArrayList(Iterables.transform(Iterables.filter(requestedTracks, urnTrackMap::containsKey), urnTrackMap::get)));
    }

    public Observable<List<Track>> forPlaylist(Urn playlistUrn) {
        if (entitySyncStateStorage.hasSyncedBefore(playlistUrn)) {
            return loadPlaylistTracks(playlistUrn);
        } else {
            return syncAndLoadPlaylistTracks(playlistUrn);
        }
    }

    public Observable<List<Track>> forPlaylist(Urn playlistUrn, long staleTimeMillis) {
        if (currentDateProvider.getCurrentTime() - staleTimeMillis > entitySyncStateStorage.lastSyncTime(playlistUrn)) {
            return syncAndLoadPlaylistTracks(playlistUrn);
        } else {
            return loadPlaylistTracks(playlistUrn);
        }
    }

    private Observable<List<Track>> loadPlaylistTracks(Urn playlistUrn) {
        return loadPlaylistTracksCommand
                .toObservable(playlistUrn)
                .subscribeOn(scheduler);
    }

    private Observable<List<Track>> syncAndLoadPlaylistTracks(Urn playlistUrn) {
        return syncInitiator
                .syncPlaylist(playlistUrn)
                .observeOn(scheduler)
                .flatMap(ignored -> loadPlaylistTracksCommand.toObservable(playlistUrn));
    }

    private Func1<List<Urn>, Observable<?>> syncMissingTracks(final List<Urn> requestedTracks) {
        return tracksAvailable -> {
            final List<Urn> missingTracks = minus(requestedTracks, tracksAvailable);
            if (missingTracks.isEmpty()) {
                return Observable.just(null);
            } else {
                return syncInitiator
                        .batchSyncTracks(missingTracks)
                        .observeOn(scheduler);
            }
        };
    }

    Observable<Track> fullTrackWithUpdate(final Urn trackUrn) {
        checkTrackUrn(trackUrn);
        return Observable.concat(
                fullTrackFromStorage(trackUrn),
                syncThenLoadTrack(trackUrn, fullTrackFromStorage(trackUrn))
        );
    }

    private void checkTrackUrn(Urn trackUrn) {
        checkArgument(trackUrn.isTrack(), "Trying to sync track without a valid track urn");
    }

    private void checkTracksUrn(Collection<Urn> trackUrns) {
        final boolean hasOnlyTracks = !Iterators.tryFind(trackUrns.iterator(), Urns.IS_NOT_TRACK).isPresent();
        checkArgument(hasOnlyTracks, "Trying to sync track without a valid track urn. trackUrns = [" + trackUrns + "]");
    }

    private Observable<Optional<Track>> trackFromStorage(Urn trackUrn) {
        return trackStorage.loadTrack(trackUrn).subscribeOn(scheduler);
    }

    private Observable<Track> fullTrackFromStorage(Urn trackUrn) {
        return trackFromStorage(trackUrn).filter(Optional::isPresent)
                                         .map(Optional::get)
                                         .zipWith(trackStorage.loadTrackDescription(trackUrn), Track::copyWithDescription)
                                         .subscribeOn(scheduler);
    }

    private Observable<Track> syncThenLoadTrack(final Urn trackUrn,
                                                final Observable<Track> loadObservable) {
        return syncInitiator.syncTrack(trackUrn).flatMap(o -> loadObservable);
    }

}
