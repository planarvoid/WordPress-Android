package com.soundcloud.android.discovery.recommendedplaylists;

import static java.util.Collections.singletonList;
import static org.mockito.Mockito.when;

import com.soundcloud.android.discovery.DiscoveryItem;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playlists.PlaylistItem;
import com.soundcloud.android.playlists.PlaylistRepository;
import com.soundcloud.android.sync.SyncOperations;
import com.soundcloud.android.sync.Syncable;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.android.testsupport.fixtures.ModelFixtures;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import rx.Observable;
import rx.observers.TestSubscriber;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;

public class RecommendedPlaylistsOperationsTest extends AndroidUnitTest {

    @Mock private SyncOperations syncOperations;
    @Mock private RecommendedPlaylistsStorage playlistsStorage;
    @Mock private PlaylistRepository playlistRepository;

    private RecommendedPlaylistsOperations operations;
    private TestSubscriber<DiscoveryItem> testSubscriber = new TestSubscriber<>();

    @Before
    public void setUp() throws Exception {
        operations = new RecommendedPlaylistsOperations(syncOperations, playlistsStorage,
                                                        playlistRepository);
    }

    @Test
    public void loadRecommendedPlaylists_emptyEntities() throws Exception {
        when(syncOperations.lazySyncIfStale(Syncable.RECOMMENDED_PLAYLISTS)).thenReturn(Observable.just(SyncOperations.Result.SYNCED));
        when(playlistsStorage.recommendedPlaylists()).thenReturn(Observable.just(singletonList(RecommendedPlaylistsFixtures.createEmptyEntity())));

        operations.recommendedPlaylists().subscribe(testSubscriber);

        testSubscriber.assertCompleted();
        testSubscriber.assertNoErrors();
        testSubscriber.assertNoValues();
    }

    @Test
    public void loadRecommendedPlaylists() throws Exception {
        PlaylistItem item = ModelFixtures.create(PlaylistItem.class);
        List<Urn> urns = singletonList(item.getUrn());
        RecommendedPlaylistsEntity recommendedPlaylistEntity = RecommendedPlaylistsFixtures.createEntity(urns);

        when(syncOperations.lazySyncIfStale(Syncable.RECOMMENDED_PLAYLISTS)).thenReturn(Observable.just(SyncOperations.Result.SYNCED));
        when(playlistsStorage.recommendedPlaylists()).thenReturn(Observable.just(singletonList(recommendedPlaylistEntity)));
        when(playlistRepository.withUrns(new HashSet<>(urns))).thenReturn(Observable.just(singletonList(item)));

        operations.recommendedPlaylists().subscribe(testSubscriber);

        testSubscriber.assertCompleted();
        testSubscriber.assertNoErrors();
        testSubscriber.assertReceivedOnNext(Collections.<DiscoveryItem>singletonList(
                RecommendedPlaylistsBucketItem.create(recommendedPlaylistEntity, singletonList(item))
        ));
    }
}
