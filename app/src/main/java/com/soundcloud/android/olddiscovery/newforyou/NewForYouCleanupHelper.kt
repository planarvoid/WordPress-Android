package com.soundcloud.android.olddiscovery.newforyou

import com.soundcloud.android.model.Urn
import com.soundcloud.android.storage.DefaultCleanupHelper
import javax.inject.Inject

internal class NewForYouCleanupHelper
@Inject constructor(private val newForYouStorage: NewForYouStorage) : DefaultCleanupHelper() {

    override fun getTracksToKeep(): MutableSet<Urn> {
        return newForYouStorage.newForYou().map { it.tracks() }.blockingGet().map { it.urn() }.toMutableSet()
    }
}
