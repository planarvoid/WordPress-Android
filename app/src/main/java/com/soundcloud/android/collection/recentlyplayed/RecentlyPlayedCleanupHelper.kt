package com.soundcloud.android.collection.recentlyplayed

import com.soundcloud.android.collection.playhistory.PlayHistoryRecord
import com.soundcloud.android.model.Urn
import com.soundcloud.android.storage.DefaultCleanupHelper
import com.soundcloud.android.storage.Tables
import com.soundcloud.propeller.PropellerDatabase
import com.soundcloud.propeller.query.Query
import javax.inject.Inject

class RecentlyPlayedCleanupHelper
@Inject constructor(private val propeller: PropellerDatabase) : DefaultCleanupHelper() {

    override fun usersToKeep(): Set<Urn> {
        return propeller.query(Query.from(Tables.RecentlyPlayed.TABLE).select(Tables.RecentlyPlayed.CONTEXT_ID).whereEq(Tables.RecentlyPlayed.CONTEXT_TYPE, PlayHistoryRecord.CONTEXT_ARTIST))
                .map { Urn.forUser(it.getLong(Tables.RecentlyPlayed.CONTEXT_ID)) }
                .toSet()
    }

    override fun playlistsToKeep(): Set<Urn> {
        return propeller.query(Query.from(Tables.RecentlyPlayed.TABLE).select(Tables.RecentlyPlayed.CONTEXT_ID).whereEq(Tables.RecentlyPlayed.CONTEXT_TYPE, PlayHistoryRecord.CONTEXT_PLAYLIST))
                .map { Urn.forPlaylist(it.getLong(Tables.RecentlyPlayed.CONTEXT_ID)) }
                .toSet()
    }
}
