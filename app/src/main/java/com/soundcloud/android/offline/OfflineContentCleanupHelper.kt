package com.soundcloud.android.offline

import com.soundcloud.android.model.Urn
import com.soundcloud.android.storage.DefaultCleanupHelper
import com.soundcloud.android.storage.Tables
import com.soundcloud.propeller.PropellerDatabase
import com.soundcloud.propeller.query.Query
import javax.inject.Inject

class OfflineContentCleanupHelper
@Inject constructor(private val propeller: PropellerDatabase) : DefaultCleanupHelper() {
    override fun playlistsToKeep(): Set<Urn> {
        return propeller.query(Query.from(Tables.OfflineContent.TABLE).select(Tables.OfflineContent._ID).whereEq(Tables.OfflineContent._TYPE, Tables.OfflineContent.TYPE_PLAYLIST))
                .map { Urn.forPlaylist(it.getLong(Tables.OfflineContent._ID)) }
                .toSet()
    }
}
