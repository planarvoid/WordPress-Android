package com.soundcloud.android.offline;

import static com.soundcloud.android.events.CurrentUserChangedEvent.forUserUpdated;
import static com.soundcloud.android.offline.OfflineContentChangedEvent.downloading;
import static com.soundcloud.android.offline.OfflineContentChangedEvent.requested;
import static com.soundcloud.android.offline.OfflineState.REQUESTED;
import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;
import static java.util.Collections.singletonList;
import static java.util.Collections.singletonMap;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.google.common.collect.Maps;
import com.soundcloud.android.accounts.AccountOperations;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playlists.Playlist;
import com.soundcloud.android.testsupport.fixtures.ModelFixtures;
import com.soundcloud.rx.eventbus.TestEventBusV2;
import io.reactivex.Single;
import io.reactivex.schedulers.Schedulers;
import io.reactivex.subjects.SingleSubject;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import rx.subjects.PublishSubject;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RunWith(MockitoJUnitRunner.class)
public class OfflinePropertiesProviderTest {

    @Mock private TrackDownloadsStorage trackDownloadsStorage;
    @Mock private OfflineStateOperations offlineStateOperations;
    @Mock private AccountOperations accountOperations;
    @Mock private OfflineContentStorage offlineContentStorage;

    private OfflinePropertiesProvider provider;
    private SingleSubject<Map<Urn, OfflineState>> offlineTracksStatesLoader;
    private PublishSubject<OfflineState> offlineLikesTracksStateLoader;
    private TestEventBusV2 eventBus = new TestEventBusV2();

    @Before
    public void setUp() throws Exception {
        offlineTracksStatesLoader = SingleSubject.create();
        offlineLikesTracksStateLoader = PublishSubject.create();
        provider = new OfflinePropertiesProvider(
                trackDownloadsStorage,
                offlineStateOperations,
                eventBus,
                Schedulers.trampoline(),
                accountOperations,
                offlineContentStorage);

        when(trackDownloadsStorage.offlineStates()).thenReturn(offlineTracksStatesLoader);
        when(offlineStateOperations.loadLikedTrackState()).thenReturn(Single.just(OfflineState.NOT_OFFLINE));
        when(accountOperations.isUserLoggedIn()).thenReturn(true);
        when(offlineContentStorage.getOfflinePlaylists()).thenReturn(Single.just(emptyList()));
    }

    @Test
    public void doesNotPublishBeforeSubscribed() {
        provider.states()
                .test()
                .assertNoValues();
    }

    @Test
    public void latestReturnsEmptyPropertiesIfNotLoaded() throws Exception {
        assertThat(provider.latest()).isEqualTo(new OfflineProperties());
    }

    @Test
    public void loadInitialStatesWhenSubscribing() {
        final Urn track = Urn.forTrack(1L);
        final Playlist playlist = ModelFixtures.playlist();

        storageEmitsOfflineTracks(track, REQUESTED);
        storageEmitsOfflinePlaylist(playlist, OfflineState.DOWNLOADED);
        storageEmitsOfflineTracksLiked(OfflineState.NOT_OFFLINE);

        provider.subscribe();

        final HashMap<Urn, OfflineState> expectedEntitiesStates = new HashMap<>();
        expectedEntitiesStates.put(track, REQUESTED);
        expectedEntitiesStates.put(playlist.urn(), OfflineState.DOWNLOADED);

        provider.states()
                .test()
                .assertValue(new OfflineProperties(expectedEntitiesStates, OfflineState.NOT_OFFLINE));
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
                .assertValue(new OfflineProperties());
    }

    @Test
    public void resetStatesWhenNewUserLogsIn() {
        final Urn track = Urn.forTrack(124L);
        final Playlist playlist = ModelFixtures.playlist();

        storageEmitsOfflineTracks(track, REQUESTED);
        storageEmitsOfflinePlaylist(playlist, OfflineState.DOWNLOADED);
        storageEmitsOfflineTracksLiked(OfflineState.NOT_OFFLINE);

        provider.subscribe();
        
        final HashMap<Urn, OfflineState> expectedEntitiesStates = new HashMap<>();
        expectedEntitiesStates.put(track, REQUESTED);
        expectedEntitiesStates.put(playlist.urn(), OfflineState.DOWNLOADED);
        provider.states()
                .test()
                .assertValue(new OfflineProperties(expectedEntitiesStates, OfflineState.NOT_OFFLINE));

        resetStorage();
        eventBus.publish(EventQueue.CURRENT_USER_CHANGED, forUserUpdated(Urn.forUser(123L)));
        provider.states()
                .test()
                .assertValue(new OfflineProperties(emptyMap(), OfflineState.NOT_OFFLINE));
    }

    private void resetStorage() {
        when(trackDownloadsStorage.offlineStates()).thenReturn(Single.never());
        when(offlineContentStorage.getOfflinePlaylists()).thenReturn(Single.just(emptyList()));
    }

    @Test
    public void notifiesStatesUpdate() {
        final HashMap<Urn, OfflineState> states = new HashMap<>();

        // Initial state
        storageEmitsOfflineTracks();
        storageEmitsOfflineTracksLiked(OfflineState.NOT_OFFLINE);

        provider.subscribe();

        provider.states()
                .test()
                .assertValue(new OfflineProperties(emptyMap(), OfflineState.NOT_OFFLINE));


        // Likes update
        eventBus.publish(EventQueue.OFFLINE_CONTENT_CHANGED, requested(emptyList(), true));
        provider.states()
                .test()
                .assertValue(new OfflineProperties(emptyMap(), REQUESTED));

        // Playlist update
        eventBus.publish(EventQueue.OFFLINE_CONTENT_CHANGED, requested(singletonList(Urn.forPlaylist(1L)), false));
        states.put(Urn.forPlaylist(1L), REQUESTED);
        provider.states()
                .test()
                .assertValue(new OfflineProperties(states, REQUESTED));

        // Track Update
        eventBus.publish(EventQueue.OFFLINE_CONTENT_CHANGED, downloading(singletonList(Urn.forTrack(2L)), false));
        states.put(Urn.forTrack(2L), OfflineState.DOWNLOADING);
        provider.states()
                .test()
                .assertValue(new OfflineProperties(states, REQUESTED));
    }

    private void storageEmitsOfflineTracksLiked(OfflineState state) {
        offlineLikesTracksStateLoader.onNext(state);
    }

    private void storageEmitsOfflineTracks() {
        offlineTracksStatesLoader.onSuccess(Maps.newHashMap());
    }

    private void storageEmitsOfflinePlaylist(Playlist playlist, OfflineState state) {
        setUpOfflinePlaylist(playlist, state);
        when(offlineContentStorage.getOfflinePlaylists()).thenReturn(Single.just(singletonList(playlist.urn())));
    }

    private void storageEmitsOfflineTracks(Urn track, OfflineState state) {
        offlineTracksStatesLoader.onSuccess(singletonMap(track, state));
    }

    private void setUpOfflinePlaylist(Playlist playlist, OfflineState state) {
        final List<Urn> urns = singletonList(playlist.urn());
        final Map<OfflineState, Collection<Urn>> stateToEntitites = singletonMap(state, urns);
        when(offlineStateOperations.loadPlaylistsOfflineState(urns)).thenReturn(Single.just(stateToEntitites));
    }
}
