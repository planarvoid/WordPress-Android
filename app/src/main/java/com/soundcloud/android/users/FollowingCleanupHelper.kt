package com.soundcloud.android.users

import com.soundcloud.android.storage.DefaultCleanupHelper
import javax.inject.Inject

class FollowingCleanupHelper
@Inject constructor(private val followingStorage: FollowingStorage) : DefaultCleanupHelper() {
    override fun usersToKeep() = followingStorage.selectAllUrns()
}

