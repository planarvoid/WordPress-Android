package com.soundcloud.android.storage

import com.soundcloud.android.model.Urn

open class DefaultCleanupHelper : DatabaseCleanupService.CleanupHelper {
    override fun getUsersToKeep() = mutableSetOf<Urn>()

    override fun getTracksToKeep() = mutableSetOf<Urn>()

    override fun getPlaylistsToKeep() = mutableSetOf<Urn>()
}
