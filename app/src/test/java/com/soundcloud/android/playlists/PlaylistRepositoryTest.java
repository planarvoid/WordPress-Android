package com.soundcloud.android.playlists;

import static com.soundcloud.android.api.TestApiResponses.status;
import static com.soundcloud.java.collections.Maps.asMap;
import static io.reactivex.Single.just;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.when;

import com.soundcloud.android.api.ApiRequestException;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.storage.RepositoryMissedSyncEvent;
import com.soundcloud.android.sync.SyncInitiator;
import com.soundcloud.android.sync.SyncJobResult;
import com.soundcloud.android.testsupport.fixtures.ModelFixtures;
import com.soundcloud.android.testsupport.fixtures.TestSyncJobResults;
import com.soundcloud.rx.eventbus.TestEventBusV2;
import io.reactivex.Single;
import io.reactivex.observers.TestObserver;
import io.reactivex.schedulers.Schedulers;
import io.reactivex.subjects.SingleSubject;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.List;
import java.util.Map;

@RunWith(MockitoJUnitRunner.class)
public class PlaylistRepositoryTest {

    private PlaylistRepository playlistRepository;

    @Mock private SyncInitiator syncinitiator;
    @Mock private PlaylistStorage playlistStorage;

    private TestEventBusV2 eventBus = new TestEventBusV2();
    private SingleSubject<SyncJobResult> syncSubject = SingleSubject.create();

    @Before
    public void setUp() throws Exception {
        playlistRepository = new PlaylistRepository(playlistStorage, syncinitiator, Schedulers.trampoline(), eventBus);
        when(playlistStorage.loadPlaylists(anyList())).thenReturn(Single.just(emptyList()));
        when(playlistStorage.availablePlaylists(anyList())).thenReturn(Single.just(emptyList()));
    }

    @Test
    public void withUrnLoadsPlaylistFromStorage() {
        final Playlist playlist = ModelFixtures.playlist();
        final List<Urn> urns = singletonList(playlist.urn());
        final List<Playlist> playlists = singletonList(playlist);

        when(playlistStorage.availablePlaylists(urns)).thenReturn(just(urns));
        when(playlistStorage.loadPlaylists(urns)).thenReturn(just(playlists));

        playlistRepository.withUrn(playlist.urn())
                          .test()
                          .assertValue(playlist);
        eventBus.verifyNoEventsOn(EventQueue.TRACKING);
    }

    @Test
    public void withUrnBackfillsFromApi() {
        final Playlist playlist = ModelFixtures.playlist();
        final List<Urn> urns = singletonList(playlist.urn());
        final List<Playlist> playlists = singletonList(playlist);

        when(playlistStorage.availablePlaylists(urns)).thenReturn(just(emptyList()));
        when(playlistStorage.loadPlaylists(urns)).thenReturn(just(playlists));
        when(syncinitiator.syncPlaylist(playlist.urn())).thenReturn(syncSubject);

        final TestObserver<Playlist> test = playlistRepository.withUrn(playlist.urn()).test();

        test.assertNoValues();
        syncSubject.onSuccess(TestSyncJobResults.successWithChange());

        test.assertValue(playlist);
        eventBus.verifyNoEventsOn(EventQueue.TRACKING);
    }

    @Test
    public void withUrnEmitsErrorForSyncError() {
        final Playlist playlist = ModelFixtures.playlist();
        final List<Urn> urns = singletonList(playlist.urn());

        when(playlistStorage.availablePlaylists(urns)).thenReturn(just(emptyList()));
        when(syncinitiator.syncPlaylist(playlist.urn())).thenReturn(syncSubject);

        final TestObserver<Playlist> subscriber = playlistRepository.withUrn(playlist.urn()).test();
        subscriber.assertNoValues();

        ApiRequestException exception = ApiRequestException.notFound(null, status(404));
        syncSubject.onError(exception);

        subscriber.assertError(exception);
    }

    @Test
    public void withUrnEmitsEmptyObservableForUnavailablePlaylist() {
        final Playlist playlist = ModelFixtures.playlist();
        final List<Urn> urns = singletonList(playlist.urn());

        when(playlistStorage.availablePlaylists(urns)).thenReturn(just(emptyList()));
        when(syncinitiator.syncPlaylist(playlist.urn())).thenReturn(syncSubject);

        final TestObserver<Playlist> subscriber = playlistRepository.withUrn(playlist.urn()).test();
        subscriber.assertNoValues();

        syncSubject.onSuccess(TestSyncJobResults.successWithChange());

        subscriber.assertNoValues();
        subscriber.assertComplete();
        assertMissingPlaylistEventSent();
    }

    @Test
    public void withUrnsLoadsPlaylistFromStorage() {
        final Playlist playlist = ModelFixtures.playlist();
        final List<Urn> urns = singletonList(playlist.urn());
        final List<Playlist> playlists = singletonList(playlist);

        when(playlistStorage.availablePlaylists(urns)).thenReturn(just(urns));
        when(playlistStorage.loadPlaylists(urns)).thenReturn(just(playlists));

        playlistRepository.withUrns(urns)
                          .test()
                          .assertValue(asMap(playlists, Playlist::urn));
        eventBus.verifyNoEventsOn(EventQueue.TRACKING);
    }

    @Test
    public void withUrnsBackfillMissingPlaylists() {
        final Playlist playlistPresent = ModelFixtures.playlist();
        final Playlist playlistToFetch = ModelFixtures.playlist();
        final List<Urn> urns = asList(playlistPresent.urn(), playlistToFetch.urn());
        final List<Playlist> expectedPlaylists = asList(playlistPresent, playlistToFetch);

        when(playlistStorage.availablePlaylists(urns)).thenReturn(just(singletonList(playlistPresent.urn())));
        when(playlistStorage.loadPlaylists(urns)).thenReturn(just(expectedPlaylists));
        when(syncinitiator.batchSyncPlaylists(singletonList(playlistToFetch.urn()))).thenReturn(syncSubject);

        final TestObserver<Map<Urn, Playlist>> subscriber = playlistRepository.withUrns(urns).test();

        subscriber.assertNoValues();

        syncSubject.onSuccess(TestSyncJobResults.successWithChange());

        subscriber.assertValue(asMap(expectedPlaylists, Playlist::urn));
        eventBus.verifyNoEventsOn(EventQueue.TRACKING);
    }

    @Test
    public void withUrnsReturnsAvailablePlaylistForSyncError() {
        final Playlist playlistPresent = ModelFixtures.playlist();
        final Playlist playlistToFetch = ModelFixtures.playlist();
        final List<Urn> urns = asList(playlistPresent.urn(), playlistToFetch.urn());
        final List<Playlist> expectedPlaylists = asList(playlistPresent);

        when(playlistStorage.availablePlaylists(urns)).thenReturn(just(singletonList(playlistPresent.urn())));
        when(playlistStorage.loadPlaylists(urns)).thenReturn(just(expectedPlaylists));
        when(syncinitiator.batchSyncPlaylists(singletonList(playlistToFetch.urn()))).thenReturn(syncSubject);

        final TestObserver<Map<Urn, Playlist>> subscriber = playlistRepository.withUrns(urns).test();

        subscriber.assertNoValues();

        syncSubject.onError(ApiRequestException.notFound(null, status(404)));

        subscriber.assertValue(asMap(expectedPlaylists, Playlist::urn));
        assertMissingPlaylistEventSent();
    }

    private void assertMissingPlaylistEventSent() {
        final RepositoryMissedSyncEvent trackingEvent = ((RepositoryMissedSyncEvent) eventBus.lastEventOn(EventQueue.TRACKING));
        assertThat(trackingEvent.getPlaylistsMissing()).isEqualTo(1);
    }
}
