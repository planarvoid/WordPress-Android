package com.soundcloud.android.olddiscovery.recommendedplaylists

import com.nhaarman.mockito_kotlin.whenever
import com.soundcloud.android.playlists.PlaylistItem
import com.soundcloud.android.playlists.PlaylistRepository
import com.soundcloud.android.sync.NewSyncOperations
import com.soundcloud.android.sync.SyncResult
import com.soundcloud.android.sync.Syncable
import com.soundcloud.android.testsupport.AndroidUnitTest
import com.soundcloud.android.testsupport.fixtures.ModelFixtures
import io.reactivex.Maybe
import io.reactivex.Single
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import java.util.Collections.singletonMap

class RecommendedPlaylistsOperationsKotlinTest : AndroidUnitTest() {

    @Mock private lateinit var syncOperations: NewSyncOperations
    @Mock private lateinit var playlistsStorage: RecommendedPlaylistsStorage
    @Mock private lateinit var playlistRepository: PlaylistRepository

    private lateinit var operations: RecommendedPlaylistsOperations

    @Before
    @Throws(Exception::class)
    fun setUp() {
        operations = RecommendedPlaylistsOperations(syncOperations,
                playlistsStorage,
                playlistRepository,
                ModelFixtures.entityItemCreator())
    }

    @Test
    fun loadRecommendedPlaylists_emptyEntities() {
        whenever(syncOperations.lazySyncIfStale(Syncable.RECOMMENDED_PLAYLISTS)).thenReturn(Single.just(SyncResult.synced()))
        whenever(playlistsStorage.recommendedPlaylists()).thenReturn(Maybe.just(listOf(RecommendedPlaylistsFixtures.createEmptyEntity())))

        operations.recommendedPlaylists()
                .test()
                .assertComplete()
                .assertNoErrors()
                .assertNoValues()
    }

    @Test
    fun loadRecommendedPlaylists() {
        val playlist = ModelFixtures.playlist()
        val urns = listOf(playlist.urn())
        val recommendedPlaylistEntity = RecommendedPlaylistsFixtures.createEntity(urns)

        whenever(syncOperations.lazySyncIfStale(Syncable.RECOMMENDED_PLAYLISTS)).thenReturn(Single.just(SyncResult.synced()))
        whenever(playlistsStorage.recommendedPlaylists()).thenReturn(Maybe.just(listOf(recommendedPlaylistEntity)))
        whenever(playlistRepository.withUrns(urns)).thenReturn(Single.just(singletonMap(playlist.urn(), playlist)))

        operations.recommendedPlaylists()
                .test()
                .assertComplete()
                .assertNoErrors()
                .assertValue(RecommendedPlaylistsBucketItem.create(recommendedPlaylistEntity, listOf<PlaylistItem>(ModelFixtures.playlistItem(playlist))))
    }
}
