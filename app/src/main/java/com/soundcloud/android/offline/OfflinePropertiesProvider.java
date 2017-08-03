package com.soundcloud.android.offline;

import static com.soundcloud.java.collections.Lists.transform;

import com.soundcloud.android.ApplicationModule;
import com.soundcloud.android.accounts.AccountOperations;
import com.soundcloud.android.collection.playlists.MyPlaylistsOperations;
import com.soundcloud.android.collection.playlists.PlaylistsOptions;
import com.soundcloud.android.events.CurrentUserChangedEvent;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playlists.Playlist;
import com.soundcloud.android.rx.observers.LambdaObserver;
import com.soundcloud.java.collections.Pair;
import com.soundcloud.rx.eventbus.EventBusV2;
import io.reactivex.BackpressureStrategy;
import io.reactivex.Maybe;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Singleton
public class OfflinePropertiesProvider {

    private final TrackDownloadsStorage trackDownloadsStorage;
    private final OfflineStateOperations offlineStateOperations;
    private final MyPlaylistsOperations myPlaylistsOperations;
    private final EventBusV2 eventBus;
    private final Scheduler scheduler;
    private final AccountOperations accountOperations;
    private final BehaviorSubject<OfflineProperties> subject = BehaviorSubject.create();

    @SuppressLint("sc.MissingCompositeDisposableRecycle")
    private final CompositeDisposable disposable = new CompositeDisposable();

    @Inject
    public OfflinePropertiesProvider(TrackDownloadsStorage trackDownloadsStorage,
                                     OfflineStateOperations offlineStateOperations,
                                     MyPlaylistsOperations myPlaylistsOperations,
                                     EventBusV2 eventBus,
                                     @Named(ApplicationModule.RX_HIGH_PRIORITY) Scheduler scheduler,
                                     AccountOperations accountOperations) {
        this.trackDownloadsStorage = trackDownloadsStorage;
        this.offlineStateOperations = offlineStateOperations;
        this.myPlaylistsOperations = myPlaylistsOperations;
        this.eventBus = eventBus;
        this.scheduler = scheduler;
        this.accountOperations = accountOperations;
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

    private Maybe<OfflineProperties> loadOfflineStates() {
        return Maybe.zip(
                trackDownloadsStorage.getOfflineStates().toMaybe(),
                loadPlaylistCollectionOfflineStates(),
                offlineStateOperations.loadLikedTrackState().toMaybe(),
                this::aggregateOfflineProperties
        );
    }

    private OfflineProperties aggregateOfflineProperties(Map<Urn, OfflineState> tracksOfflineStates, Map<Urn, OfflineState> playlistOfflineStates, OfflineState likedTracksState) {
        final Map<Urn, OfflineState> allOfflineStates = createMap();
        allOfflineStates.putAll(tracksOfflineStates);
        allOfflineStates.putAll(playlistOfflineStates);
        return new OfflineProperties(allOfflineStates, likedTracksState);
    }

    private Maybe<Map<Urn, OfflineState>> loadPlaylistCollectionOfflineStates() {
        return myPlaylistsOperations.myPlaylists(PlaylistsOptions.OFFLINE_ONLY)
                                    .flatMapSingleElement(this::loadPlaylistsOfflineStatesSync);
    }

    private Single<Map<Urn, OfflineState>> loadPlaylistsOfflineStatesSync(List<Playlist> playlists) {
        final List<Urn> playlistUrns = transform(playlists, Playlist::urn);
        //Purpose of this chain is to invert Map<OfflineState, Collection<Urn>> to Map<Urn, OfflineState>
        return offlineStateOperations.loadPlaylistsOfflineState(playlistUrns)
                                     //Flat map it to observable of pairs of offline state to playlist urn
                                     .flatMapObservable(this::flattenMultimap)
                                     //Build a map of playlist urn -> offline state
                                     .scan(createMap(), this::addPairToMap)
                                     //The last value is the fully built map
                                     .lastOrError();
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
}
