package com.soundcloud.android.offline;

import com.soundcloud.android.ApplicationModule;
import com.soundcloud.android.accounts.AccountOperations;
import com.soundcloud.android.events.CurrentUserChangedEvent;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.rx.observers.LambdaObserver;
import com.soundcloud.java.collections.Pair;
import com.soundcloud.rx.eventbus.EventBusV2;
import io.reactivex.BackpressureStrategy;
import io.reactivex.Observable;
import io.reactivex.Scheduler;
import io.reactivex.Single;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.subjects.BehaviorSubject;
import io.reactivex.subjects.Subject;

import android.annotation.SuppressLint;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Singleton
public class OfflinePropertiesProvider implements IOfflinePropertiesProvider {

    private final TrackDownloadsStorage trackDownloadsStorage;
    private final OfflineStateOperations offlineStateOperations;
    private final EventBusV2 eventBus;
    private final Scheduler scheduler;
    private final AccountOperations accountOperations;
    private final OfflineContentStorage offlineContentStorage;
    private final BehaviorSubject<OfflineProperties> subject = BehaviorSubject.create();

    @SuppressLint("sc.MissingCompositeDisposableRecycle")
    private final CompositeDisposable disposable = new CompositeDisposable();

    @Inject
    public OfflinePropertiesProvider(TrackDownloadsStorage trackDownloadsStorage,
                                     OfflineStateOperations offlineStateOperations,
                                     EventBusV2 eventBus,
                                     @Named(ApplicationModule.RX_HIGH_PRIORITY) Scheduler scheduler,
                                     AccountOperations accountOperations,
                                     OfflineContentStorage offlineContentStorage) {
        this.trackDownloadsStorage = trackDownloadsStorage;
        this.offlineStateOperations = offlineStateOperations;
        this.eventBus = eventBus;
        this.scheduler = scheduler;
        this.accountOperations = accountOperations;
        this.offlineContentStorage = offlineContentStorage;
    }

    public void subscribe() {
        disposable.add(userSessionStart().switchMap(trigger -> notifyStateChanges()).subscribeWith(LambdaObserver.onNext(subject::onNext)));
    }

    private Observable<Boolean> userSessionStart() {
        return eventBus
                .queue(EventQueue.CURRENT_USER_CHANGED)
                .map(CurrentUserChangedEvent::isUserUpdated)
                .startWith(accountOperations.isUserLoggedIn())
                .filter(isSessionStarted -> isSessionStarted);
    }

    private Observable<OfflineProperties> notifyStateChanges() {
        return loadOfflineStates()
                .toObservable()
                .startWith(new OfflineProperties())
                .switchMap(this::loadStateUpdates)
                .subscribeOn(scheduler);
    }

    private Observable<OfflineProperties> loadStateUpdates(OfflineProperties initialProperties) {
        return listenToUpdates().scan(initialProperties, this::reduce);
    }

    private OfflineProperties reduce(OfflineProperties properties, OfflineContentChangedEvent event) {
        return new OfflineProperties(updateEntitiesStates(properties, event), updateLikedStates(properties, event));
    }

    private Single<OfflineProperties> loadOfflineStates() {
        return Single.zip(
                trackDownloadsStorage.offlineStates(),
                loadPlaylistCollectionOfflineStates(),
                offlineStateOperations.loadLikedTrackState(),
                this::aggregateOfflineProperties
        );
    }

    private OfflineProperties aggregateOfflineProperties(Map<Urn, OfflineState> tracksOfflineStates, Map<Urn, OfflineState> playlistOfflineStates, OfflineState likedTracksState) {
        final Map<Urn, OfflineState> allOfflineStates = createMap();
        allOfflineStates.putAll(tracksOfflineStates);
        allOfflineStates.putAll(playlistOfflineStates);
        return new OfflineProperties(allOfflineStates, likedTracksState);
    }

    private Single<Map<Urn, OfflineState>> loadPlaylistCollectionOfflineStates() {
        return offlineContentStorage.getOfflinePlaylists()
                                    .flatMap(this::loadPlaylistsOfflineStatesSync);
    }

    private Single<Map<Urn, OfflineState>> loadPlaylistsOfflineStatesSync(List<Urn> playlists) {
        if (playlists.isEmpty()) {
            return Single.just(Collections.emptyMap());
        } else {
            //Purpose of this chain is to invert Map<OfflineState, Collection<Urn>> to Map<Urn, OfflineState>
            return offlineStateOperations.loadPlaylistsOfflineState(playlists)
                                         //Flat map it to observable of pairs of offline state to playlist urn
                                         .flatMapObservable(this::flattenMultimap)
                                         //Build a map of playlist urn -> offline state
                                         .scan(createMap(), this::addPairToMap)
                                         //The last value is the fully built map
                                         .lastOrError();
        }
    }

    private Observable<Pair<OfflineState, Urn>> flattenMultimap(Map<OfflineState, Collection<Urn>> map) {
        return Observable.fromIterable(map.entrySet())
                         .flatMap(entry -> Observable.fromIterable(entry.getValue())
                                                                        .map(urn -> Pair.of(entry.getKey(), urn)));
    }

    private Map<Urn, OfflineState> addPairToMap(Map<Urn, OfflineState> map, Pair<OfflineState, Urn> pair) {
        map.put(pair.second(), pair.first());
        return map;
    }

    private Map<Urn, OfflineState> createMap() {
        return new HashMap<>();
    }

    private Subject<OfflineContentChangedEvent> listenToUpdates() {
        return eventBus.queue(EventQueue.OFFLINE_CONTENT_CHANGED);
    }

    private HashMap<Urn, OfflineState> updateEntitiesStates(OfflineProperties properties, OfflineContentChangedEvent event) {
        final HashMap<Urn, OfflineState> updatedEntitiesStates = new HashMap<>(properties.getOfflineEntitiesStates());
        for (Urn urn : event.entities) {
            updatedEntitiesStates.put(urn, event.state);
        }
        return updatedEntitiesStates;
    }

    private OfflineState updateLikedStates(OfflineProperties properties, OfflineContentChangedEvent event) {
        if (event.isLikedTrackCollection) {
            return event.state;
        } else {
            return properties.getLikedTracksState();
        }
    }

    public Observable<OfflineProperties> states() {
        return subject.scan(OfflineProperties::apply).toFlowable(BackpressureStrategy.LATEST).toObservable();
    }

    public OfflineProperties latest() {
        OfflineProperties value = subject.getValue();
        return value == null ? new OfflineProperties() : value;
    }
}
