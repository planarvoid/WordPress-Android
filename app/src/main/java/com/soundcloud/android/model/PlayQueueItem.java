package com.soundcloud.android.model;

import com.google.common.base.Objects;

public class PlayQueueItem {

    private long trackId;
    private String source;
    private String sourceVersion;

    public PlayQueueItem(long trackId, String source, String sourceVersion) {
        this.trackId = trackId;
        this.source = source;
        this.sourceVersion = sourceVersion;
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
