package com.soundcloud.android.offline;

import static com.soundcloud.java.collections.Lists.transform;

import com.soundcloud.android.ApplicationModule;
import com.soundcloud.android.collection.playlists.MyPlaylistsOperations;
import com.soundcloud.android.collection.playlists.PlaylistsOptions;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playlists.Playlist;
import com.soundcloud.android.rx.observers.DefaultSubscriber;
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
    private final BehaviorSubject<OfflineProperties> subject = BehaviorSubject.create();
    private OfflineProperties offlineProperties = OfflineProperties.empty();

    @Inject
    public OfflinePropertiesProvider(TrackDownloadsStorage trackDownloadsStorage,
                                     OfflineStateOperations offlineStateOperations,
                                     MyPlaylistsOperations myPlaylistsOperations,
                                     EventBus eventBus,
                                     @Named(ApplicationModule.HIGH_PRIORITY) Scheduler scheduler) {
        this.trackDownloadsStorage = trackDownloadsStorage;
        this.offlineStateOperations = offlineStateOperations;
        this.myPlaylistsOperations = myPlaylistsOperations;
        this.eventBus = eventBus;
        this.scheduler = scheduler;
    }

    public void subscribe() {
        publishSnapshot();

        loadOfflineStates()
                .subscribeOn(scheduler)
                .doOnNext(offlineStates -> {
                    store(offlineStates);
                    publishSnapshot();
                })
                .subscribe(new DefaultSubscriber<>());

        listenToUpdates()
                .doOnNext(event -> {
                    update(event);
                    publishSnapshot();
                })
                .subscribe(new DefaultSubscriber<>());
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

    private void store(OfflineProperties offlineProperties) {
        this.offlineProperties = offlineProperties;
    }

    private void update(OfflineContentChangedEvent event) {
        // Note : because it publishes immutable snapshots, we make a copy of the states before updating.
        // Is this problem w.r.t memory management?
        offlineProperties = OfflineProperties.from(updateEntitiesStates(event), updateLikedStates(event));
    }

    private HashMap<Urn, OfflineState> updateEntitiesStates(OfflineContentChangedEvent event) {
        final HashMap<Urn, OfflineState> updatedEntitiesStates = new HashMap<>(offlineProperties.offlineEntitiesStates());
        for (Urn urn : event.entities) {
            updatedEntitiesStates.put(urn, event.state);
        }
        return updatedEntitiesStates;
    }

    private OfflineState updateLikedStates(OfflineContentChangedEvent event) {
        if (event.isLikedTrackCollection) {
            return event.state;
        } else {
            return offlineProperties.likedTracksState();
        }
    }

    private void publishSnapshot() {
        subject.onNext(offlineProperties);
    }

    public Observable<OfflineProperties> states() {
        return subject.asObservable();
    }

}
