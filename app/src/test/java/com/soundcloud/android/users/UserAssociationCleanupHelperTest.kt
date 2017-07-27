package com.soundcloud.android.users

import com.soundcloud.android.model.Urn
import com.soundcloud.android.testsupport.StorageIntegrationTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test

class UserAssociationCleanupHelperTest : StorageIntegrationTest() {
    private val followedUser = Urn.forUser(1L)
    private lateinit var userAssociationCleanupHelper: UserAssociationCleanupHelper
    @Before
    fun setUp() {
        testFixtures().insertFollowing(followedUser)
        userAssociationCleanupHelper = UserAssociationCleanupHelper(propeller())
    }

    @Test
    fun returnsUserToKeep() {
        val usersToKeep = userAssociationCleanupHelper.usersToKeep

        assertThat(usersToKeep).containsOnly(followedUser)
    }
}
