package com.soundcloud.android.playback;

import com.soundcloud.android.model.PostProperty;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.tracks.TrackProperty;
import com.soundcloud.android.utils.ScTextUtils;
import com.soundcloud.java.collections.PropertySet;
import com.soundcloud.java.objects.MoreObjects;

public final class PlayQueueItem {

    private final Urn trackUrn;
    private final Urn reposter;
    private final Urn relatedEntity;
    private final String source;
    private final String sourceVersion;
    private final boolean shouldPersist;
    private PropertySet metaData;

    private PlayQueueItem(Urn trackUrn, Urn reposter, Urn relatedEntity, String source, String sourceVersion,
                          PropertySet metaData, boolean shouldPersist) {
        this.trackUrn = trackUrn;
        this.reposter = reposter;
        this.relatedEntity = relatedEntity;
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

    public void setMetaData(PropertySet metaData) {
        this.metaData = metaData;
    }

    public Urn getRelatedEntity() {
        return relatedEntity;
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

    public static class Builder {
        private final Urn track;
        private final Urn reposter;
        private String source = ScTextUtils.EMPTY_STRING;
        private String sourceVersion = ScTextUtils.EMPTY_STRING;
        private PropertySet adData = PropertySet.create();
        private Urn relatedEntity = Urn.NOT_SET;
        private boolean shouldPersist = true;

        public Builder(Urn track) {
            this(track, Urn.NOT_SET);
        }

        public Builder(PropertySet track) {
            this(track.get(TrackProperty.URN),
                    track.getOrElse(PostProperty.REPOSTER_URN, Urn.NOT_SET));
        }

        public Builder(Urn track, Urn reposter) {
            this.track = track;
            this.reposter = reposter;
        }

        public Builder fromSource(String source, String sourceVersion) {
            this.source = source;
            this.sourceVersion = sourceVersion;
            return this;
        }

        public Builder withAdData(PropertySet adData){
            this.adData = adData;
            return this;
        }

        public Builder persist(boolean shouldPersist) {
            this.shouldPersist = shouldPersist;
            return this;
        }

        public Builder relatedEntity(Urn relatedEntity) {
            this.relatedEntity = relatedEntity;
            return this;
        }

        public PlayQueueItem build(){
            return new PlayQueueItem(track, reposter, relatedEntity, source, sourceVersion, adData, shouldPersist);
        }
    }
}
