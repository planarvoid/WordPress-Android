package com.soundcloud.android.olddiscovery.recommendedplaylists

import com.soundcloud.android.model.Urn
import com.soundcloud.android.storage.DefaultCleanupHelper
import com.soundcloud.android.storage.Tables
import com.soundcloud.propeller.PropellerDatabase
import com.soundcloud.propeller.query.Query
import javax.inject.Inject

internal class RecommendedPlaylistsCleanupHelper
@Inject constructor(private val propeller: PropellerDatabase) : DefaultCleanupHelper() {

    override fun getPlaylistsToKeep(): MutableSet<Urn> {
        return propeller.query(Query.from(Tables.RecommendedPlaylist.TABLE).select(Tables.RecommendedPlaylist.PLAYLIST_ID))
                .map { Urn.forPlaylist(it.getLong(Tables.RecommendedPlaylist.PLAYLIST_ID)) }
                .toMutableSet()
    }
}
