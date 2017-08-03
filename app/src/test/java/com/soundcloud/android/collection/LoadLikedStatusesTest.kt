package com.soundcloud.android.collection

import com.soundcloud.android.model.Urn
import com.soundcloud.android.testsupport.StorageIntegrationTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import java.util.*

class LoadLikedStatusesTest : StorageIntegrationTest() {

    private lateinit var command: LoadLikedStatuses

    @Before
    fun setUp() {
        command = LoadLikedStatuses(propeller())
    }

    @Test
    fun shouldReturnPlaylistAndTrackLikeStatuses() {
        val likedPlaylist = testFixtures().insertLikedPlaylist(Date())!!
        val likedTrack = testFixtures().insertLikedTrack(Date())
        val playlist = testFixtures().insertPlaylist()
        val track = testFixtures().insertTrack()

        val urns = listOf(likedPlaylist, playlist).map {it.urn} + listOf(likedTrack, track).map {it.urn}

        val likedStatuses = command.call(urns)

        assertThat(likedStatuses).hasSize(4)
        assertThat(likedStatuses[likedPlaylist.urn]).isTrue()
        assertThat(likedStatuses[playlist.urn]).isFalse()
        assertThat(likedStatuses[likedTrack.urn]).isTrue()
        assertThat(likedStatuses[track.urn]).isFalse()
    }

    @Test
    fun shouldNotReturnLikedPlaylistForTrackUrn() {
        val numericId = 1L
        testFixtures().insertLikedPlaylist(Date(), testFixtures().insertPlaylist(Urn.forPlaylist(numericId)))
        val track = testFixtures().insertTrack(Urn.forTrack(numericId))

        val likedStatuses = command.call(listOf(track.urn))

        assertThat(likedStatuses).hasSize(1)
        assertThat(likedStatuses[track.urn]).isFalse()
    }

    @Test
    fun shouldNotReturnLikedTrackForPlaylistUrn() {
        val numericId = 2L
        val playlist = testFixtures().insertPlaylist(Urn.forPlaylist(numericId))
        testFixtures().insertLikedTrack(Date(), testFixtures().insertTrack(Urn.forTrack(numericId)))

        val likedStatuses = command.call(listOf(playlist.urn))

        assertThat(likedStatuses).hasSize(1)
        assertThat(likedStatuses[playlist.urn]).isFalse()
    }
}
