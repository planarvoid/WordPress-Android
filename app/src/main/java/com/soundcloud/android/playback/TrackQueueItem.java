package com.soundcloud.android.playback;

import com.soundcloud.android.ads.AdData;
import com.soundcloud.android.model.PostProperty;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.tracks.TrackProperty;
import com.soundcloud.android.utils.ScTextUtils;
import com.soundcloud.java.collections.PropertySet;
import com.soundcloud.java.objects.MoreObjects;
import com.soundcloud.java.optional.Optional;

public class TrackQueueItem extends PlayQueueItem {

    private final Urn trackUrn;
    private final Urn reposter;
    private final Urn relatedEntity;
    private final String source;
    private final String sourceVersion;
    private final Urn sourceUrn;
    private final Urn queryUrn;
    private final boolean shouldPersist;
    private final boolean blocked;

    private TrackQueueItem(Urn trackUrn, Urn reposter, Urn relatedEntity, String source,
                           String sourceVersion, Optional<AdData> adData, boolean shouldPersist, Urn sourceUrn, Urn queryUrn, boolean blocked) {
        this.trackUrn = trackUrn;
        this.reposter = reposter;
        this.relatedEntity = relatedEntity;
        this.source = source;
        this.sourceVersion = sourceVersion;
        this.shouldPersist = shouldPersist;
        this.queryUrn = queryUrn;
        this.sourceUrn = sourceUrn;
        this.blocked = blocked;
        super.setAdData(adData);
    }

    public Urn getUrn() {
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

    public Urn getSourceUrn() {
        return sourceUrn;
    }

    public Urn getQueryUrn() {
        return queryUrn;
    }

    public Urn getRelatedEntity() {
        return relatedEntity;
    }

    public boolean shouldPersist() {
        return shouldPersist;
    }

    public boolean isBlocked() {
        return blocked;
    }

    @Override
    public Kind getKind() {
        return Kind.TRACK;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        TrackQueueItem that = (TrackQueueItem) o;
        return MoreObjects.equal(trackUrn, that.trackUrn) && MoreObjects.equal(source, that.source)
                && MoreObjects.equal(sourceVersion, that.sourceVersion) && getKind() == that.getKind()
                && MoreObjects.equal(blocked, that.blocked);
    }

    @Override
    public int hashCode() {
        return MoreObjects.hashCode(trackUrn, source, sourceVersion);
    }

    public static class Builder {
        private final Urn track;
        private final Urn reposter;
        private boolean blocked;
        private String source = ScTextUtils.EMPTY_STRING;
        private String sourceVersion = ScTextUtils.EMPTY_STRING;
        private Optional<AdData> adData = Optional.absent();
        private Urn relatedEntity = Urn.NOT_SET;
        private Urn sourceUrn = Urn.NOT_SET;
        private Urn queryUrn = Urn.NOT_SET;
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

        public Builder fromSource(String source, String sourceVersion, Urn sourceUrn, Urn queryUrn) {
            this.source = source;
            this.sourceVersion = sourceVersion;
            this.sourceUrn = sourceUrn;
            this.queryUrn = queryUrn;
            return this;
        }

        public Builder withAdData(AdData adData){
            this.adData = Optional.of(adData);
            return this;
        }

        public Builder blocked(boolean blocked) {
            this.blocked = blocked;
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

        public TrackQueueItem build(){
            return new TrackQueueItem(track, reposter, relatedEntity, source, sourceVersion, adData, shouldPersist, sourceUrn, queryUrn, blocked);
        }
    }
}
