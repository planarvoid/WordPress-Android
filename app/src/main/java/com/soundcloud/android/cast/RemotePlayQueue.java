package com.soundcloud.android.cast;

import com.soundcloud.android.model.Urn;

import java.util.List;

public class RemotePlayQueue {

    private final List<Urn> trackList;
    private final Urn currentTrackUrn;

    public RemotePlayQueue(List<Urn> trackList, Urn currentTrackUrn) {
        this.trackList = trackList;
        this.currentTrackUrn = currentTrackUrn;
    }

    public List<Urn> getTrackList() {
        return trackList;
    }

    public Urn getCurrentTrackUrn() {
        return currentTrackUrn;
    }

    public int getCurrentPosition() {
        return trackList.indexOf(currentTrackUrn);
    }

}
