package com.soundcloud.android.collection.playhistory

import com.nhaarman.mockito_kotlin.whenever
import com.soundcloud.android.model.Urn
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner

@RunWith(MockitoJUnitRunner::class)
class PlayHistoryCleanupHelperTest {
    @Mock private lateinit var playHistoryStorage: PlayHistoryStorage
    private lateinit var cleanupHelper: PlayHistoryCleanupHelper

    @Before
    fun setup() {
        cleanupHelper = PlayHistoryCleanupHelper(playHistoryStorage)
    }

    @Test
    fun returnsTracksToKeep() {
        val trackUrn = Urn.forTrack(1L)
        val contextUrn = Urn.forTrack(2L)
        whenever(playHistoryStorage.loadAll()).thenReturn(listOf(PlayHistoryRecord.create(1L, trackUrn, contextUrn)))

        val tracksToKeep = cleanupHelper.tracksToKeep()

        assertThat(tracksToKeep).containsOnly(trackUrn, contextUrn)
    }

    @Test
    fun returnsPlaylistsToKeep() {
        val trackUrn = Urn.forTrack(1L)
        val contextUrn = Urn.forPlaylist(2L)
        whenever(playHistoryStorage.loadAll()).thenReturn(listOf(PlayHistoryRecord.create(1L, trackUrn, contextUrn)))

        val playlistsToKeep = cleanupHelper.playlistsToKeep()

        assertThat(playlistsToKeep).containsOnly(contextUrn)
    }
}
