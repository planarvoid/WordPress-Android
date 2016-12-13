package com.soundcloud.android.playlists;

import static com.soundcloud.android.api.TestApiResponses.status;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.mockito.Mockito.when;
import static rx.Observable.just;

import com.soundcloud.android.api.ApiRequestException;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.sync.SyncInitiator;
import com.soundcloud.android.sync.SyncJobResult;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.android.testsupport.fixtures.ModelFixtures;
import com.soundcloud.android.testsupport.fixtures.TestSyncJobResults;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import rx.observers.TestSubscriber;
import rx.schedulers.Schedulers;
import rx.subjects.PublishSubject;

import java.util.List;


public class PlaylistRepositoryTest extends AndroidUnitTest {

    private PlaylistRepository playlistRepository;

    @Mock private SyncInitiator syncinitiator;
    @Mock private PlaylistStorage playlistStorage;

    private TestSubscriber<List<PlaylistItem>> subscriber =new TestSubscriber<>();
    private TestSubscriber<PlaylistItem> singleSubscriber =new TestSubscriber<>();
    private PublishSubject<SyncJobResult> syncSubject = PublishSubject.create();

    @Before
    public void setUp() throws Exception {
        playlistRepository = new PlaylistRepository(playlistStorage, syncinitiator, Schedulers.immediate());
    }

    @Test
    public void withUrnLoadsPlaylistFromStorage() {
        final PlaylistItem playlistItem = ModelFixtures.playlistItem();
        final List<Urn> urns = singletonList(playlistItem.getUrn());
        final List<PlaylistItem> playlists = singletonList(playlistItem);

        when(playlistStorage.availablePlaylists(urns)).thenReturn(just(urns));
        when(playlistStorage.loadPlaylists(urns)).thenReturn(just(playlists));

        playlistRepository.withUrn(playlistItem.getUrn()).subscribe(singleSubscriber);

        singleSubscriber.assertValue(playlistItem);
    }

    @Test
    public void withUrnBackfillsFromApi() {
        final PlaylistItem playlistItem = ModelFixtures.playlistItem();
        final List<Urn> urns = singletonList(playlistItem.getUrn());
        final List<PlaylistItem> playlists = singletonList(playlistItem);

        when(playlistStorage.availablePlaylists(urns)).thenReturn(just(emptyList()));
        when(playlistStorage.loadPlaylists(urns)).thenReturn(just(playlists));
        when(syncinitiator.batchSyncPlaylists(singletonList(playlistItem.getUrn()))).thenReturn(syncSubject);

        playlistRepository.withUrn(playlistItem.getUrn()).subscribe(singleSubscriber);

        singleSubscriber.assertNoValues();
        syncSubject.onNext(TestSyncJobResults.successWithChange());
        syncSubject.onCompleted();

        singleSubscriber.assertValue(playlistItem);
    }

    @Test
    public void withUrnEmitsErrorForSyncError() {
        final PlaylistItem playlistItem = ModelFixtures.playlistItem();
        final List<Urn> urns = singletonList(playlistItem.getUrn());

        when(playlistStorage.availablePlaylists(urns)).thenReturn(just(emptyList()));
        when(syncinitiator.batchSyncPlaylists(singletonList(playlistItem.getUrn()))).thenReturn(syncSubject);

        playlistRepository.withUrn(playlistItem.getUrn()).subscribe(singleSubscriber);

        singleSubscriber.assertNoValues();

        ApiRequestException exception = ApiRequestException.notFound(null, status(404));
        syncSubject.onError(exception);

        singleSubscriber.assertError(exception);
    }

    @Test
    public void withUrnsLoadsPlaylistFromStorage() {
        final PlaylistItem playlistItem = ModelFixtures.playlistItem();
        final List<Urn> urns = singletonList(playlistItem.getUrn());
        final List<PlaylistItem> playlists = singletonList(playlistItem);

        when(playlistStorage.availablePlaylists(urns)).thenReturn(just(urns));
        when(playlistStorage.loadPlaylists(urns)).thenReturn(just(playlists));

        playlistRepository.withUrns(urns).subscribe(subscriber);

        subscriber.assertValue(playlists);
    }

    @Test
    public void withUrnsBackfillMissingPlaylists() {
        final PlaylistItem playlistItemPresent = ModelFixtures.playlistItem();
        final PlaylistItem playlistItemToFetch = ModelFixtures.playlistItem();
        final List<Urn> urns = asList(playlistItemPresent.getUrn(), playlistItemToFetch.getUrn());
        final List<PlaylistItem> expectedPlaylists = asList(playlistItemPresent, playlistItemToFetch);

        when(playlistStorage.availablePlaylists(urns)).thenReturn(just(singletonList(playlistItemPresent.getUrn())));
        when(playlistStorage.loadPlaylists(urns)).thenReturn(just(expectedPlaylists));
        when(syncinitiator.batchSyncPlaylists(singletonList(playlistItemToFetch.getUrn()))).thenReturn(syncSubject);

        playlistRepository.withUrns(urns).subscribe(subscriber);

        subscriber.assertNoValues();

        syncSubject.onNext(TestSyncJobResults.successWithChange());
        syncSubject.onCompleted();

        subscriber.assertValue(expectedPlaylists);
    }

    @Test
    public void withUrnsReturnsAvailablePlaylistForSyncError() {
        final PlaylistItem playlistItemPresent = ModelFixtures.playlistItem();
        final PlaylistItem playlistItemToFetch = ModelFixtures.playlistItem();
        final List<Urn> urns = asList(playlistItemPresent.getUrn(), playlistItemToFetch.getUrn());
        final List<PlaylistItem> expectedPlaylists = asList(playlistItemPresent);

        when(playlistStorage.availablePlaylists(urns)).thenReturn(just(singletonList(playlistItemPresent.getUrn())));
        when(playlistStorage.loadPlaylists(urns)).thenReturn(just(expectedPlaylists));
        when(syncinitiator.batchSyncPlaylists(singletonList(playlistItemToFetch.getUrn()))).thenReturn(syncSubject);

        playlistRepository.withUrns(urns).subscribe(subscriber);

        subscriber.assertNoValues();

        syncSubject.onError(ApiRequestException.notFound(null, status(404)));
        syncSubject.onCompleted();

        subscriber.assertValue(expectedPlaylists);
    }
}
