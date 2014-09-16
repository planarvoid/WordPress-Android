package com.soundcloud.android.playback.ui;

import com.soundcloud.android.ads.AdProperty;
import com.soundcloud.android.ads.LeaveBehindProperty;
import com.soundcloud.android.tracks.TrackUrn;
import com.soundcloud.propeller.PropertySet;
import org.jetbrains.annotations.NotNull;

final class TrackPageData {
    private final int positionInPlayQueue;
    private final TrackUrn trackUrn;
    private final PropertySet properties;

    static TrackPageData forTrack(int positionInPlayQueue, @NotNull TrackUrn trackUrn, PropertySet properties) {
        return new TrackPageData(positionInPlayQueue, trackUrn, properties);
    }

    static TrackPageData forAd(int positionInPlayQueue, @NotNull TrackUrn trackUrn, PropertySet properties) {
        return new TrackPageData(positionInPlayQueue, trackUrn, properties);
    }

    private TrackPageData(int positionInPlayQueue, @NotNull TrackUrn trackUrn, PropertySet properties) {
        this.positionInPlayQueue = positionInPlayQueue;
        this.trackUrn = trackUrn;
        this.properties = properties;
    }

    public PropertySet getProperties() {
        return properties;
    }

    public TrackUrn getTrackUrn() {
        return trackUrn;
    }

    public int getPositionInPlayQueue() {
        return positionInPlayQueue;
    }

    boolean isAdPage(){
        return properties.contains(AdProperty.AD_URN);
    }

    boolean hasLeaveBehind() {
        return properties.contains(LeaveBehindProperty.LEAVE_BEHIND_URN);
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
