package com.soundcloud.android.activities

import com.soundcloud.android.model.Urn
import com.soundcloud.android.storage.DefaultCleanupHelper
import com.soundcloud.android.storage.Table
import com.soundcloud.android.storage.TableColumns
import com.soundcloud.android.storage.Tables
import com.soundcloud.propeller.PropellerDatabase
import com.soundcloud.propeller.query.Query
import javax.inject.Inject

class ActivitiesCleanupHelper
@Inject constructor(private val propeller: PropellerDatabase) : DefaultCleanupHelper() {
    override fun usersToKeep(): Set<Urn> {
        return propeller.query(Query.from(Table.ActivityView).select(TableColumns.ActivityView.USER_ID))
                .map { Urn.forUser(it.getLong(TableColumns.ActivityView.USER_ID)) }
                .toSet()
    }

    override fun tracksToKeep(): Set<Urn> {
        return loadSounds().filter { it.isTrack }.toSet()
    }

    override fun playlistsToKeep(): Set<Urn> {
        return loadSounds().filter { it.isPlaylist }.toSet()
    }

    private fun loadSounds(): List<Urn> {
        return propeller.query(Query.from(Table.ActivityView).select(TableColumns.ActivityView.SOUND_ID, TableColumns.ActivityView.SOUND_TYPE))
                .map {
                    val soundId = it.getLong(TableColumns.ActivityView.SOUND_ID)
                    if (it.getInt(TableColumns.ActivityView.SOUND_TYPE) == Tables.Sounds.TYPE_TRACK) {
                        Urn.forTrack(soundId)
                    } else {
                        Urn.forPlaylist(soundId)
                    }
                }
    }
}

