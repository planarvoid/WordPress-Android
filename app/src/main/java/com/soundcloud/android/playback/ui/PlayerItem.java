package com.soundcloud.android.playback.ui;

import com.soundcloud.android.model.Urn;
import com.soundcloud.android.tracks.TrackProperty;
import com.soundcloud.java.collections.PropertySet;

public class PlayerItem {
    protected final PropertySet source;

    public PlayerItem(PropertySet source) {
        this.source = source;
    }

    public Urn getTrackUrn() {
        return source.get(TrackProperty.URN);
    }
}
