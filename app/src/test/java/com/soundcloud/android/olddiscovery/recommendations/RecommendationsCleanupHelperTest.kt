package com.soundcloud.android.olddiscovery.recommendations

import com.soundcloud.android.api.model.ModelCollection
import com.soundcloud.android.model.Urn
import com.soundcloud.android.testsupport.StorageIntegrationTest
import org.assertj.core.api.Assertions
import org.junit.After
import org.junit.Before
import org.junit.Test

class RecommendationsCleanupHelperTest : StorageIntegrationTest() {
    private lateinit var cleanupHelper: RecommendationsCleanupHelper

    private lateinit var seedTrackUrn: Urn
    private lateinit var recommendedTrackUrn: Urn

    @Before
    fun setup() {
        cleanupHelper = RecommendationsCleanupHelper(propeller())
        val seedTrack = testFixtures().insertTrack()
        seedTrackUrn = seedTrack.urn
        val recommendedTrack = testFixtures().insertTrack()
        recommendedTrackUrn = recommendedTrack.urn
        val apiRecommendation = ApiRecommendation(seedTrack, "liked", ModelCollection(listOf(recommendedTrack)))
        testFixtures().insertRecommendation(apiRecommendation, "soundcloud:query:urn", 0)
    }

    @Test
    fun returnRecommendedTracksToKeep() {
        val tracksToKeep = cleanupHelper.tracksToKeep()

        Assertions.assertThat(tracksToKeep).containsOnly(seedTrackUrn, recommendedTrackUrn)
    }

    @After
    fun tearDown() {

    }
}
