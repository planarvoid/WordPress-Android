package com.soundcloud.android.discovery.recommendedplaylists;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.soundcloud.android.discovery.DiscoveryItem;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playlists.PlaylistItem;
import com.soundcloud.android.playlists.PlaylistOperations;
import com.soundcloud.android.sync.SyncOperations;
import com.soundcloud.android.sync.Syncable;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.android.testsupport.fixtures.ModelFixtures;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import rx.Observable;
import rx.observers.TestSubscriber;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;

public class RecommendedPlaylistsOperationsTest extends AndroidUnitTest {

    @Mock private SyncOperations syncOperations;
    @Mock private RecommendedPlaylistsStorage playlistsStorage;
    @Mock private PlaylistOperations playlistOperations;

    private RecommendedPlaylistsOperations operations;
    private TestSubscriber<DiscoveryItem> testSubscriber = new TestSubscriber<>();

    @Before
    public void setUp() throws Exception {
        operations = new RecommendedPlaylistsOperations(syncOperations, playlistsStorage, playlistOperations);
    }

    @Test
    public void loadRecommendedPlaylists_emptyEntities() throws Exception {
        when(syncOperations.lazySyncIfStale(Syncable.RECOMMENDED_PLAYLISTS)).thenReturn(Observable.just(SyncOperations.Result.SYNCED));
        when(playlistsStorage.recommendedPlaylists()).thenReturn(Observable.just(Collections.singletonList(RecommendedPlaylistsFixtures.createEmptyEntity())));

        operations.recommendedPlaylists().subscribe(testSubscriber);

        testSubscriber.assertCompleted();
        testSubscriber.assertNoErrors();
        testSubscriber.assertNoValues();
    }

    @Test
    public void loadRecommendedPlaylists() throws Exception {
        PlaylistItem item = ModelFixtures.create(PlaylistItem.class);
        List<Urn> urns = Collections.singletonList(item.getUrn());
        RecommendedPlaylistsEntity recommendedPlaylistEntity = RecommendedPlaylistsFixtures.createEntity(urns);

        when(syncOperations.lazySyncIfStale(Syncable.RECOMMENDED_PLAYLISTS)).thenReturn(Observable.just(SyncOperations.Result.SYNCED));
        when(playlistsStorage.recommendedPlaylists()).thenReturn(Observable.just(Collections.singletonList(recommendedPlaylistEntity)));
        when(playlistOperations.playlistsMap(new HashSet<>(urns))).thenReturn(Observable.just(Collections.singletonMap(item.getUrn(), item)));

        operations.recommendedPlaylists().subscribe(testSubscriber);

        testSubscriber.assertCompleted();
        testSubscriber.assertNoErrors();
        testSubscriber.assertReceivedOnNext(Collections.<DiscoveryItem>singletonList(
                RecommendedPlaylistsBucketItem.create(recommendedPlaylistEntity, Collections.singletonList(item))
        ));
    }
}
