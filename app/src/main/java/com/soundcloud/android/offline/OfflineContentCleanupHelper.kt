package com.soundcloud.android.offline

import com.soundcloud.android.model.Urn
import com.soundcloud.android.storage.DefaultCleanupHelper
import javax.inject.Inject

class OfflineContentCleanupHelper
@Inject constructor(private val offlineContentStorage: OfflineContentStorage) : DefaultCleanupHelper() {
    override fun playlistsToKeep(): Set<Urn> = HashSet(offlineContentStorage.offlinePlaylists.blockingGet())
}
