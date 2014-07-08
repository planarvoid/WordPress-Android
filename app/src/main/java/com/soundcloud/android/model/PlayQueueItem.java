package com.soundcloud.android.model;

import com.google.common.base.Objects;

public class PlayQueueItem {

    private final long trackId;
    private final String source;
    private final String sourceVersion;
    private final boolean isAudioAd;

    public PlayQueueItem(long trackId, String source, String sourceVersion) {
        this(trackId, source, sourceVersion, false);
    }

    public PlayQueueItem(long trackId, String source, String sourceVersion, boolean isAudioAd) {
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
