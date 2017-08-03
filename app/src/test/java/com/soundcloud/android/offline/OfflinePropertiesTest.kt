package com.soundcloud.android.offline

import com.soundcloud.android.model.Urn
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.junit.MockitoJUnitRunner
import java.util.Collections.emptyMap
import java.util.Collections.singletonMap

@RunWith(MockitoJUnitRunner::class)
class OfflinePropertiesTest {

    @Test
    fun returnNOT_OFFLINEWhenEntityIsAbsent() {
        assertThat(OfflineProperties()
                .likedTracksState)
                .isEqualTo(OfflineState.NOT_OFFLINE)

        assertThat(OfflineProperties()
                .state(Urn.forTrack(123L)))
                .isEqualTo(OfflineState.NOT_OFFLINE)
    }

    @Test
    fun returnStateAsCreated() {
        assertThat(OfflineProperties(emptyMap<Urn, OfflineState>(), OfflineState.REQUESTED)
                .likedTracksState)
                .isEqualTo(OfflineState.REQUESTED)

        assertThat(OfflineProperties(singletonMap(Urn.forTrack(123L), OfflineState.DOWNLOADED), OfflineState.NOT_OFFLINE)
                .state(Urn.forTrack(123L)))
                .isEqualTo(OfflineState.DOWNLOADED)
    }

    @Test
    fun appliesNewStateToOldOne() {
        val track1 = Urn.forTrack(1L)
        val track2 = Urn.forTrack(2L)
        val oldMap = mapOf(Pair(track1, OfflineState.NOT_OFFLINE), Pair(track2, OfflineState.NOT_OFFLINE))
        val oldState = OfflineProperties(oldMap, OfflineState.NOT_OFFLINE)
        val currentMap = mapOf(Pair(track1, OfflineState.REQUESTED))
        val currentState = OfflineProperties(currentMap, OfflineState.REQUESTED)

        val newState = oldState.apply(currentState)

        assertThat(newState.state(track1)).isEqualTo(OfflineState.REQUESTED)
        assertThat(newState.state(track2)).isEqualTo(OfflineState.NOT_OFFLINE)

        assertThat(newState.likedTracksState).isEqualTo(OfflineState.REQUESTED)
    }

    @Test
    fun testDefaultContructor() {
        val emptyObject = OfflineProperties()

        assertThat(emptyObject.offlineEntitiesStates).isEqualTo(emptyMap<Urn, OfflineState>())
        assertThat(emptyObject.likedTracksState).isEqualTo(OfflineState.NOT_OFFLINE)
    }
}
