package com.soundcloud.android.activities

import com.soundcloud.android.api.model.ApiPlaylist
import com.soundcloud.android.api.model.ApiTrack
import com.soundcloud.android.api.model.ApiUser
import com.soundcloud.android.sync.activities.ApiPlaylistRepostActivity
import com.soundcloud.android.sync.activities.ApiTrackLikeActivity
import com.soundcloud.android.testsupport.StorageIntegrationTest
import org.assertj.core.api.Assertions
import org.junit.Before
import org.junit.Test
import java.util.*

class ActivitiesCleanupHelperTest : StorageIntegrationTest() {

    private lateinit var cleanupHelper: ActivitiesCleanupHelper

    private lateinit var likedTrack: ApiTrack
    private lateinit var likingUser: ApiUser
    private lateinit var repostedPlaylist: ApiPlaylist
    private lateinit var repostingUser: ApiUser

    @Before
    fun setup() {
        cleanupHelper = ActivitiesCleanupHelper(propeller())
        likedTrack = testFixtures().insertTrack()
        likingUser = testFixtures().insertUser()
        repostedPlaylist = testFixtures().insertPlaylist()
        repostingUser = testFixtures().insertUser()
        testFixtures().insertTrackLikeActivity(ApiTrackLikeActivity(likedTrack, likingUser, Date()))
        testFixtures().insertPlaylistRepostActivity(ApiPlaylistRepostActivity(repostedPlaylist, repostingUser, Date()))
    }

    @Test
    fun returnsTrackToKeep() {
        val tracksToKeep = cleanupHelper.tracksToKeep()

        Assertions.assertThat(tracksToKeep).containsOnly(likedTrack.urn)
    }

    @Test
    fun returnsPlaylistToKeep() {
        val playlistsToKeep = cleanupHelper.playlistsToKeep()

        Assertions.assertThat(playlistsToKeep).containsOnly(repostedPlaylist.urn)
    }

    @Test
    fun returnsUsersToKeep() {
        val usersToKeep = cleanupHelper.usersToKeep()

        Assertions.assertThat(usersToKeep).containsOnly(likingUser.urn, repostingUser.urn)
    }
}
