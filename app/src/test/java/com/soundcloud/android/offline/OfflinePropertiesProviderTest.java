package com.soundcloud.android.offline;

import static com.soundcloud.android.events.CurrentUserChangedEvent.forUserUpdated;
import static com.soundcloud.android.offline.OfflineContentChangedEvent.downloading;
import static com.soundcloud.android.offline.OfflineContentChangedEvent.requested;
import static com.soundcloud.android.offline.OfflineState.REQUESTED;
import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;
import static java.util.Collections.singletonList;
import static java.util.Collections.singletonMap;
import static org.mockito.Mockito.when;

import com.soundcloud.android.accounts.AccountOperations;
import com.soundcloud.android.collection.playlists.MyPlaylistsOperations;
import com.soundcloud.android.collection.playlists.PlaylistsOptions;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playlists.Playlist;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.android.testsupport.fixtures.ModelFixtures;
import com.soundcloud.rx.eventbus.TestEventBus;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import rx.Observable;
import rx.schedulers.Schedulers;
import rx.subjects.PublishSubject;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class OfflinePropertiesProviderTest extends AndroidUnitTest {

    @Mock private TrackDownloadsStorage trackDownloadsStorage;
    @Mock private OfflineStateOperations offlineStateOperations;
    @Mock private MyPlaylistsOperations myPlaylistsOperations;
    @Mock private AccountOperations accountOperations;

    private OfflinePropertiesProvider provider;
    private PublishSubject<Map<Urn, OfflineState>> offlineTracksStatesLoader = PublishSubject.create();
    private PublishSubject<List<Playlist>> offlinePlaylistsLoader = PublishSubject.create();
    private PublishSubject<OfflineState> offlineLikesTracksStateLoader = PublishSubject.create();
    private TestEventBus eventBus = new TestEventBus();

    @Before
    public void setUp() throws Exception {
        provider = new OfflinePropertiesProvider(
                trackDownloadsStorage,
                offlineStateOperations,
                myPlaylistsOperations,
                eventBus,
                Schedulers.immediate(),
                accountOperations
        );

        when(trackDownloadsStorage.getOfflineStates()).thenReturn(offlineTracksStatesLoader);
        when(myPlaylistsOperations.myPlaylists(PlaylistsOptions.OFFLINE_ONLY)).thenReturn(offlinePlaylistsLoader);
        when(trackDownloadsStorage.getLikesOfflineState()).thenReturn(offlineLikesTracksStateLoader);
        when(offlineStateOperations.loadLikedTrackState()).thenReturn(OfflineState.NOT_OFFLINE);
        when(accountOperations.isUserLoggedIn()).thenReturn(true);
    }

    @Test
    public void doesNotPublishBeforeSubscribed() {
        provider.states()
                .test()
                .assertNoValues();
    }

    @Test
    public void loadInitialStatesWhenSubscribing() {
        final Urn track = Urn.forTrack(1L);
        final Playlist playlist = ModelFixtures.playlist();

        provider.subscribe();

        storageEmitsOfflineTracks(track, REQUESTED);
        storageEmitsOfflinePlaylist(playlist, OfflineState.DOWNLOADED);
        storageEmitsOfflineTracksLiked(OfflineState.NOT_OFFLINE);

        final HashMap<Urn, OfflineState> expectedEntitiesStates = new HashMap<>();
        expectedEntitiesStates.put(track, REQUESTED);
        expectedEntitiesStates.put(playlist.urn(), OfflineState.DOWNLOADED);

        provider.states()
                .test()
                .assertValue(OfflineProperties.from(expectedEntitiesStates, OfflineState.NOT_OFFLINE));
    }

    @Test
    public void doesntLoadOnSubscribeIfNotLoggedIn() throws Exception {
        when(accountOperations.isUserLoggedIn()).thenReturn(false);

        provider.subscribe();

        provider.states()
                .test()
                .assertNoValues();
    }

    @Test
    public void loadsWhenUserLogsIn() {
        when(accountOperations.isUserLoggedIn()).thenReturn(false);

        provider.subscribe();
        eventBus.publish(EventQueue.CURRENT_USER_CHANGED, forUserUpdated(Urn.forUser(123L)));

        provider.states()
                .test()
                .assertValue(OfflineProperties.empty());
    }

    @Test
    public void resetStatesWhenNewUserLogsIn() {
        final Urn track = Urn.forTrack(124L);
        final Playlist playlist = ModelFixtures.playlist();

        provider.subscribe();

        storageEmitsOfflineTracks(track, REQUESTED);
        storageEmitsOfflinePlaylist(playlist, OfflineState.DOWNLOADED);
        storageEmitsOfflineTracksLiked(OfflineState.NOT_OFFLINE);
        
        final HashMap<Urn, OfflineState> expectedEntitiesStates = new HashMap<>();
        expectedEntitiesStates.put(track, REQUESTED);
        expectedEntitiesStates.put(playlist.urn(), OfflineState.DOWNLOADED);
        provider.states()
                .test()
                .assertValue(OfflineProperties.from(expectedEntitiesStates, OfflineState.NOT_OFFLINE));

        resetStorage();
        eventBus.publish(EventQueue.CURRENT_USER_CHANGED, forUserUpdated(Urn.forUser(123L)));
        provider.states()
                .test()
                .assertValue(OfflineProperties.from(emptyMap(), OfflineState.NOT_OFFLINE));
    }

    private void resetStorage() {
        when(trackDownloadsStorage.getOfflineStates()).thenReturn(Observable.empty());
        when(myPlaylistsOperations.myPlaylists(PlaylistsOptions.OFFLINE_ONLY)).thenReturn(Observable.empty());
        when(trackDownloadsStorage.getLikesOfflineState()).thenReturn(Observable.empty());
        when(offlineStateOperations.loadLikedTrackState()).thenReturn(OfflineState.NOT_OFFLINE);
    }

    @Test
    public void notifiesStatesUpdate() {
        final HashMap<Urn, OfflineState> states = new HashMap<>();

        provider.subscribe();

        // Initial state
        storageEmitsOfflineTracks();
        storageEmitsOfflinePlaylist();
        storageEmitsOfflineTracksLiked(OfflineState.NOT_OFFLINE);

        provider.states()
                .test()
                .assertValue(OfflineProperties.from(emptyMap(), OfflineState.NOT_OFFLINE));


        // Likes update
        eventBus.publish(EventQueue.OFFLINE_CONTENT_CHANGED, requested(emptyList(), true));
        provider.states()
                .test()
                .assertValue(OfflineProperties.from(emptyMap(), REQUESTED));

        // Playlist update
        eventBus.publish(EventQueue.OFFLINE_CONTENT_CHANGED, requested(singletonList(Urn.forPlaylist(1L)), false));
        states.put(Urn.forPlaylist(1L), REQUESTED);
        provider.states()
                .test()
                .assertValue(OfflineProperties.from(states, REQUESTED));

        // Track Update
        eventBus.publish(EventQueue.OFFLINE_CONTENT_CHANGED, downloading(singletonList(Urn.forTrack(2L)), false));
        states.put(Urn.forTrack(2L), OfflineState.DOWNLOADING);
        provider.states()
                .test()
                .assertValue(OfflineProperties.from(states, REQUESTED));
    }

    private void storageEmitsOfflineTracksLiked(OfflineState state) {
        offlineLikesTracksStateLoader.onNext(state);
    }

    private void storageEmitsOfflinePlaylist() {
        offlinePlaylistsLoader.onCompleted();
    }

    private void storageEmitsOfflineTracks() {
        offlineTracksStatesLoader.onCompleted();
    }

    private void storageEmitsOfflinePlaylist(Playlist playlist, OfflineState state) {
        setUpOfflinePlaylist(playlist, state);
        offlinePlaylistsLoader.onNext(singletonList(playlist));
    }

    private void storageEmitsOfflineTracks(Urn track, OfflineState state) {
        offlineTracksStatesLoader.onNext(singletonMap(track, state));
    }

    private void setUpOfflinePlaylist(Playlist playlist, OfflineState state) {
        final List<Urn> urns = singletonList(playlist.urn());
        final Map<OfflineState, Collection<Urn>> stateToEntitites = singletonMap(state, urns);
        when(offlineStateOperations.loadPlaylistsOfflineState(urns)).thenReturn(stateToEntitites);
    }
}
