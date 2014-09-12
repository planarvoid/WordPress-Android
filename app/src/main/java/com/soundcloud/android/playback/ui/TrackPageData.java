package com.soundcloud.android.playback.ui;

import com.soundcloud.android.tracks.TrackUrn;
import com.soundcloud.propeller.PropertySet;
import org.jetbrains.annotations.NotNull;

class TrackPageData {
    private final int positionInPlayQueue;
    private final TrackUrn trackUrn;
    private PropertySet audioAd;

    static TrackPageData forTrack(int positionInPlayQueue, @NotNull TrackUrn trackUrn) {
        return new TrackPageData(positionInPlayQueue, trackUrn, null);
    }

    static TrackPageData forAd(int positionInPlayQueue, @NotNull TrackUrn trackUrn, PropertySet audioAd) {
        return new TrackPageData(positionInPlayQueue, trackUrn, audioAd);
    }

    private TrackPageData(int positionInPlayQueue, @NotNull TrackUrn trackUrn, PropertySet audioAd) {
        this.positionInPlayQueue = positionInPlayQueue;
        this.trackUrn = trackUrn;
        this.audioAd = audioAd;
    }

    public PropertySet getAudioAd() {
        return audioAd;
    }

    public TrackUrn getTrackUrn() {
        return trackUrn;
    }

    public int getPositionInPlayQueue() {
        return positionInPlayQueue;
    }

    boolean isAdPage(){
        return audioAd != null;
    }

    @Override
    public String toString() {
        return "ViewPageData{" +
                "positionInPlayQueue=" + positionInPlayQueue +
                ", trackUrn=" + trackUrn +
                ", audioAd=" + audioAd +
                '}';
    }
}
