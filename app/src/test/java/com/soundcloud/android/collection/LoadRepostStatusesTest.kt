package com.soundcloud.android.collection

import com.soundcloud.android.api.model.ApiPlaylist
import com.soundcloud.android.api.model.ApiTrack
import com.soundcloud.android.testsupport.StorageIntegrationTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import java.util.*

class LoadRepostStatusesTest : StorageIntegrationTest() {

    private lateinit var command: LoadRepostStatuses

    @Before
    fun setUp() {
        command = LoadRepostStatuses(propeller())
    }

    @Test
    fun shouldReturnPlaylistAndTrackLikeStatuses() {
        val repostedPlaylist = insertRepostedPlaylist()
        val repostedTrack = insertRepostedTrack()
        val playlist = testFixtures().insertPlaylist()
        val track = testFixtures().insertTrack()

        val urns = listOf(repostedPlaylist, playlist).map { it.urn } + listOf(repostedTrack, track).map { it.urn }

        val repostedStatuses = command.call(urns)

        assertThat(repostedStatuses).hasSize(4)
        assertThat(repostedStatuses[repostedPlaylist.urn]).isTrue()
        assertThat(repostedStatuses[playlist.urn]).isFalse()
        assertThat(repostedStatuses[repostedTrack.urn]).isTrue()
        assertThat(repostedStatuses[track.urn]).isFalse()
    }

    private fun insertRepostedPlaylist(): ApiPlaylist {
        val repostedPlaylist = testFixtures().insertPlaylist()
        testFixtures().insertPlaylistRepost(repostedPlaylist.id, Date().time)
        return repostedPlaylist
    }

    private fun insertRepostedTrack(): ApiTrack {
        val repostedTrack = testFixtures().insertTrack()
        testFixtures().insertTrackRepost(repostedTrack.id, Date().time)
        return repostedTrack
    }
}
