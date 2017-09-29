package com.soundcloud.android.playlists

import com.soundcloud.android.commands.Command
import com.soundcloud.android.model.Urn
import com.soundcloud.android.offline.OfflineContentStorage
import com.soundcloud.android.utils.ErrorUtils
import com.soundcloud.android.utils.OpenForTesting

import javax.inject.Inject

@OpenForTesting
class RemovePlaylistCommand
@Inject
constructor(private val removePlaylistFromDatabaseCommand: RemovePlaylistFromDatabaseCommand,
            private val offlineContentStorage: OfflineContentStorage) : Command<Urn, Boolean>() {

    override fun call(input: Urn): Boolean? {
        if (removePlaylistFromDatabaseCommand.call(input).success()) {
            val error = offlineContentStorage.removePlaylistsFromOffline(input).blockingGet()
            if (error != null) {
                ErrorUtils.handleThrowable(error, "Unable to remove playlist from offline storage")
                return false
            }
        }
        return true
    }
}
