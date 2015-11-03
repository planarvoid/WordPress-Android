package com.soundcloud.android.playback.ui;

import com.soundcloud.android.ads.InterstitialProperty;
import com.soundcloud.android.ads.LeaveBehindProperty;
import com.soundcloud.android.model.Urn;
import com.soundcloud.java.collections.PropertySet;
import org.jetbrains.annotations.NotNull;

final class TrackPageData extends PlayerPageData {
    private final Urn trackUrn;
    private final Urn collectionUrn;

    TrackPageData(int positionInPlayQueue, @NotNull Urn trackUrn, Urn collectionUrn, PropertySet properties) {
        super(Kind.TRACK, positionInPlayQueue, properties);
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
        return properties.contains(LeaveBehindProperty.LEAVE_BEHIND_URN)
                || properties.contains(InterstitialProperty.INTERSTITIAL_URN);
    }

    @Override
    public String toString() {
        return "TrackPageData{" +
                "positionInPlayQueue=" + positionInPlayQueue +
                ", trackUrn=" + trackUrn +
                ", properties=" + properties +
                '}';
    }
}
