package com.soundcloud.android.olddiscovery.charts

import com.soundcloud.android.api.model.ChartCategory
import com.soundcloud.android.api.model.ChartType
import com.soundcloud.android.api.model.ModelCollection
import com.soundcloud.android.model.Urn
import com.soundcloud.android.sync.charts.ApiChart
import com.soundcloud.android.sync.charts.ApiImageResource
import com.soundcloud.android.testsupport.StorageIntegrationTest
import org.assertj.core.api.Assertions
import org.junit.Before
import org.junit.Test
import java.util.*

class ChartsCleanupHelperTest : StorageIntegrationTest() {
    private lateinit var cleanupHelper: ChartsCleanupHelper

    private val track1 = Urn.forTrack(1L)
    private val track2 = Urn.forTrack(2L)
    private val tracks = ModelCollection(mutableListOf<ApiImageResource>(ApiImageResource.create(track1, null), ApiImageResource.create(track2, null)))

    @Before
    fun setup() {
        cleanupHelper = ChartsCleanupHelper(propeller())
        testFixtures().insertChart(ApiChart<ApiImageResource>("chart", Urn.forGenre("soundcloud:genre:rock"), ChartType.NONE, ChartCategory.NONE, Date(), tracks), 0)
    }

    @Test
    fun returnTracksFromChartsToKeep() {
        val tracksToKeep = cleanupHelper.tracksToKeep()

        Assertions.assertThat(tracksToKeep).containsOnly(track1, track2)
    }
}
