package com.soundcloud.android.olddiscovery.charts

import com.soundcloud.android.model.Urn
import com.soundcloud.android.storage.DefaultCleanupHelper
import com.soundcloud.android.storage.Tables.ChartTracks
import com.soundcloud.propeller.PropellerDatabase
import com.soundcloud.propeller.query.Query
import javax.inject.Inject

class ChartsCleanupHelper
@Inject constructor(private val propeller: PropellerDatabase) : DefaultCleanupHelper() {
    override fun getTracksToKeep(): MutableSet<Urn> {
        return propeller.query(Query.from(ChartTracks.TABLE).select(ChartTracks.TRACK_ID)).map({ Urn.forTrack(it.getLong(ChartTracks.TRACK_ID)) }).toMutableSet()
    }
}
