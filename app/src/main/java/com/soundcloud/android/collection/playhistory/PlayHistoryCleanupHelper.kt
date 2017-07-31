package com.soundcloud.android.collection.playhistory

import com.soundcloud.android.model.Urn
import com.soundcloud.android.storage.DefaultCleanupHelper
import com.soundcloud.android.storage.Tables
import com.soundcloud.propeller.PropellerDatabase
import com.soundcloud.propeller.query.Query
import javax.inject.Inject

class PlayHistoryCleanupHelper
@Inject constructor(private val propeller: PropellerDatabase) : DefaultCleanupHelper() {

    override fun tracksToKeep(): Set<Urn> {
        return propeller.query(Query.from(Tables.PlayHistory.TABLE).select(Tables.PlayHistory.TRACK_ID))
                .map { Urn.forTrack(it.getLong(Tables.PlayHistory.TRACK_ID)) }
                .toSet()
    }
}
