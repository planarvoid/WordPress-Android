package com.soundcloud.android.collection.playhistory

import com.soundcloud.android.model.Urn
import com.soundcloud.android.testsupport.StorageIntegrationTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test

class PlayHistoryCleanupHelperTest : StorageIntegrationTest() {
    private lateinit var cleanupHelper: PlayHistoryCleanupHelper

    @Before
    fun setup() {
        cleanupHelper = PlayHistoryCleanupHelper(propeller())
    }

    @Test
    fun returnsTracksToKeep() {
        val trackUrn = Urn.forTrack(12L)
        testFixtures().insertPlayHistory(123L, trackUrn)

        val tracksToKeep = cleanupHelper.tracksToKeep()

        assertThat(tracksToKeep).containsOnly(trackUrn)
    }
}
