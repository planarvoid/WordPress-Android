package com.soundcloud.android.offline

import com.soundcloud.android.api.model.ApiPlaylist
import com.soundcloud.android.testsupport.StorageIntegrationTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test

class OfflineContentCleanupHelperTest : StorageIntegrationTest() {
    private lateinit var offlinePlaylist: ApiPlaylist
    private lateinit var offlineContentCleanupHelper: OfflineContentCleanupHelper

    @Before
    fun setUp() {
        offlineContentCleanupHelper = OfflineContentCleanupHelper(propeller())
    }

    @Test
    fun returnsPlaylistsToKeep() {
        //Offline likes
        testFixtures().insertLikesMarkedForOfflineSync()
        //Offline playlist
        offlinePlaylist = testFixtures().insertPlaylistMarkedForOfflineSync()
        //Playlist not marked for offline
        testFixtures().insertPlaylist()

        val playlistsToKeep = offlineContentCleanupHelper.playlistsToKeep

        assertThat(playlistsToKeep).containsOnly(offlinePlaylist.urn)
    }
}
