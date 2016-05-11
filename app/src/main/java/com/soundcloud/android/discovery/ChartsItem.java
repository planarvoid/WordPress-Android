package com.soundcloud.android.discovery;

import com.soundcloud.android.image.ImageResource;

import java.util.Collections;
import java.util.List;

class ChartsItem extends DiscoveryItem {
    public final List<? extends ImageResource> newAndHotTracks;
    public final List<? extends ImageResource> topFiftyTracks;

    ChartsItem(List<? extends ImageResource> newAndHotTracks, List<? extends ImageResource> topFiftyTracks) {
        super(Kind.ChartItem);
        this.newAndHotTracks = Collections.unmodifiableList(newAndHotTracks);
        this.topFiftyTracks = Collections.unmodifiableList(topFiftyTracks);
    }
}
