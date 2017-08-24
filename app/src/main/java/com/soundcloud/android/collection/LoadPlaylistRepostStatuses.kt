package com.soundcloud.android.collection

import com.soundcloud.android.commands.Command
import com.soundcloud.android.model.Urn
import com.soundcloud.android.utils.OpenForTesting
import javax.inject.Inject

@OpenForTesting
class LoadPlaylistRepostStatuses
@Inject
constructor(private val loadRepostStatuses: LoadRepostStatuses) : Command<Iterable<Urn>, Map<Urn, Boolean>>() {

    override fun call(input: Iterable<Urn>): Map<Urn, Boolean> {
        return loadRepostStatuses.call(input.filter { it.isPlaylist })
    }
}
