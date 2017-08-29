package com.soundcloud.android.storage

import com.soundcloud.android.events.MetricEvent
import com.soundcloud.android.events.ReferringEvent
import com.soundcloud.android.events.TrackingEvent
import com.soundcloud.java.optional.Optional
import com.soundcloud.reporting.DataPoint
import com.soundcloud.reporting.Metric

data class RepositoryMissedSyncEvent(val id: String = defaultId(),
                                     val ts: Long = defaultTimestamp(),
                                     val referringEvent: Optional<ReferringEvent> = Optional.absent(),
                                     val usersMissing: Int = 0,
                                     val tracksMissing: Int = 0,
                                     val playlistsMissing: Int = 0) : TrackingEvent(), MetricEvent {
    override fun id() = id

    override fun timestamp() = ts

    override fun referringEvent() = referringEvent

    override fun toMetric(): Metric {
        return Metric.create("StorageCleanup",
                             DataPoint.numeric("UsersMissing", usersMissing),
                             DataPoint.numeric("TracksMissing", tracksMissing),
                             DataPoint.numeric("PlaylistsMissing", playlistsMissing))
    }

    override fun putReferringEvent(referringEvent: ReferringEvent): TrackingEvent {
        return copy(referringEvent = Optional.of(referringEvent))
    }

    companion object {
        fun fromTracksMissing(tracksMissing: Int) = RepositoryMissedSyncEvent(tracksMissing = tracksMissing)
        fun fromPlaylistsMissing(playlistsMissing: Int) = RepositoryMissedSyncEvent(playlistsMissing = playlistsMissing)
        fun fromUsersMissing(usersMissing: Int) = RepositoryMissedSyncEvent(usersMissing = usersMissing)
    }
}
