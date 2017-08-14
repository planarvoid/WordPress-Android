package com.soundcloud.android.collection.recentlyplayed

import com.soundcloud.android.collection.playhistory.PlayHistoryRecord
import com.soundcloud.android.model.Urn
import com.soundcloud.android.storage.DefaultCleanupHelper
import javax.inject.Inject

class RecentlyPlayedCleanupHelper
@Inject constructor(private val recentlyPlayedStorage: RecentlyPlayedStorage) : DefaultCleanupHelper() {

    override fun usersToKeep(): Set<Urn> {
        return recentlyPlayedStorage.loadContextIdsByType(PlayHistoryRecord.CONTEXT_ARTIST)
                .map { Urn.forUser(it) }
                .toSet()
    }

    override fun playlistsToKeep(): Set<Urn> {
        return recentlyPlayedStorage.loadContextIdsByType(PlayHistoryRecord.CONTEXT_PLAYLIST)
                .map { Urn.forPlaylist(it) }
                .toSet()
    }
}
