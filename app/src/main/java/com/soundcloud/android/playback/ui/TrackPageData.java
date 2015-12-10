package com.soundcloud.android.playback.ui;

import com.soundcloud.android.ads.AdData;
import com.soundcloud.android.ads.OverlayAdData;
import com.soundcloud.android.model.Urn;
import com.soundcloud.java.optional.Optional;

import org.jetbrains.annotations.NotNull;

final class TrackPageData extends PlayerPageData {
    private final Urn collectionUrn;

    TrackPageData(int positionInPlayQueue, @NotNull Urn trackUrn, Urn collectionUrn, Optional<AdData> adData) {
        super(Kind.TRACK, trackUrn, positionInPlayQueue, adData);
        this.collectionUrn = collectionUrn;
    }

    public Urn getCollectionUrn() {
        return collectionUrn;
    }

    boolean hasAdOverlay() {
        return adData.isPresent() && adData.get() instanceof OverlayAdData;
    }

    @Override
    public String toString() {
        return "TrackPageData{" +
                "positionInPlayQueue=" + positionInPlayQueue +
                ", trackUrn=" + urn +
                ", adData=" + adData+
                '}';
    }
}
