package com.soundcloud.android.offline

import com.nhaarman.mockito_kotlin.whenever
import com.soundcloud.android.model.Urn
import com.soundcloud.android.testsupport.AndroidUnitTest
import com.soundcloud.java.collections.Lists
import io.reactivex.Single
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.Mock

class OfflineContentCleanupHelperTest : AndroidUnitTest() {
    private lateinit var offlineContentCleanupHelper: OfflineContentCleanupHelper
    @Mock lateinit var offlineContentStorage: OfflineContentStorage

    @Before
    fun setUp() {
        offlineContentCleanupHelper = OfflineContentCleanupHelper(offlineContentStorage)
    }

    @Test
    fun returnsPlaylistsToKeep() {
        whenever(offlineContentStorage.offlinePlaylists).thenReturn(Single.just(Lists.newArrayList(Urn.forPlaylist(123))))

        val playlistsToKeep = offlineContentCleanupHelper.playlistsToKeep()

        assertThat(playlistsToKeep).containsOnly(Urn.forPlaylist(123))
    }
}
