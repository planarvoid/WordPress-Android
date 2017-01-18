package com.soundcloud.android.cast;

import static java.util.Collections.singletonList;

import com.google.auto.value.AutoValue;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playback.PlayQueue;
import com.soundcloud.android.playback.PlaySessionSource;

import java.util.Collections;
import java.util.List;
import java.util.Map;

@Deprecated @AutoValue
public abstract class RemotePlayQueue {

    public static RemotePlayQueue create(List<Urn> trackList, Urn currentTrackUrn) {
        return new AutoValue_RemotePlayQueue(Collections.unmodifiableList(trackList), currentTrackUrn);
    }

    abstract List<Urn> trackList();

    abstract Urn currentTrackUrn();

    boolean hasSameTracks(List<Urn> trackList) {
        return trackList().equals(trackList);
    }

    public int getCurrentPosition() {
        return trackList().indexOf(currentTrackUrn());
    }

    public boolean isEmpty() {
        return trackList().isEmpty();
    }

    PlayQueue toPlayQueue(PlaySessionSource playSessionSource,
                          Map<Urn, Boolean> blockedTracks) {
        List<Urn> trackUrns = isEmpty() ? singletonList(currentTrackUrn()) : trackList();
        return PlayQueue.fromTrackUrnList(trackUrns, playSessionSource, blockedTracks);
    }
}
