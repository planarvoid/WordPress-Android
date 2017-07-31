package com.soundcloud.android.profile

import com.soundcloud.android.api.model.ApiPlaylist
import com.soundcloud.android.api.model.ApiTrack
import com.soundcloud.android.testsupport.StorageIntegrationTest
import org.assertj.core.api.Assertions
import org.junit.Before
import org.junit.Test
import java.util.*

class PostsCleanupHelperTest : StorageIntegrationTest() {

    private lateinit var cleanupHelper: PostsCleanupHelper

    private lateinit var postedTrack: ApiTrack
    private lateinit var postedPlaylist: ApiPlaylist

    @Before
    fun setup() {
        cleanupHelper = PostsCleanupHelper(propeller())
        postedTrack = testFixtures().insertPostedTrack(Date(), false)
        postedPlaylist = testFixtures().insertPostedPlaylist(Date())
    }

    @Test
    fun returnsTrackToKeep() {
        val tracksToKeep = cleanupHelper.tracksToKeep

        Assertions.assertThat(tracksToKeep).containsOnly(postedTrack.urn)
    }

    @Test
    fun returnsPlaylistToKeep() {
        val playlistsToKeep = cleanupHelper.playlistsToKeep

        Assertions.assertThat(playlistsToKeep).containsOnly(postedPlaylist.urn)
    }
}
