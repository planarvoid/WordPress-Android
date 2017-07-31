package com.soundcloud.android.olddiscovery.recommendedplaylists

import com.soundcloud.android.api.model.ModelCollection
import com.soundcloud.android.model.Urn
import com.soundcloud.android.testsupport.StorageIntegrationTest
import org.assertj.core.api.Assertions
import org.junit.Before
import org.junit.Test

class RecommendedPlaylistsCleanupHelperTest : StorageIntegrationTest() {

    private lateinit var cleanupHelper: RecommendedPlaylistsCleanupHelper

    private lateinit var playlistUrn: Urn

    @Before
    fun setup() {
        cleanupHelper = RecommendedPlaylistsCleanupHelper(propeller())
        val playlist = testFixtures().insertPlaylist()
        playlistUrn = playlist.urn
        testFixtures().insertRecommendedPlaylist(ApiRecommendedPlaylistBucket.create("chilling", "Chilling", "artwork", ModelCollection(listOf(playlist))))
    }

    @Test
    fun returnsRecommendedPlaylistsToKeep() {
        val tracksToKeep = cleanupHelper.playlistsToKeep()

        Assertions.assertThat(tracksToKeep).containsOnly(playlistUrn)
    }
}
