package com.soundcloud.android.model;

import java.util.List;

public class TrackHolder extends CollectionHolder<Track> {

    // needed for jackson
    public TrackHolder() {}

    public TrackHolder(List<Track> collection) {
        super(collection);
    }
}
