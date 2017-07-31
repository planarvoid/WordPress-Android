package com.soundcloud.android.stations

import com.soundcloud.android.testsupport.StorageIntegrationTest
import org.assertj.core.api.Assertions
import org.junit.Before
import org.junit.Test

class StationsCleanupHelperTest : StorageIntegrationTest() {

    private lateinit var cleanupHelper: StationsCleanupHelper

    @Before
    fun setup() {
        cleanupHelper = StationsCleanupHelper(propeller())
    }

    @Test
    fun returnTracksToKeep() {
        val insertStation = testFixtures().insertStation()

        val tracksToKeep = cleanupHelper.tracksToKeep()

        Assertions.assertThat(tracksToKeep).containsExactlyElementsOf(insertStation.trackRecords.map { it.urn })
    }
}
