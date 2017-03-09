package com.soundcloud.android.events;

import com.google.auto.value.AutoValue;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.tracks.Track;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

@AutoValue
public abstract class TrackChangedEvent {
    public abstract Map<Urn, Track> changeMap();

    public static TrackChangedEvent forUpdate(Track track) {
        return new AutoValue_TrackChangedEvent(Collections.singletonMap(track.urn(), track));
    }

    public static TrackChangedEvent forUpdate(Collection<Track> tracks) {
        final Map<Urn, Track> changeSet = new HashMap<>(tracks.size());
        for (Track track : tracks) {
            changeSet.put(track.urn(), track);
        }
        return new AutoValue_TrackChangedEvent(changeSet);
    }
}
