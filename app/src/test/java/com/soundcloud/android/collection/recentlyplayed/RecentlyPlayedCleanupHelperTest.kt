package com.soundcloud.android.collection.recentlyplayed

import com.soundcloud.android.model.Urn
import com.soundcloud.android.testsupport.StorageIntegrationTest
import org.assertj.core.api.Assertions
import org.junit.Before
import org.junit.Test

class RecentlyPlayedCleanupHelperTest : StorageIntegrationTest() {
    private lateinit var cleanupHelper: RecentlyPlayedCleanupHelper

    @Before
    fun setup() {
        cleanupHelper = RecentlyPlayedCleanupHelper(propeller())
    }

    @Test
    fun returnUsersToKeep() {
        val userUrn = Urn.forUser(2L)
        testFixtures().insertRecentlyPlayed(2L, userUrn)

        val usersToKeep = cleanupHelper.usersToKeep()

        Assertions.assertThat(usersToKeep).containsOnly(userUrn)
    }

    @Test
    fun returnPlaylistsToKeep() {
        val playlistUrn = Urn.forPlaylist(3L)
        testFixtures().insertRecentlyPlayed(3L, playlistUrn)

        val playlistsToKeep = cleanupHelper.playlistsToKeep()

        Assertions.assertThat(playlistsToKeep).containsOnly(playlistUrn)
    }
}


