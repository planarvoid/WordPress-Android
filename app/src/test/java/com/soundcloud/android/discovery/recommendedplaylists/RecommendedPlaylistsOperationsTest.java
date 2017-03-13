package com.soundcloud.android.discovery.recommendedplaylists;

import static java.util.Collections.singletonList;
import static java.util.Collections.singletonMap;
import static org.mockito.Mockito.when;

import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playlists.Playlist;
import com.soundcloud.android.playlists.PlaylistRepository;
import com.soundcloud.android.sync.SyncOperations;
import com.soundcloud.android.sync.Syncable;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.android.testsupport.fixtures.ModelFixtures;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import rx.Observable;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;

public class RecommendedPlaylistsOperationsTest extends AndroidUnitTest {

    @Mock private SyncOperations syncOperations;
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
        when(syncOperations.lazySyncIfStale(Syncable.RECOMMENDED_PLAYLISTS)).thenReturn(Observable.just(SyncOperations.Result.SYNCED));
        when(playlistsStorage.recommendedPlaylists()).thenReturn(Observable.just(singletonList(RecommendedPlaylistsFixtures.createEmptyEntity())));

        operations.recommendedPlaylists()
                  .test()
                  .assertCompleted()
                  .assertNoErrors()
                  .assertNoValues();
    }

    @Test
    public void loadRecommendedPlaylists() throws Exception {
        Playlist playlist = ModelFixtures.playlist();
        List<Urn> urns = singletonList(playlist.urn());
        RecommendedPlaylistsEntity recommendedPlaylistEntity = RecommendedPlaylistsFixtures.createEntity(urns);

        when(syncOperations.lazySyncIfStale(Syncable.RECOMMENDED_PLAYLISTS)).thenReturn(Observable.just(SyncOperations.Result.SYNCED));
        when(playlistsStorage.recommendedPlaylists()).thenReturn(Observable.just(singletonList(recommendedPlaylistEntity)));
        when(playlistRepository.withUrns(new HashSet<>(urns))).thenReturn(Observable.just(singletonMap(playlist.urn(), playlist)));

        operations.recommendedPlaylists()
                  .test()
                  .assertCompleted()
                  .assertNoErrors()
                  .assertReceivedOnNext(Collections.singletonList(
                          RecommendedPlaylistsBucketItem.create(recommendedPlaylistEntity, singletonList(ModelFixtures.playlistItem(playlist)))
                  ));
    }
}
