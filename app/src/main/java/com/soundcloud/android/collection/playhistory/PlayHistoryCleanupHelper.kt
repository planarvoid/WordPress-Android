package com.soundcloud.android.collection.playhistory

import com.soundcloud.android.model.Urn
import com.soundcloud.android.storage.DefaultCleanupHelper
import javax.inject.Inject

class PlayHistoryCleanupHelper
@Inject constructor(private val playHistoryStorage: PlayHistoryStorage) : DefaultCleanupHelper() {

    override fun tracksToKeep(): Set<Urn> {
        return playHistoryStorage.loadAll().flatMap { listOf(it.trackUrn(), it.contextUrn()) }.filter { it.isTrack }.toSet()
    }

    override fun playlistsToKeep(): Set<Urn> {
        return playHistoryStorage.loadAll().map { it.contextUrn() }.filter { it.isPlaylist }.toSet()
    }
}
