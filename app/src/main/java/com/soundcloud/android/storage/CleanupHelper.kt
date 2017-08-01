package com.soundcloud.android.storage

import com.soundcloud.android.model.Urn

interface CleanupHelper {

    fun usersToKeep(): Set<Urn>

    fun tracksToKeep(): Set<Urn>

    fun playlistsToKeep(): Set<Urn>
}
