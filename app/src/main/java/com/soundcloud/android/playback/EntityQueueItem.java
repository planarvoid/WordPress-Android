package com.soundcloud.android.playback;

import com.soundcloud.android.ads.AdData;
import com.soundcloud.android.model.PostProperty;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.tracks.TrackProperty;
import com.soundcloud.android.utils.ScTextUtils;
import com.soundcloud.java.collections.PropertySet;
import com.soundcloud.java.optional.Optional;

public abstract class EntityQueueItem extends PlayQueueItem {

    protected final Urn urn;
    protected final Urn reposter;
    protected final Urn relatedEntity;
    protected final String source;
    protected final String sourceVersion;
    protected final Urn sourceUrn;
    protected final Urn queryUrn;
    protected final boolean shouldPersist;
    protected final boolean blocked;

    public EntityQueueItem(Urn urn, Urn reposter, String source, String sourceVersion, Urn queryUrn, Urn relatedEntity, boolean blocked, boolean shouldPersist, Urn sourceUrn, Optional<AdData> adData) {
        this.sourceVersion = sourceVersion;
        this.source = source;
        this.queryUrn = queryUrn;
        this.blocked = blocked;
        this.shouldPersist = shouldPersist;
        this.sourceUrn = sourceUrn;
        this.reposter = reposter;
        this.relatedEntity = relatedEntity;
        this.urn = urn;
        super.setAdData(adData);
    }

    public Urn getUrn() {
        return urn;
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
    public abstract Kind getKind();

    public static abstract class Builder <T extends Builder<T>> {
        protected final Urn playable;
        protected final Urn reposter;
        protected boolean blocked;
        protected String source = ScTextUtils.EMPTY_STRING;
        protected String sourceVersion = ScTextUtils.EMPTY_STRING;
        protected Optional<AdData> adData = Optional.absent();
        protected Urn relatedEntity = Urn.NOT_SET;
        protected Urn sourceUrn = Urn.NOT_SET;
        protected Urn queryUrn = Urn.NOT_SET;
        protected boolean shouldPersist = true;

        public Builder(Urn track) {
            this(track, Urn.NOT_SET);
        }

        public Builder(PropertySet track) {
            this(track.get(TrackProperty.URN),
                    track.getOrElse(PostProperty.REPOSTER_URN, Urn.NOT_SET));
        }

        public Builder(Urn playable, Urn reposter) {
            this.playable = playable;
            this.reposter = reposter;
        }

        public T fromSource(String source, String sourceVersion) {
            this.source = source;
            this.sourceVersion = sourceVersion;
            return getThis();
        }

        public T fromSource(String source, String sourceVersion, Urn sourceUrn, Urn queryUrn) {
            this.source = source;
            this.sourceVersion = sourceVersion;
            this.sourceUrn = sourceUrn;
            this.queryUrn = queryUrn;
            return getThis();
        }

        public T copySource(EntityQueueItem entityQueueItem) {
            this.source = entityQueueItem.getSource();
            this.sourceVersion = entityQueueItem.getSourceVersion();
            this.sourceUrn = entityQueueItem.getSourceUrn();
            this.queryUrn = entityQueueItem.getQueryUrn();
            return getThis();
        }

        public T withAdData(AdData adData){
            this.adData = Optional.of(adData);
            return getThis();
        }

        public T blocked(boolean blocked) {
            this.blocked = blocked;
            return getThis();
        }

        public T persist(boolean shouldPersist) {
            this.shouldPersist = shouldPersist;
            return getThis();
        }

        public T relatedEntity(Urn relatedEntity) {
            this.relatedEntity = relatedEntity;
            return getThis();
        }

        protected abstract T getThis();
    }
}
