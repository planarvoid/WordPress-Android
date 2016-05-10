package com.soundcloud.android.discovery;

import com.soundcloud.android.image.ImageResource;

import java.util.List;

class ChartItem extends DiscoveryItem {
    private final List<? extends ImageResource> newAndHotTracks;
    private final List<? extends ImageResource> topFiftyTracks;

    ChartItem(List<? extends ImageResource> newAndHotTracks, List<? extends ImageResource> topFiftyTracks) {
        super(Kind.ChartItem);
        this.newAndHotTracks = newAndHotTracks;
        this.topFiftyTracks = topFiftyTracks;
    }

    public List<? extends ImageResource> getNewAndHotTracks() {
        return newAndHotTracks;
    }

    public List<? extends ImageResource> getTopFiftyTracks() {
        return topFiftyTracks;
    }
}
