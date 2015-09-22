package com.soundcloud.android.playback.ui;

import com.soundcloud.android.ads.AdProperty;
import com.soundcloud.android.ads.InterstitialProperty;
import com.soundcloud.android.ads.LeaveBehindProperty;
import com.soundcloud.android.model.Urn;
import com.soundcloud.java.collections.PropertySet;
import org.jetbrains.annotations.NotNull;

final class TrackPageData {
    private final int positionInPlayQueue;
    private final Urn trackUrn;
    private final PropertySet properties;
    private final Urn relatedToTrack;

    TrackPageData(int positionInPlayQueue, @NotNull Urn trackUrn, PropertySet properties, Urn relatedToTrack) {
        this.positionInPlayQueue = positionInPlayQueue;
        this.trackUrn = trackUrn;
        this.properties = properties;
        this.relatedToTrack = relatedToTrack;
    }

    public PropertySet getProperties() {
        return properties;
    }

    public Urn getTrackUrn() {
        return trackUrn;
    }

    public int getPositionInPlayQueue() {
        return positionInPlayQueue;
    }

    public boolean hasRelatedTrack() {
        return relatedToTrack.isTrack();
    }

    public Urn getRelatedTrackUrn() {
        return relatedToTrack;
    }

    boolean isAdPage(){
        return properties.contains(AdProperty.AD_URN);
    }

    boolean hasAdOverlay() {
        return properties.contains(LeaveBehindProperty.LEAVE_BEHIND_URN)
                || properties.contains(InterstitialProperty.INTERSTITIAL_URN);
    }

    @Override
    public String toString() {
        return "ViewPageData{" +
                "positionInPlayQueue=" + positionInPlayQueue +
                ", trackUrn=" + trackUrn +
                ", properties=" + properties +
                '}';
    }
}
