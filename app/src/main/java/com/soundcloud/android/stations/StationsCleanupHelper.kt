package com.soundcloud.android.stations

import com.soundcloud.android.model.Urn
import com.soundcloud.android.storage.DefaultCleanupHelper
import com.soundcloud.android.storage.Tables
import com.soundcloud.propeller.PropellerDatabase
import com.soundcloud.propeller.query.Query
import javax.inject.Inject

class StationsCleanupHelper
@Inject constructor(private val propeller: PropellerDatabase) : DefaultCleanupHelper() {
    override fun tracksToKeep(): Set<Urn> {
        return propeller.query(Query.from(Tables.StationsPlayQueues.TABLE).select(Tables.StationsPlayQueues.TRACK_ID))
                .map { Urn.forTrack(it.getLong(Tables.StationsPlayQueues.TRACK_ID)) }
                .toSet()
    }
}
