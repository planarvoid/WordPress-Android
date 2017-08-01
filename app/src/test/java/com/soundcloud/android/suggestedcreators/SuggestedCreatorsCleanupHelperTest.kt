package com.soundcloud.android.suggestedcreators

import com.soundcloud.android.sync.suggestedCreators.ApiSuggestedCreator
import com.soundcloud.android.testsupport.StorageIntegrationTest
import org.assertj.core.api.Assertions
import org.junit.Before
import org.junit.Test

class SuggestedCreatorsCleanupHelperTest : StorageIntegrationTest() {
    private lateinit var cleanupHelper: SuggestedCreatorsCleanupHelper

    @Before
    fun setup() {
        cleanupHelper = SuggestedCreatorsCleanupHelper(propeller())
    }

    @Test
    fun returnUsersFromChartsToKeep() {
        val seedUser = testFixtures().insertUser()
        val suggestedUser = testFixtures().insertUser()
        testFixtures().insertSuggestedCreator(ApiSuggestedCreator.create(seedUser, suggestedUser, "followed"))
        val usersToKeep = cleanupHelper.usersToKeep()

        Assertions.assertThat(usersToKeep).containsOnly(seedUser.urn, suggestedUser.urn)
    }
}

