package com.soundcloud.android.users

import com.soundcloud.android.model.Urn
import com.soundcloud.android.testsupport.StorageIntegrationTest
import io.reactivex.schedulers.Schedulers
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.robolectric.RuntimeEnvironment

class FollowingCleanupHelperTest : StorageIntegrationTest() {
    private val followedUser = Urn.forUser(1L)
    private lateinit var followingStorage: FollowingStorage
    private lateinit var followingCleanupHelper: FollowingCleanupHelper
    @Before
    fun setUp() {
        val database = FollowingDatabase(FollowingOpenHelper(RuntimeEnvironment.application), Schedulers.trampoline())
        followingStorage = FollowingStorage(database)
        followingCleanupHelper = FollowingCleanupHelper(followingStorage)
    }

    @Test
    fun returnsUserToKeep() {
        followingStorage.insertFollowing(followedUser, true).test()

        val usersToKeep = followingCleanupHelper.usersToKeep()

        assertThat(usersToKeep).containsOnly(followedUser)
    }
}
