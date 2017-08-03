package com.soundcloud.android.collection

import com.soundcloud.android.commands.Command
import com.soundcloud.android.model.Urn
import com.soundcloud.android.storage.Tables
import com.soundcloud.propeller.CursorReader
import com.soundcloud.propeller.PropellerDatabase
import com.soundcloud.propeller.query.Query
import javax.inject.Inject

open class LoadLikedStatuses
@Inject
constructor(private val propeller: PropellerDatabase) : Command<Iterable<Urn>, Map<Urn, Boolean>>() {

    override fun call(input: Iterable<Urn>): Map<Urn, Boolean> {
        return toLikedSet(likedPlaylists(input) + likedTracks(input), input)
    }

    private fun toLikedSet(likedUrns: List<Urn>, input: Iterable<Urn>): Map<Urn, Boolean> {
        val likedMap = mutableMapOf<Urn, Boolean>()
        likedMap.putAll(input.map { it to false })
        likedMap.putAll(likedUrns.map { it to true })
        return likedMap
    }

    private fun likedPlaylists(input: Iterable<Urn>): List<Urn> {
        return query(input.filter { it.isPlaylist }, Tables.Sounds.TYPE_PLAYLIST).map { Urn.forPlaylist(it.getLong(Tables.Likes._ID)) }
    }

    private fun likedTracks(input: Iterable<Urn>): List<Urn> {
        return query(input.filter { it.isTrack }, Tables.Sounds.TYPE_TRACK).map { Urn.forTrack(it.getLong(Tables.Likes._ID)) }
    }

    private fun query(urns: List<Urn>, type: Int): Iterable<CursorReader> {
        return if (urns.isNotEmpty()) {
            propeller.query(Query.from(Tables.Likes.TABLE)
                    .whereNull(Tables.Likes.REMOVED_AT.qualifiedName())
                    .whereEq(Tables.Likes._TYPE, type)
                    .whereIn(Tables.Likes._ID, urns.map { it.numericId }))
        } else emptyList()
    }
}
