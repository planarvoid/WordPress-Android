package com.soundcloud.android.cast;

import com.soundcloud.android.model.Urn;

import java.util.Collections;
import java.util.List;

class RemotePlayQueue {

    private final List<Urn> trackList;
    private final Urn currentTrackUrn;

    RemotePlayQueue(List<Urn> trackList, Urn currentTrackUrn) {
        this.trackList = Collections.unmodifiableList(trackList);
        this.currentTrackUrn = currentTrackUrn;
    }

    List<Urn> getTrackList() {
        return trackList;
    }

    Urn getCurrentTrackUrn() {
        return currentTrackUrn;
    }

    public int getCurrentPosition() {
        return trackList.indexOf(currentTrackUrn);
    }

}
