package com.soundcloud.android.discovery;

import com.soundcloud.android.image.ImageResource;
import com.soundcloud.android.model.Urn;

import java.util.List;

class ChartListItem {

    private final List<? extends ImageResource> trackArtworks;
    private final Urn genre;

    ChartListItem(List<? extends ImageResource> trackArtworks, Urn genre) {
        this.trackArtworks = trackArtworks;
        this.genre = genre;
    }

    List<? extends ImageResource> getTrackArtworks() {
        return trackArtworks;
    }

    Urn getGenre() {
        return genre;
    }
}
