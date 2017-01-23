package com.soundcloud.android.offline;

import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;
import static java.util.Collections.singletonList;
import static java.util.Collections.singletonMap;
import static org.mockito.Mockito.when;

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

    private OfflinePropertiesProvider provider;
    private PublishSubject<Map<Urn, OfflineState>> offlineTracksStates = PublishSubject.create();
    private PublishSubject<List<Playlist>> offlinePlaylists = PublishSubject.create();
    private PublishSubject<OfflineState> offlineLikesTracksState = PublishSubject.create();
    private TestEventBus eventBus = new TestEventBus();

    @Before
    public void setUp() throws Exception {
        provider = new OfflinePropertiesProvider(
                trackDownloadsStorage,
                offlineStateOperations,
                myPlaylistsOperations,
                eventBus,
                Schedulers.immediate()
        );

        when(trackDownloadsStorage.getOfflineStates()).thenReturn(offlineTracksStates);
        when(myPlaylistsOperations.myPlaylists(PlaylistsOptions.OFFLINE_ONLY)).thenReturn(offlinePlaylists);
        when(trackDownloadsStorage.getLikesOfflineState()).thenReturn(offlineLikesTracksState);
        when(offlineStateOperations.loadLikedTrackState()).thenReturn(OfflineState.NOT_OFFLINE);
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

        publishOfflineTracks(track, OfflineState.REQUESTED);
        publishOfflinePlaylist(playlist, OfflineState.DOWNLOADED);
        publishOfflineTracksLiked(OfflineState.NOT_OFFLINE);

        final HashMap<Urn, OfflineState> expectedEntitiesStates = new HashMap<>();
        expectedEntitiesStates.put(track, OfflineState.REQUESTED);
        expectedEntitiesStates.put(playlist.urn(), OfflineState.DOWNLOADED);

        provider.states()
                .test()
                .assertValue(OfflineProperties.from(expectedEntitiesStates, OfflineState.NOT_OFFLINE));
    }

    @Test
    public void notifiesStatesUpdate() {
        final HashMap<Urn, OfflineState> states = new HashMap<>();

        provider.subscribe();

        // Initial state
        publishOfflineTracks();
        publishOfflinePlaylist();
        publishOfflineTracksLiked(OfflineState.NOT_OFFLINE);

        provider.states()
                .test()
                .assertValue(OfflineProperties.from(emptyMap(), OfflineState.NOT_OFFLINE));


        // Likes update
        eventBus.publish(EventQueue.OFFLINE_CONTENT_CHANGED, OfflineContentChangedEvent.requested(emptyList(), true));
        provider.states()
                .test()
                .assertValue(OfflineProperties.from(emptyMap(), OfflineState.REQUESTED));

        // Playlist update
        eventBus.publish(EventQueue.OFFLINE_CONTENT_CHANGED, OfflineContentChangedEvent.requested(singletonList(Urn.forPlaylist(1L)), false));
        states.put(Urn.forPlaylist(1L), OfflineState.REQUESTED);
        provider.states()
                .test()
                .assertValue(OfflineProperties.from(states, OfflineState.REQUESTED));

        // Track Update
        eventBus.publish(EventQueue.OFFLINE_CONTENT_CHANGED, OfflineContentChangedEvent.downloading(singletonList(Urn.forTrack(2L)), false));
        states.put(Urn.forTrack(2L), OfflineState.DOWNLOADING);
        provider.states()
                .test()
                .assertValue(OfflineProperties.from(states, OfflineState.REQUESTED));
    }

    private void publishOfflineTracksLiked(OfflineState state) {
        offlineLikesTracksState.onNext(state);
    }

    private void publishOfflinePlaylist() {
        offlinePlaylists.onCompleted();
    }

    private void publishOfflineTracks() {
        offlineTracksStates.onCompleted();
    }

    private void publishOfflinePlaylist(Playlist playlist, OfflineState state) {
        setUpOfflinePlaylist(playlist, state);
        offlinePlaylists.onNext(singletonList(playlist));
    }

    private void publishOfflineTracks(Urn track, OfflineState state) {
        offlineTracksStates.onNext(singletonMap(track, state));
    }

    private void setUpOfflinePlaylist(Playlist playlist, OfflineState state) {
        final List<Urn> urns = singletonList(playlist.urn());
        final Map<OfflineState, Collection<Urn>> stateToEntitites = singletonMap(state, urns);
        when(offlineStateOperations.loadPlaylistsOfflineState(urns)).thenReturn(stateToEntitites);
    }
}
