package com.soundcloud.android.playback;

import com.google.common.base.Objects;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.utils.ScTextUtils;
import com.soundcloud.propeller.PropertySet;

public final class PlayQueueItem {

    private final Urn trackUrn;
    private final String source;
    private final String sourceVersion;

    private final PropertySet metaData;
    private final boolean shouldPersist;

    public static PlayQueueItem fromTrack(Urn trackUrn) {
        return fromTrack(trackUrn, ScTextUtils.EMPTY_STRING, ScTextUtils.EMPTY_STRING);
    }

    public static PlayQueueItem fromTrack(Urn trackUrn, String source, String sourceVersion) {
        return new PlayQueueItem(trackUrn, source, sourceVersion, PropertySet.create(), true);
    }

    public static PlayQueueItem fromTrack(Urn trackUrn, String source, String sourceVersion, PropertySet metaData, boolean shouldPersist) {
        return new PlayQueueItem(trackUrn, source, sourceVersion, metaData, shouldPersist);
    }

    private PlayQueueItem(Urn trackUrn, String source, String sourceVersion, PropertySet metaData, boolean shouldPersist) {
        this.trackUrn = trackUrn;
        this.source = source;
        this.sourceVersion = sourceVersion;
        this.metaData = metaData;
        this.shouldPersist = shouldPersist;
    }

    public Urn getTrackUrn() {
        return trackUrn;
    }

    public String getSource() {
        return source;
    }

    public String getSourceVersion() {
        return sourceVersion;
    }

    public PropertySet getMetaData() {
        return metaData;
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

    public boolean shouldPersist() {
        return shouldPersist;
    }
}
