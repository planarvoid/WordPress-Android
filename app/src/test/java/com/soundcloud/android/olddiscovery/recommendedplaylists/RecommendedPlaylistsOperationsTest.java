package com.soundcloud.android.olddiscovery.recommendedplaylists;

import static java.util.Collections.singletonList;
import static java.util.Collections.singletonMap;
import static org.mockito.Mockito.when;

import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playlists.Playlist;
import com.soundcloud.android.playlists.PlaylistRepository;
import com.soundcloud.android.sync.NewSyncOperations;
import com.soundcloud.android.sync.SyncResult;
import com.soundcloud.android.sync.Syncable;
import com.soundcloud.android.testsupport.fixtures.ModelFixtures;
import io.reactivex.Maybe;
import io.reactivex.Single;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.List;

@RunWith(MockitoJUnitRunner.class)
public class RecommendedPlaylistsOperationsTest {

    @Mock private NewSyncOperations syncOperations;
    @Mock private RecommendedPlaylistsStorage playlistsStorage;
    @Mock private PlaylistRepository playlistRepository;

    private RecommendedPlaylistsOperations operations;

    @Before
    public void setUp() throws Exception {
        operations = new RecommendedPlaylistsOperations(syncOperations,
                                                        playlistsStorage,
                                                        playlistRepository,
                                                        ModelFixtures.entityItemCreator());
    }

    @Test
    public void loadRecommendedPlaylists_emptyEntities() throws Exception {
        when(syncOperations.lazySyncIfStale(Syncable.RECOMMENDED_PLAYLISTS)).thenReturn(Single.just(SyncResult.synced()));
        when(playlistsStorage.recommendedPlaylists()).thenReturn(Maybe.just(singletonList(RecommendedPlaylistsFixtures.createEmptyEntity())));

        operations.recommendedPlaylists()
                  .test()
                  .assertComplete()
                  .assertNoErrors()
                  .assertNoValues();
    }

    @Test
    public void loadRecommendedPlaylists() throws Exception {
        Playlist playlist = ModelFixtures.playlist();
        List<Urn> urns = singletonList(playlist.urn());
        RecommendedPlaylistsEntity recommendedPlaylistEntity = RecommendedPlaylistsFixtures.createEntity(urns);

        when(syncOperations.lazySyncIfStale(Syncable.RECOMMENDED_PLAYLISTS)).thenReturn(Single.just(SyncResult.synced()));
        when(playlistsStorage.recommendedPlaylists()).thenReturn(Maybe.just(singletonList(recommendedPlaylistEntity)));
        when(playlistRepository.withUrns(urns)).thenReturn(Single.just(singletonMap(playlist.urn(), playlist)));

        operations.recommendedPlaylists()
                  .test()
                  .assertComplete()
                  .assertNoErrors()
                  .assertValue(RecommendedPlaylistsBucketItem.create(recommendedPlaylistEntity, singletonList(ModelFixtures.playlistItem(playlist))));
    }
}
