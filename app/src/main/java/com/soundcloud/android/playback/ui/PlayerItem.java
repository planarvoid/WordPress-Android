package com.soundcloud.android.playback.ui;

import com.soundcloud.android.model.Urn;
import com.soundcloud.android.tracks.TrackItem;

public class PlayerItem {
    protected final TrackItem source;

    public PlayerItem(TrackItem source) {
        this.source = source;
    }

    public Urn getTrackUrn() {
        return source.getUrn();
    }
}
