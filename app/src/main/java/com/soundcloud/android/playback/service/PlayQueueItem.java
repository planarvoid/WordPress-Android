package com.soundcloud.android.playback.service;

import com.google.common.base.Objects;
import com.soundcloud.android.ads.AudioAd;
import com.soundcloud.android.tracks.TrackUrn;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.utils.ScTextUtils;

final class PlayQueueItem {

    private final TrackUrn trackUrn;
    private final String source;
    private final String sourceVersion;
    private final boolean isAudioAd;

    public static PlayQueueItem fromTrack(long trackId, String source, String sourceVersion){
        return new PlayQueueItem(trackId, source, sourceVersion, false);
    }

    public static PlayQueueItem fromAudioAd(AudioAd audioAd){
        // TODO : Proper source + version?
        return new PlayQueueItem(audioAd.getApiTrack().getId(), ScTextUtils.EMPTY_STRING, ScTextUtils.EMPTY_STRING, true);
    }

    private PlayQueueItem(long trackId, String source, String sourceVersion, boolean isAudioAd) {
        this.trackUrn = Urn.forTrack(trackId);
        this.source = source;
        this.sourceVersion = sourceVersion;
        this.isAudioAd = isAudioAd;
    }

    @Deprecated
    public long getTrackId() {
        return trackUrn.numericId;
    }

    public TrackUrn getTrackUrn() {
        return trackUrn;
    }

    public String getSource() {
        return source;
    }

    public String getSourceVersion() {
        return sourceVersion;
    }

    public boolean isAudioAd() {
        return isAudioAd;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        PlayQueueItem that = (PlayQueueItem) o;
        return Objects.equal(trackUrn, that.trackUrn) && Objects.equal(source, that.source)
                && Objects.equal(sourceVersion, that.sourceVersion);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(trackUrn, source, sourceVersion);
    }
}
