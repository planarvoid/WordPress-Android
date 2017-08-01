package com.soundcloud.android.stream

import com.soundcloud.android.model.Urn
import com.soundcloud.android.testsupport.StorageIntegrationTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test

class StreamCleanupHelperTest : StorageIntegrationTest() {
    private lateinit var streamCleanupHelper: StreamCleanupHelper
    @Before
    fun setUp() {
        streamCleanupHelper = StreamCleanupHelper(propeller())
    }

    @Test
    fun returnsTracksToKeep() {
        val trackUrn = Urn.forTrack(12L)
        testFixtures().insertStreamTrackPost(trackUrn.numericId, 123L)

        val tracksToKeep = streamCleanupHelper.tracksToKeep()

        assertThat(tracksToKeep).containsOnly(trackUrn)
    }

    @Test
    fun returnsPlaylistsToKeep() {
        val playlistUrn = Urn.forPlaylist(12L)
        testFixtures().insertStreamPlaylistPost(playlistUrn.numericId, 123L)

        val playlistsToKeep = streamCleanupHelper.playlistsToKeep()

        assertThat(playlistsToKeep).containsOnly(playlistUrn)
    }

    @Test
    fun returnsUsersToKeep() {
        val trackUrn = Urn.forTrack(12L)
        val userUrn = Urn.forUser(23L)
        testFixtures().insertStreamTrackRepost(trackUrn.numericId, 123L, userUrn.numericId)

        val usersToKeep = streamCleanupHelper.usersToKeep()

        assertThat(usersToKeep).containsOnly(userUrn)
    }
}
