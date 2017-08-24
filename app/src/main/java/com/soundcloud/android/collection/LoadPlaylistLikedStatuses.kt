package com.soundcloud.android.collection

import com.soundcloud.android.commands.Command
import com.soundcloud.android.model.Urn
import com.soundcloud.android.utils.OpenForTesting
import javax.inject.Inject

@OpenForTesting
class LoadPlaylistLikedStatuses
@Inject
constructor(private val loadLikedStatuses: LoadLikedStatuses) : Command<Iterable<Urn>, Map<Urn, Boolean>>() {

    override fun call(input: Iterable<Urn>): Map<Urn, Boolean> {
        return loadLikedStatuses.call(input.filter { it.isPlaylist })
    }
}
