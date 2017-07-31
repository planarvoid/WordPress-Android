package com.soundcloud.android.profile

import com.soundcloud.android.model.Urn
import com.soundcloud.android.storage.DefaultCleanupHelper
import com.soundcloud.android.storage.Tables
import com.soundcloud.propeller.PropellerDatabase
import com.soundcloud.propeller.query.Query
import javax.inject.Inject

class PostsCleanupHelper
@Inject constructor(private val propeller: PropellerDatabase) : DefaultCleanupHelper() {
    override fun tracksToKeep(): Set<Urn> {
        return loadPosts().filter { it.isTrack }.toSet()
    }

    override fun playlistsToKeep(): Set<Urn> {
        return loadPosts().filter { it.isPlaylist }.toSet()
    }

    private fun loadPosts(): List<Urn> {
        return propeller.query(Query.from(Tables.Posts.TABLE).select(Tables.Posts.TARGET_ID, Tables.Posts.TARGET_TYPE))
                .map {
                    val soundId = it.getLong(Tables.Posts.TARGET_ID)
                    if (it.getInt(Tables.Posts.TARGET_TYPE) == Tables.Sounds.TYPE_TRACK) {
                        Urn.forTrack(soundId)
                    } else {
                        Urn.forPlaylist(soundId)
                    }
                }
    }
}

