package com.soundcloud.android.collection.recentlyplayed

import com.nhaarman.mockito_kotlin.whenever
import com.soundcloud.android.collection.playhistory.PlayHistoryRecord
import com.soundcloud.android.model.Urn
import com.soundcloud.android.playlists.PlaylistRepository
import com.soundcloud.android.stations.StationMetadata
import com.soundcloud.android.stations.StationsRepository
import com.soundcloud.android.sync.NewSyncOperations
import com.soundcloud.android.sync.SyncResult
import com.soundcloud.android.sync.Syncable
import com.soundcloud.android.testsupport.fixtures.ModelFixtures
import com.soundcloud.android.users.UserRepository
import com.soundcloud.java.optional.Optional
import io.reactivex.Single
import io.reactivex.schedulers.Schedulers
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.*
import org.mockito.junit.MockitoJUnitRunner

@RunWith(MockitoJUnitRunner::class)
class RecentlyPlayedOperationsTest {

    @Mock private lateinit var recentlyPlayedStorage: RecentlyPlayedStorage
    @Mock private lateinit var syncOperations: NewSyncOperations
    @Mock private lateinit var clearRecentlyPlayedCommand: ClearRecentlyPlayedCommand
    @Mock private lateinit var stationsRepository: StationsRepository
    @Mock private lateinit var userRepository: UserRepository
    @Mock private lateinit var playlistRepository: PlaylistRepository

    private lateinit var operations: RecentlyPlayedOperations

    private val scheduler = Schedulers.trampoline()

    private val userUrn = Urn.forUser(1)
    private val artistStationUrn = Urn.forArtistStation(1)
    private val trackStationUrn = Urn.forTrackStation(1)
    private val playlistUrn = Urn.forPlaylist(1)

    private val userPlayHistoryRecord = PlayHistoryRecord.forRecentlyPlayed(100, userUrn)
    private val artistStationPlayHistoryRecord = PlayHistoryRecord.forRecentlyPlayed(200, artistStationUrn)
    private val trackStationPlayHistoryRecord = PlayHistoryRecord.forRecentlyPlayed(300, trackStationUrn)
    private val playlistPlayHistoryRecord = PlayHistoryRecord.forRecentlyPlayed(400, playlistUrn)

    private val user = ModelFixtures.userBuilder()
            .urn(userUrn)
            .username("username")
            .avatarUrl(Optional.absent())
            .build();
    private val artistStation = StationMetadata.builder()
            .urn(artistStationUrn)
            .title("artist-station-title")
            .permalink(Optional.absent())
            .type("track")
            .imageUrlTemplate(Optional.absent())
            .build()
    private val trackStation = StationMetadata.builder()
            .urn(trackStationUrn)
            .title("track-station-title")
            .permalink(Optional.absent())
            .type("track")
            .imageUrlTemplate(Optional.absent())
            .build()
    private val playlist = ModelFixtures.playlistBuilder()
            .urn(playlistUrn)
            .title("playlist-title")
            .imageUrlTemplate(Optional.absent())
            .trackCount(5)
            .isAlbum(false)
            .offlineState(Optional.absent())
            .isLikedByCurrentUser(true)
            .isPrivate(true)
            .build()

    private val userPlayableItem = RecentlyPlayedPlayableItem.forUser(userUrn, "username", Optional.absent(), 100, false)
    private val artistStationPlayableItem = RecentlyPlayedPlayableItem.forStation(artistStationUrn, "artist-station-title", Optional.absent(), 200)
    private val trackStationPlayableItem = RecentlyPlayedPlayableItem.forStation(trackStationUrn, "track-station-title", Optional.absent(), 300)
    private val playlistPlayableItem = RecentlyPlayedPlayableItem.forPlaylist(playlistUrn,
            Optional.absent(),
            "playlist-title",
            5,
            false,
            Optional.absent(),
            true,
            true,
            400)

    @Before
    fun setUp() {
        whenever(syncOperations.lazySyncIfStale(Syncable.RECENTLY_PLAYED)).thenReturn(Single.just(SyncResult.noOp()))

        operations = RecentlyPlayedOperations(recentlyPlayedStorage,
                scheduler,
                syncOperations,
                clearRecentlyPlayedCommand,
                userRepository,
                playlistRepository,
                stationsRepository)
    }

    @Test
    fun shouldAggregateMedataFromRepositories() {
        givenStorageReturnsProperData()
        givenRepositoriesReturnProperData()

        operations.recentlyPlayed()
                .test()
                .assertValue(listOf(playlistPlayableItem, trackStationPlayableItem, artistStationPlayableItem, userPlayableItem))

        verify(stationsRepository).stationsMetadata(listOf(trackStationUrn, artistStationUrn))
        verify(userRepository).usersInfo(listOf(userUrn))
        verify(playlistRepository).loadPlaylistsByUrn(listOf(playlistUrn))
    }

    @Test
    fun shouldIgnoreRepositoriesFailures() {
        givenStorageReturnsProperData()
        givenRepositoriesReturnException()

        operations.recentlyPlayed()
                .doOnSuccess { it.size }
                .test()
                .assertValue(listOf())
                .assertNoErrors()

        verify(stationsRepository).stationsMetadata(listOf(trackStationUrn, artistStationUrn))
        verify(userRepository).usersInfo(listOf(userUrn))
        verify(playlistRepository).loadPlaylistsByUrn(listOf(playlistUrn))
    }

    fun givenStorageReturnsProperData() {
        val recentlyPlayed = Single.just(listOf(userPlayHistoryRecord, trackStationPlayHistoryRecord, artistStationPlayHistoryRecord, playlistPlayHistoryRecord))
        whenever(recentlyPlayedStorage.loadRecentlyPlayed(anyInt())).thenReturn(recentlyPlayed)
    }

    fun givenRepositoriesReturnProperData() {
        whenever(userRepository.usersInfo(anyList())).thenReturn(Single.just(listOf(user)))
        whenever(stationsRepository.stationsMetadata(anyList())).thenReturn(Single.just(listOf(artistStation, trackStation)))
        whenever(playlistRepository.loadPlaylistsByUrn(anyList())).thenReturn(Single.just(listOf(playlist)))
    }

    fun givenRepositoriesReturnException() {
        whenever(userRepository.usersInfo(anyList())).thenReturn(Single.error(RuntimeException()))
        whenever(stationsRepository.stationsMetadata(anyList())).thenReturn(Single.error(RuntimeException()))
        whenever(playlistRepository.loadPlaylistsByUrn(anyList())).thenReturn(Single.error(RuntimeException()))
    }

}
