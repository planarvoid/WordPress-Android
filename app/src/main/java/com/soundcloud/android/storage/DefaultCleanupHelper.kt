package com.soundcloud.android.storage

import com.soundcloud.android.model.Urn

open class DefaultCleanupHelper : CleanupHelper {
    override fun usersToKeep() = setOf<Urn>()
    override fun tracksToKeep() = setOf<Urn>()
    override fun playlistsToKeep() = setOf<Urn>()
}
