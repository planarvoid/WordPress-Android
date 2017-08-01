package com.soundcloud.android.storage;


import com.google.auto.value.AutoValue;
import com.soundcloud.android.events.MetricEvent;
import com.soundcloud.android.events.ReferringEvent;
import com.soundcloud.android.events.TrackingEvent;
import com.soundcloud.java.optional.Optional;
import com.soundcloud.reporting.DataPoint;
import com.soundcloud.reporting.Metric;

@AutoValue
public abstract class StorageCleanupEvent extends TrackingEvent implements MetricEvent {

    public static StorageCleanupEvent create(int usersRemoved, int tracksRemoved, int playlistsRemoved) {
        return new AutoValue_StorageCleanupEvent(
                defaultId(),
                defaultTimestamp(),
                Optional.absent(),
                usersRemoved,
                tracksRemoved,
                playlistsRemoved
        );
    }

    abstract int usersRemoved();
    abstract int tracksRemoved();
    abstract int playlistsRemoved();

    @Override
    public Metric toMetric() {
        return Metric.create("StorageCleanup",
                             DataPoint.numeric("UsersRemoved", usersRemoved()),
                             DataPoint.numeric("TracksRemoved", tracksRemoved()),
                             DataPoint.numeric("PlaylistsRemoved", playlistsRemoved()));
    }

    @Override
    public TrackingEvent putReferringEvent(ReferringEvent referringEvent) {
        return new AutoValue_StorageCleanupEvent(
                id(),
                timestamp(),
                Optional.absent(),
                usersRemoved(),
                tracksRemoved(),
                playlistsRemoved()
        );
    }
}
