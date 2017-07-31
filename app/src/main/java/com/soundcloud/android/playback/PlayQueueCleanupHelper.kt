package com.soundcloud.android.playback

import com.soundcloud.android.storage.DefaultCleanupHelper
import javax.inject.Inject

class PlayQueueCleanupHelper
@Inject constructor(private val playQueueStorage: PlayQueueStorage) : DefaultCleanupHelper() {
    override fun tracksToKeep() = loadUrns().filter { it.isTrack }.toSet()

    override fun playlistsToKeep() =  loadUrns().filter { it.isPlaylist }.toSet()

    override fun usersToKeep() =  loadUrns().filter { it.isUser }.toSet()

    private fun loadUrns() = playQueueStorage.loadPlayableQueueItems().blockingGet().flatMap { listOf(it.relatedEntity, it.urn, it.reposter, it.sourceUrn) }

}
