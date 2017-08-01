package com.soundcloud.android.stream

import com.soundcloud.android.model.Urn
import com.soundcloud.android.storage.DefaultCleanupHelper
import com.soundcloud.android.storage.Table
import com.soundcloud.android.storage.TableColumns
import com.soundcloud.android.storage.Tables
import com.soundcloud.propeller.PropellerDatabase
import com.soundcloud.propeller.query.Query
import javax.inject.Inject

class StreamCleanupHelper
@Inject constructor(private val propeller: PropellerDatabase) : DefaultCleanupHelper() {
    override fun tracksToKeep(): Set<Urn> {
        return propeller.query(Query.from(Table.SoundStream).select(TableColumns.SoundStream.SOUND_ID).whereEq(TableColumns.SoundStream.SOUND_TYPE, Tables.Sounds.TYPE_TRACK))
                .map { Urn.forTrack(it.getLong(TableColumns.SoundStream.SOUND_ID)) }
                .toSet()
    }

    override fun playlistsToKeep(): Set<Urn> {
        return propeller.query(Query.from(Table.SoundStream).select(TableColumns.SoundStream.SOUND_ID).whereEq(TableColumns.SoundStream.SOUND_TYPE, Tables.Sounds.TYPE_PLAYLIST))
                .map { Urn.forPlaylist(it.getLong(TableColumns.SoundStream.SOUND_ID)) }
                .toSet()
    }

    override fun usersToKeep(): Set<Urn> {
        return propeller.query(Query.from(Table.SoundStream).select(TableColumns.SoundStream.REPOSTER_ID))
                .map { Urn.forUser(it.getLong(TableColumns.SoundStream.REPOSTER_ID)) }
                .toSet()
    }
}
