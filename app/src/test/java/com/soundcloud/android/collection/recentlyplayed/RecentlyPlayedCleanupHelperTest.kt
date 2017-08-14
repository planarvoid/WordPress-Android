package com.soundcloud.android.collection.recentlyplayed

import com.nhaarman.mockito_kotlin.any
import com.nhaarman.mockito_kotlin.verify
import com.nhaarman.mockito_kotlin.whenever
import com.soundcloud.android.collection.playhistory.PlayHistoryRecord
import com.soundcloud.android.model.Urn
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner

@RunWith(MockitoJUnitRunner::class)
class RecentlyPlayedCleanupHelperTest {
    private lateinit var cleanupHelper: RecentlyPlayedCleanupHelper

    @Mock private lateinit var recentlyPlayedStorage: RecentlyPlayedStorage

    @Before
    fun setup() {
        cleanupHelper = RecentlyPlayedCleanupHelper(recentlyPlayedStorage)
    }

    @Test
    fun returnUsersToKeep() {
        whenever(recentlyPlayedStorage.loadContextIdsByType(any())).thenReturn(setOf(1))

        val usersToKeep = cleanupHelper.usersToKeep()

        assertThat(usersToKeep).containsOnly(Urn.forUser(1))
        verify(recentlyPlayedStorage).loadContextIdsByType(PlayHistoryRecord.CONTEXT_ARTIST)
    }

    @Test
    fun returnPlaylistsToKeep() {
        whenever(recentlyPlayedStorage.loadContextIdsByType(any())).thenReturn(setOf(1))

        val playlistsToKeep = cleanupHelper.playlistsToKeep()

        assertThat(playlistsToKeep).containsOnly(Urn.forPlaylist(1))
        verify(recentlyPlayedStorage).loadContextIdsByType(PlayHistoryRecord.CONTEXT_PLAYLIST)
    }
}


