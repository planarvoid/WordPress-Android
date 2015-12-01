package com.soundcloud.android.playback.ui;

import com.soundcloud.android.ads.AdData;
import com.soundcloud.android.ads.OverlayAdData;
import com.soundcloud.android.model.Urn;
import com.soundcloud.java.optional.Optional;

import org.jetbrains.annotations.NotNull;

final class TrackPageData extends PlayerPageData {
    private final Urn trackUrn;
    private final Urn collectionUrn;

    TrackPageData(int positionInPlayQueue, @NotNull Urn trackUrn, Urn collectionUrn, Optional<AdData> adData) {
        super(Kind.TRACK, positionInPlayQueue, adData);
        this.trackUrn = trackUrn;
        this.collectionUrn = collectionUrn;
    }

    public Urn getTrackUrn() {
        return trackUrn;
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
                ", trackUrn=" + trackUrn +
                ", adData=" + adData+
                '}';
    }
}
