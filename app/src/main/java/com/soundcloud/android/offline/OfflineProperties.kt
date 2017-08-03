package com.soundcloud.android.offline

import com.soundcloud.android.model.Urn

data class OfflineProperties(val offlineEntitiesStates: Map<Urn, OfflineState> = mapOf<Urn, OfflineState>(),
                             val likedTracksState: OfflineState = OfflineState.NOT_OFFLINE) {

    fun state(urn: Urn) = offlineEntitiesStates.getOrElse(urn) { OfflineState.NOT_OFFLINE }

    fun apply(newState: OfflineProperties): OfflineProperties {
        val newOfflineEntitiesState = mutableMapOf<Urn, OfflineState>()
        newOfflineEntitiesState.putAll(offlineEntitiesStates)
        newOfflineEntitiesState.putAll(newState.offlineEntitiesStates)
        return OfflineProperties(newOfflineEntitiesState, newState.likedTracksState)
    }
}
