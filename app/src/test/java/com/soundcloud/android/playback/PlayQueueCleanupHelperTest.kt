package com.soundcloud.android.playback

import com.nhaarman.mockito_kotlin.whenever
import com.soundcloud.android.model.Urn
import com.soundcloud.android.testsupport.StorageIntegrationTest
import com.soundcloud.java.optional.Optional
import io.reactivex.Single
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.Mock

class PlayQueueCleanupHelperTest : StorageIntegrationTest() {
    @Mock private lateinit var playQueueStorage: PlayQueueStorage

    private val trackUrn = Urn.forTrack(1L)
    private val playlistUrn = Urn.forPlaylist(3L)
    private val reposterUrn = Urn.forUser(2L)
    private val sourceUrn = Urn.forPlaylist(4L)
    private val queryUrn = Urn("sc:query:urn")

    private lateinit var playQueueCleanupHelper: PlayQueueCleanupHelper

    @Before
    fun setUp() {
        playQueueCleanupHelper = PlayQueueCleanupHelper(playQueueStorage)
    }

    @Test
    fun returnsTracksToKeep() {
        initPlayQueueStorage()
        val tracksToKeep = playQueueCleanupHelper.tracksToKeep()

        assertThat(tracksToKeep).containsOnly(trackUrn)
    }

    @Test
    fun returnsPlaylistsToKeep() {
        initPlayQueueStorage()
        val playlistsToKeep = playQueueCleanupHelper.playlistsToKeep()

        assertThat(playlistsToKeep).containsOnly(playlistUrn, sourceUrn)
    }

    @Test
    fun returnsUsersToKeep() {
        initPlayQueueStorage()
        val usersToKeep = playQueueCleanupHelper.usersToKeep()

        assertThat(usersToKeep).containsOnly(reposterUrn)
    }

    private fun initPlayQueueStorage() {
        whenever(playQueueStorage.loadPlayableQueueItems()).thenReturn(Single.just(listOf(TrackQueueItem(trackUrn,
                reposterUrn,
                playlistUrn,
                "source",
                "1.0",
                Optional.absent(),
                sourceUrn,
                queryUrn,
                false,
                PlaybackContext.create(PlaybackContext.Bucket.ARTIST_STATION),
                true))))
    }
}
