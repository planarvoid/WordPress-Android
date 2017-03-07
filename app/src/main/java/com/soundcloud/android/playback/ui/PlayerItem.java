package com.soundcloud.android.playback.ui;

import com.soundcloud.android.model.Urn;
import com.soundcloud.android.tracks.TrackItem;
import com.soundcloud.java.optional.Optional;


public class PlayerItem {
    protected final Optional<TrackItem> source;

    public PlayerItem(TrackItem source) {
        this.source = Optional.of(source);
    }

    public PlayerItem() {
        source = Optional.absent();
    }

    public Urn getTrackUrn() {
        return source.transform(TrackItem::getUrn).or(Urn.NOT_SET);
    }
}
