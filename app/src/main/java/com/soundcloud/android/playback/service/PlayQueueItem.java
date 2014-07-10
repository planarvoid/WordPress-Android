package com.soundcloud.android.playback.service;

import com.google.common.base.Objects;
import com.soundcloud.android.ads.AudioAd;
import com.soundcloud.android.utils.ScTextUtils;

class PlayQueueItem {

    private final long trackId;
    private final String source;
    private final String sourceVersion;
    private final boolean isAudioAd;

    public static PlayQueueItem fromTrack(long trackId, String source, String sourceVersion){
        return new PlayQueueItem(trackId, source, sourceVersion, false);
    }

    public static PlayQueueItem fromAudioAd(AudioAd audioAd){
        // TODO : Proper source + version?
        return new PlayQueueItem(audioAd.getTrackSummary().getId(), ScTextUtils.EMPTY_STRING, ScTextUtils.EMPTY_STRING, true);
    }

    private PlayQueueItem(long trackId, String source, String sourceVersion, boolean isAudioAd) {
        this.trackId = trackId;
        this.source = source;
        this.sourceVersion = sourceVersion;
        this.isAudioAd = isAudioAd;
    }

    public long getTrackId() {
        return trackId;
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
        if (trackId != that.trackId) {
            return false;
        } else {
            return Objects.equal(source, that.source) && Objects.equal(sourceVersion, that.sourceVersion);
        }
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(trackId, source, sourceVersion);
    }
}
