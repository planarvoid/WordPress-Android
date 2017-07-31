package com.soundcloud.android.likes

import com.soundcloud.android.model.Urn
import com.soundcloud.android.storage.BaseRxResultMapperV2
import com.soundcloud.android.storage.DefaultCleanupHelper
import com.soundcloud.android.storage.Tables
import com.soundcloud.propeller.PropellerDatabase
import com.soundcloud.propeller.query.Query
import javax.inject.Inject

class LikeCleanupHelper
@Inject constructor(private val propeller: PropellerDatabase) : DefaultCleanupHelper() {

    override fun tracksToKeep(): Set<Urn> {
        return loadLikes().filter { it.isTrack }.toSet()
    }

    override fun playlistsToKeep(): Set<Urn> {
        return loadLikes().filter { it.isPlaylist }.toSet()
    }

    private fun loadLikes(): List<Urn> {
        return propeller.query(Query.from(Tables.Likes.TABLE).select(Tables.Likes._ID, Tables.Likes._TYPE))
                .map { BaseRxResultMapperV2.readSoundUrn(it, Tables.Likes._ID, Tables.Likes._TYPE) }
    }

}
