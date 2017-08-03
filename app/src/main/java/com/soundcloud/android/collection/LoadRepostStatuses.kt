package com.soundcloud.android.collection

import com.soundcloud.android.commands.Command
import com.soundcloud.android.model.Urn
import com.soundcloud.android.storage.Tables
import com.soundcloud.android.storage.Tables.Posts
import com.soundcloud.propeller.CursorReader
import com.soundcloud.propeller.PropellerDatabase
import com.soundcloud.propeller.query.Query
import javax.inject.Inject

open class LoadRepostStatuses
@Inject
constructor(private val propeller: PropellerDatabase) : Command<Iterable<Urn>, Map<Urn, Boolean>>() {

    override fun call(input: Iterable<Urn>): Map<Urn, Boolean> {
        return toRepostedSet(input, playlists(input) + tracks(input))
    }

    private fun playlists(input: Iterable<Urn>): List<Pair<Urn, Boolean>> {
        return query(input.filter { it.isPlaylist }, Tables.Sounds.TYPE_PLAYLIST).map { Urn.forPlaylist(it.getLong(Posts.TARGET_ID)) to it.isReposted() }
    }

    private fun tracks(input: Iterable<Urn>): List<Pair<Urn, Boolean>> {
        return query(input.filter { it.isTrack }, Tables.Sounds.TYPE_TRACK).map { Urn.forTrack(it.getLong(Posts.TARGET_ID)) to it.isReposted() }
    }

    private fun query(input: List<Urn>, type: Int): Iterable<CursorReader> {
        return if (input.isNotEmpty()) {
            propeller.query(Query.from(Posts.TABLE)
                    .select(Posts.TARGET_ID, Posts.TYPE)
                    .whereNull(Posts.REMOVED_AT)
                    .whereEq(Posts.TARGET_TYPE, type)
                    .whereIn(Posts.TARGET_ID, input.map { it.numericId }))
        } else emptyList()
    }

    private fun toRepostedSet(input: Iterable<Urn>, reposts: List<Pair<Urn, Boolean>>): Map<Urn, Boolean> {
        val result = mutableMapOf<Urn, Boolean>()
        result.putAll(input.map { it to false })
        result.putAll(reposts)
        return result
    }
}

private fun CursorReader.isReposted() = this.isNotNull(Posts.TYPE) && this.getString(Posts.TYPE) == Posts.TYPE_REPOST
