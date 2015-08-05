package com.soundcloud.android.playback;

import com.soundcloud.android.model.Urn;
import com.soundcloud.android.tracks.TrackProperty;
import com.soundcloud.android.utils.ScTextUtils;
import com.soundcloud.java.collections.PropertySet;
import com.soundcloud.java.objects.MoreObjects;

public final class PlayQueueItem {

    private final Urn trackUrn;
    private final Urn reposter;
    private final String source;
    private final String sourceVersion;

    private final PropertySet metaData;
    private final boolean shouldPersist;

    public static PlayQueueItem fromTrack(Urn trackUrn) {
        return fromTrack(trackUrn, Urn.NOT_SET, ScTextUtils.EMPTY_STRING, ScTextUtils.EMPTY_STRING);
    }

    public static PlayQueueItem fromTrack(PropertySet track, String source, String sourceVersion) {
        return new PlayQueueItem(track.get(TrackProperty.URN), track.getOrElse(TrackProperty.REPOSTER_URN, Urn.NOT_SET),
                source, sourceVersion, PropertySet.create(), true);
    }

    public static PlayQueueItem fromTrack(Urn trackUrn, String source, String sourceVersion) {
        return new PlayQueueItem(trackUrn, Urn.NOT_SET, source, sourceVersion, PropertySet.create(), true);
    }

    public static PlayQueueItem fromTrack(Urn trackUrn, Urn reposter, String source, String sourceVersion) {
        return new PlayQueueItem(trackUrn, reposter, source, sourceVersion, PropertySet.create(), true);
    }

    public static PlayQueueItem fromTrack(Urn trackUrn, Urn reposter, String source, String sourceVersion, PropertySet metaData, boolean shouldPersist) {
        return new PlayQueueItem(trackUrn, reposter, source, sourceVersion, metaData, shouldPersist);
    }

    private PlayQueueItem(Urn trackUrn, Urn reposter, String source, String sourceVersion, PropertySet metaData, boolean shouldPersist) {
        this.trackUrn = trackUrn;
        this.reposter = reposter;
        this.source = source;
        this.sourceVersion = sourceVersion;
        this.metaData = metaData;
        this.shouldPersist = shouldPersist;
    }

    public Urn getTrackUrn() {
        return trackUrn;
    }

    public Urn getReposter() {
        return reposter;
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
        return MoreObjects.equal(trackUrn, that.trackUrn) && MoreObjects.equal(source, that.source)
                && MoreObjects.equal(sourceVersion, that.sourceVersion);
    }

    @Override
    public int hashCode() {
        return MoreObjects.hashCode(trackUrn, source, sourceVersion);
    }

    public boolean shouldPersist() {
        return shouldPersist;
    }
}
