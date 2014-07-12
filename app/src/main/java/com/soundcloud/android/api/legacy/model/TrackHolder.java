package com.soundcloud.android.api.legacy.model;

import java.util.List;

public class TrackHolder extends CollectionHolder<PublicApiTrack> {

    // needed for jackson
    public TrackHolder() {}

    public TrackHolder(List<PublicApiTrack> collection) {
        super(collection);
    }
}
