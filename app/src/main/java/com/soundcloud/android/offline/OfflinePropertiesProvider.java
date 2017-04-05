package com.soundcloud.android.offline;

import static com.soundcloud.android.rx.observers.DefaultSubscriber.fireAndForget;
import static com.soundcloud.java.collections.Lists.transform;

import com.soundcloud.android.ApplicationModule;
import com.soundcloud.android.accounts.AccountOperations;
import com.soundcloud.android.collection.playlists.MyPlaylistsOperations;
import com.soundcloud.android.collection.playlists.PlaylistsOptions;
import com.soundcloud.android.events.CurrentUserChangedEvent;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playlists.Playlist;
import com.soundcloud.rx.eventbus.EventBus;
import rx.Observable;
import rx.Scheduler;
import rx.subjects.BehaviorSubject;
import rx.subjects.Subject;

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
    private final EventBus eventBus;
    private final Scheduler scheduler;
    private final AccountOperations accountOperations;
    private final BehaviorSubject<OfflineProperties> subject = BehaviorSubject.create();

    @Inject
    public OfflinePropertiesProvider(TrackDownloadsStorage trackDownloadsStorage,
                                     OfflineStateOperations offlineStateOperations,
                                     MyPlaylistsOperations myPlaylistsOperations,
                                     EventBus eventBus,
                                     @Named(ApplicationModule.HIGH_PRIORITY) Scheduler scheduler,
                                     AccountOperations accountOperations) {
        this.trackDownloadsStorage = trackDownloadsStorage;
        this.offlineStateOperations = offlineStateOperations;
        this.myPlaylistsOperations = myPlaylistsOperations;
        this.eventBus = eventBus;
        this.scheduler = scheduler;
        this.accountOperations = accountOperations;
    }

    public void subscribe() {
        fireAndForget(userSessionStart().switchMap(trigger -> notifyStateChanges()));
    }

    private Observable<Boolean> userSessionStart() {
        return eventBus
                .queue(EventQueue.CURRENT_USER_CHANGED).asObservable()
                .map(CurrentUserChangedEvent::isUserUpdated)
                .startWith(accountOperations.isUserLoggedIn())
                .filter(isSessionStarted -> isSessionStarted);
    }

    private Observable<OfflineProperties> notifyStateChanges() {
        return loadOfflineStates()
                .startWith(OfflineProperties.empty())
                .switchMap(this::loadStateUpdates)
                .doOnNext(this::publishSnapshot)
                .subscribeOn(scheduler);
    }

    private Observable<OfflineProperties> loadStateUpdates(OfflineProperties initialProperties) {
        return listenToUpdates().scan(initialProperties, this::reduce);
    }

    private OfflineProperties reduce(OfflineProperties properties, OfflineContentChangedEvent event) {
        return OfflineProperties.from(updateEntitiesStates(properties, event), updateLikedStates(properties, event));
    }

    private Observable<OfflineProperties> loadOfflineStates() {
        return Observable.zip(
                trackDownloadsStorage.getOfflineStates(),
                loadPlaylistCollectionOfflineStates(),
                this::aggregateOfflineProperties
        );
    }

    private OfflineProperties aggregateOfflineProperties(Map<Urn, OfflineState> tracksOfflineStates, Map<Urn, OfflineState> playlistOfflineStates) {
        final OfflineState likedTracksState = offlineStateOperations.loadLikedTrackState();

        final HashMap<Urn, OfflineState> allOfflineStates = new HashMap<>();
        allOfflineStates.putAll(tracksOfflineStates);
        allOfflineStates.putAll(playlistOfflineStates);

        return OfflineProperties.from(allOfflineStates, likedTracksState);
    }

    private Observable<Map<Urn, OfflineState>> loadPlaylistCollectionOfflineStates() {
        return myPlaylistsOperations.myPlaylists(PlaylistsOptions.OFFLINE_ONLY)
                                    .map(this::loadPlaylistsOfflineStatesSync);
    }

    private Map<Urn, OfflineState> loadPlaylistsOfflineStatesSync(List<Playlist> playlists) {
        final List<Urn> playlistUrns = transform(playlists, Playlist::urn);
        final Map<OfflineState, Collection<Urn>> playlistOfflineStates = offlineStateOperations.loadPlaylistsOfflineState(playlistUrns);
        final Map<Urn, OfflineState> playlistToState = new HashMap<>();

        for (OfflineState state : playlistOfflineStates.keySet()) {
            final Collection<Urn> urns = playlistOfflineStates.get(state);
            for (Urn playlist : urns) {
                playlistToState.put(playlist, state);
            }
        }
        return playlistToState;
    }

    private Subject<OfflineContentChangedEvent, OfflineContentChangedEvent> listenToUpdates() {
        return eventBus.queue(EventQueue.OFFLINE_CONTENT_CHANGED);
    }

    private HashMap<Urn, OfflineState> updateEntitiesStates(OfflineProperties properties, OfflineContentChangedEvent event) {
        final HashMap<Urn, OfflineState> updatedEntitiesStates = new HashMap<>(properties.offlineEntitiesStates());
        for (Urn urn : event.entities) {
            updatedEntitiesStates.put(urn, event.state);
        }
        return updatedEntitiesStates;
    }

    private OfflineState updateLikedStates(OfflineProperties properties, OfflineContentChangedEvent event) {
        if (event.isLikedTrackCollection) {
            return event.state;
        } else {
            return properties.likedTracksState();
        }
    }

    private void publishSnapshot(OfflineProperties newOfflineProperties) {
        subject.onNext(newOfflineProperties);
    }

    public Observable<OfflineProperties> states() {
        return subject.asObservable();
    }

}
