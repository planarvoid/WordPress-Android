package com.soundcloud.android.olddiscovery.newforyou

import com.nhaarman.mockito_kotlin.whenever
import com.soundcloud.android.model.Urn
import com.soundcloud.android.testsupport.fixtures.ModelFixtures
import io.reactivex.Single
import org.assertj.core.api.Assertions
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import java.util.*

@RunWith(MockitoJUnitRunner::class)
class NewForYouCleanupHelperTest {
    @Mock private lateinit var newForYouStorage: NewForYouStorage

    private lateinit var cleanupHelper: NewForYouCleanupHelper

    private lateinit var trackUrn: Urn

    @Before
    fun setup() {
        cleanupHelper = NewForYouCleanupHelper(newForYouStorage)

        val track = ModelFixtures.track()
        trackUrn = track.urn()
        whenever(newForYouStorage.newForYou()).thenReturn(Single.just(NewForYou.create(Date(), Urn("soundcloud:query:123"), mutableListOf(track))))
    }

    @Test
    fun returnNewForYouTracksToKeep() {
        val tracksToKeep = cleanupHelper.tracksToKeep()

        Assertions.assertThat(tracksToKeep).containsOnly(trackUrn)
    }
}
