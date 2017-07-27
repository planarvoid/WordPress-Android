package com.soundcloud.android.likes

import com.soundcloud.android.model.Urn
import com.soundcloud.android.testsupport.StorageIntegrationTest
import org.assertj.core.api.Assertions
import org.junit.Before
import org.junit.Test
import java.util.*

class LikeCleanupHelperTest : StorageIntegrationTest() {

    private lateinit var cleanupHelper: LikeCleanupHelper

    private lateinit var likedTrack: Urn
    private lateinit var likedPlaylist: Urn

    @Before
    fun setup() {
        cleanupHelper = LikeCleanupHelper(propeller())
        likedTrack = testFixtures().insertLikedTrack(Date()).urn
        likedPlaylist = testFixtures().insertLikedPlaylist(Date()).urn
    }

    @Test
    fun returnsLikedTrackToKeep() {
        val tracksToKeep = cleanupHelper.tracksToKeep

        Assertions.assertThat(tracksToKeep).containsOnly(likedTrack)
    }

    @Test
    fun returnsLikedPlaylistToKeep() {
        val playlistsToKeep = cleanupHelper.playlistsToKeep

        Assertions.assertThat(playlistsToKeep).containsOnly(likedPlaylist)
    }
}
