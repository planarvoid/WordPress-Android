package com.soundcloud.android.stations;

import static com.soundcloud.android.events.EntityStateChangedEvent.fromStationsUpdated;
import static com.soundcloud.android.events.EventQueue.ENTITY_STATE_CHANGED;
import static com.soundcloud.android.playback.PlaySessionSource.forStation;
import static com.soundcloud.android.rx.RxUtils.IS_TRUE;
import static com.soundcloud.android.rx.RxUtils.continueWith;

import com.soundcloud.android.ApplicationModule;
import com.soundcloud.android.commands.StoreTracksCommand;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playback.DiscoverySource;
import com.soundcloud.android.playback.PlayQueue;
import com.soundcloud.android.playback.PlaySessionSource;
import com.soundcloud.android.stream.SoundStreamItem;
import com.soundcloud.android.sync.SyncInitiator;
import com.soundcloud.android.sync.SyncJobResult;
import com.soundcloud.android.sync.SyncStateStorage;
import com.soundcloud.android.sync.Syncable;
import com.soundcloud.java.optional.Optional;
import com.soundcloud.propeller.ChangeResult;
import com.soundcloud.rx.eventbus.EventBus;
import rx.Observable;
import rx.Scheduler;
import rx.functions.Action1;
import rx.functions.Func1;
import rx.internal.util.UtilityFunctions;

import javax.inject.Inject;
import javax.inject.Named;
import java.util.List;
import java.util.concurrent.Callable;

public class StationsOperations {

    private static final Func1<StationRecord, Boolean> HAS_TRACKS = new Func1<StationRecord, Boolean>() {
        @Override
        public Boolean call(StationRecord station) {
            return station.getTracks().size() > 0;
        }
    };

    private static final Func1<StationWithTracks, Boolean> STATIONS_CONTAINING_TRACKS = new Func1<StationWithTracks, Boolean>() {
        @Override
        public Boolean call(StationWithTracks station) {
            return station.getStationInfoTracks().size() > 0;
        }
    };

    private final Action1<ApiStation> storeTracks = new Action1<ApiStation>() {
        @Override
        public void call(ApiStation apiStation) {
            storeTracksCommand.call(apiStation.getTrackRecords());
        }
    };

    private final Func1<Boolean, Boolean> markMigrationCompleted = new Func1<Boolean, Boolean>() {
        @Override
        public Boolean call(Boolean wasSuccessful) {
            stationsStorage.markRecentToLikedMigrationComplete();
            return true;
        }
    };

    private final SyncStateStorage syncStateStorage;
    private final StationsStorage stationsStorage;
    private final StationsApi stationsApi;
    private final StoreTracksCommand storeTracksCommand;
    private final StoreStationCommand storeStationCommand;
    private final SyncInitiator syncInitiator;
    private final Scheduler scheduler;
    private final EventBus eventBus;

    @Inject
    public StationsOperations(SyncStateStorage syncStateStorage,
                              StationsStorage stationsStorage,
                              StationsApi stationsApi,
                              StoreTracksCommand storeTracksCommand,
                              StoreStationCommand storeStationCommand,
                              SyncInitiator syncInitiator,
                              @Named(ApplicationModule.HIGH_PRIORITY) Scheduler scheduler, EventBus eventBus) {
        this.syncStateStorage = syncStateStorage;
        this.stationsStorage = stationsStorage;
        this.stationsApi = stationsApi;
        this.syncInitiator = syncInitiator;
        this.scheduler = scheduler;
        this.storeTracksCommand = storeTracksCommand;
        this.storeStationCommand = storeStationCommand;
        this.eventBus = eventBus;
    }

    public Observable<StationRecord> station(Urn station) {
        return station(station, UtilityFunctions.<StationRecord>identity());
    }

    Observable<StationRecord> stationWithSeed(Urn station, final Urn seed) {
        return station(station, prependSeed(seed));
    }

    Observable<StationWithTracks> stationWithTracks(Urn station, final Optional<Urn> seed) {
        return stationWithTracks(station,
                                 seed.isPresent() ?
                                 prependSeed(seed.get()) :
                                 UtilityFunctions.<StationRecord>identity());
    }

    private Observable<StationWithTracks> stationWithTracks(Urn station,
                                                            Func1<StationRecord, StationRecord> toStation) {
        return stationsStorage
                .clearExpiredPlayQueue(station)
                .flatMap(continueWith(loadStationWithTracks(station, toStation)))
                .subscribeOn(scheduler);
    }

    private Observable<StationRecord> station(Urn station, Func1<StationRecord, StationRecord> toStation) {
        return stationsStorage
                .clearExpiredPlayQueue(station)
                .flatMap(continueWith(getStation(station, toStation)))
                .subscribeOn(scheduler);
    }

    private Observable<StationRecord> getStation(Urn station, Func1<StationRecord, StationRecord> toStation) {
        return Observable
                .concat(stationsStorage.station(station).filter(HAS_TRACKS),
                        syncSingleStation(station, toStation)
                )
                .first();
    }

    private Observable<StationWithTracks> loadStationWithTracks(Urn station,
                                                                Func1<StationRecord, StationRecord> toStation) {
        return Observable
                .concat(loadStationWithTracks(station).filter(STATIONS_CONTAINING_TRACKS),
                        syncSingleStation(station, toStation).flatMap(continueWith(loadStationWithTracks(station))))
                .first();
    }

    private Observable<StationWithTracks> loadStationWithTracks(Urn station) {
        return stationsStorage.stationWithTracks(station);
    }

    private Observable<StationRecord> syncSingleStation(Urn station,
                                                        Func1<StationRecord, StationRecord> toStation) {
        return stationsApi.fetchStation(station)
                          .doOnNext(storeTracks)
                          .map(toStation)
                          .doOnNext(storeStationCommand.toAction1());
    }

    private Func1<StationRecord, StationRecord> prependSeed(final Urn seed) {
        return new Func1<StationRecord, StationRecord>() {
            @Override
            public StationRecord call(StationRecord station) {
                if (station.getTracks().isEmpty()) {
                    return station;
                }
                return Station.stationWithSeedTrack(station, seed);
            }
        };
    }

    public Observable<StationRecord> collection(final int type) {
        final Observable<StationRecord> collection;
        if (syncStateStorage.hasSyncedBefore(typeToSyncable(type))) {
            collection = loadStationsCollection(type);
        } else {
            collection = syncAndLoadStationsCollection(type);
        }
        return collection.subscribeOn(scheduler);
    }

    private Observable<StationRecord> loadStationsCollection(final int type) {
        return stationsStorage.getStationsCollection(type).subscribeOn(scheduler);
    }

    private Observable<StationRecord> syncAndLoadStationsCollection(int type) {
        return syncStations(type).flatMap(continueWith(loadStationsCollection(type)));
    }

    public Observable<SyncJobResult> syncStations(int type) {
        if (StationsCollectionsTypes.LIKED == type) {
            return migrateRecentToLikedIfNeeded().flatMap(continueWith(syncLikedStations()));
        } else {
            return syncInitiator.sync(typeToSyncable(type));
        }
    }

    Observable<SyncJobResult> syncLikedStations() {
        return syncInitiator.sync(Syncable.LIKED_STATIONS);
    }

    Observable<Boolean> migrateRecentToLikedIfNeeded() {
        if (stationsStorage.shouldRunRecentToLikedMigration()) {
            return stationsApi.requestRecentToLikedMigration()
                              .filter(IS_TRUE)
                              .map(markMigrationCompleted)
                              .subscribeOn(scheduler);
        } else {
            return Observable.just(true);
        }
    }

    ChangeResult saveLastPlayedTrackPosition(Urn collectionUrn, int position) {
        return stationsStorage.saveLastPlayedTrackPosition(collectionUrn, position);
    }

    private Syncable typeToSyncable(int type) {
        switch (type) {
            case StationsCollectionsTypes.LIKED:
                return Syncable.LIKED_STATIONS;
            case StationsCollectionsTypes.RECOMMENDATIONS:
                return Syncable.RECOMMENDED_STATIONS;
            default:
                throw new IllegalArgumentException("Unknown station's type: " + type);
        }
    }

    ChangeResult saveRecentlyPlayedStation(Urn stationUrn) {
        final ChangeResult result = stationsStorage.saveUnsyncedRecentlyPlayedStation(stationUrn);
        syncInitiator.requestSystemSync();
        return result;
    }

    Observable<ChangeResult> toggleStationLike(Urn stationUrn, boolean liked) {
        return stationsStorage.updateLocalStationLike(stationUrn, liked)
                              .doOnNext(eventBus.publishAction1(ENTITY_STATE_CHANGED, fromStationsUpdated(stationUrn)))
                              .subscribeOn(scheduler);
    }

    public Observable<PlayQueue> fetchUpcomingTracks(final Urn station,
                                                     final int currentSize,
                                                     final PlaySessionSource playSessionSource) {
        final PlaySessionSource discoverySource = forStation(playSessionSource.getOriginScreen(),
                                                             playSessionSource.getCollectionOwnerUrn(),
                                                             DiscoverySource.STATIONS_SUGGESTIONS);
        return stationsApi
                .fetchStation(station)
                .doOnNext(storeTracks)
                .doOnNext(storeStationCommand.toAction1())
                .flatMap(loadPlayQueue(station, currentSize))
                .toList()
                .map(toPlayQueue(station, discoverySource))
                .subscribeOn(scheduler);
    }

    private Func1<List<StationTrack>, PlayQueue> toPlayQueue(final Urn station,
                                                             final PlaySessionSource playSessionSource) {
        return new Func1<List<StationTrack>, PlayQueue>() {
            @Override
            public PlayQueue call(List<StationTrack> tracks) {
                return PlayQueue.fromStation(station, tracks, playSessionSource);
            }
        };
    }

    private Func1<StationRecord, Observable<StationTrack>> loadPlayQueue(final Urn station, final int startPosition) {
        return new Func1<StationRecord, Observable<StationTrack>>() {
            @Override
            public Observable<StationTrack> call(StationRecord ignored) {
                return stationsStorage.loadPlayQueue(station, startPosition);
            }
        };
    }

    public void clearData() {
        stationsStorage.clear();
    }

    public Observable<SoundStreamItem> onboardingStreamItem() {
        return shouldShowOnboardingStreamItem()
                .filter(IS_TRUE)
                .flatMap(continueWith(Observable.just(SoundStreamItem.forStationOnboarding())));
    }

    private Observable<Boolean> shouldShowOnboardingStreamItem() {
        return Observable.fromCallable(new Callable<Boolean>() {
            public Boolean call() {
                return !stationsStorage.isOnboardingStreamItemDisabled();
            }
        });
    }

    public void disableOnboardingStreamItem() {
        stationsStorage.disableOnboardingStreamItem();
    }

    boolean shouldShowLikedStationsOnboarding() {
        return !stationsStorage.isOnboardingForLikedStationsDisabled();
    }

    void disableLikedStationsOnboarding() {
        stationsStorage.disableLikedStationsOnboarding();
    }
}
