package com.soundcloud.android.playback;

import com.soundcloud.android.ads.AdData;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playback.PlaybackContext.Bucket;
import com.soundcloud.java.optional.Optional;
import com.soundcloud.java.strings.Strings;

public abstract class PlayableQueueItem extends PlayQueueItem {

    protected final Urn urn;
    protected final Urn reposter;
    protected final Urn relatedEntity;
    protected final String source;
    protected final String sourceVersion;
    protected final Urn sourceUrn;
    protected final Urn queryUrn;
    protected final boolean blocked;
    protected final PlaybackContext playbackContext;
    protected boolean played;

    public PlayableQueueItem(Urn urn,
                             Urn reposter,
                             String source,
                             String sourceVersion,
                             Urn queryUrn,
                             Urn relatedEntity,
                             boolean blocked,
                             Urn sourceUrn,
                             Optional<AdData> adData,
                             PlaybackContext playbackContext, boolean played) {
        this.sourceVersion = sourceVersion;
        this.source = source;
        this.queryUrn = queryUrn;
        this.blocked = blocked;
        this.sourceUrn = sourceUrn;
        this.reposter = reposter;
        this.relatedEntity = relatedEntity;
        this.urn = urn;
        this.playbackContext = playbackContext;
        this.played = played;
        this.adData = adData;

        if (playbackContext == null) {
            throw new IllegalArgumentException("PlaybackContext can not be null");
        }
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

    public boolean isBlocked() {
        return blocked;
    }

    public boolean isPlayed() {
        return played;
    }

    public void setPlayed() {
        this.played = true;
    }

    public PlaybackContext getPlaybackContext() {
        return playbackContext;
    }

    public boolean isBucket(Bucket bucket) {
        return playbackContext.bucket().equals(bucket);
    }

    public boolean isVisible() {
        return played || !isBucket(Bucket.AUTO_PLAY);
    }

    @Override
    public abstract Kind getKind();

    public static abstract class Builder<T extends Builder<T>> {
        protected final Urn playable;
        protected Urn reposter;
        protected boolean blocked;
        protected String source = Strings.EMPTY;
        protected String sourceVersion = Strings.EMPTY;
        protected Optional<AdData> adData = Optional.absent();
        protected Urn relatedEntity = Urn.NOT_SET;
        protected Urn sourceUrn = Urn.NOT_SET;
        protected Urn queryUrn = Urn.NOT_SET;
        protected PlaybackContext playbackContext;
        protected boolean played;

        public Builder(Urn entityUrn) {
            this(entityUrn, Urn.NOT_SET);
        }

        public Builder(PlayableWithReposter playableAndReposter) {
            this(playableAndReposter.getUrn(),
                 playableAndReposter.getReposterUrn().or(Urn.NOT_SET));
        }

        public Builder(Urn playable, Urn reposter) {
            this.playable = playable;
            this.reposter = reposter;
        }

        public T withPlaybackContext(PlaybackContext playbackContext) {
            this.playbackContext = playbackContext;
            return getThis();
        }

        public T fromSource(String source) {
            this.source = source;
            return getThis();
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

        public T copySourceAndPlaybackContext(PlayableQueueItem playableQueueItem) {
            this.source = playableQueueItem.getSource();
            this.sourceVersion = playableQueueItem.getSourceVersion();
            this.sourceUrn = playableQueueItem.getSourceUrn();
            this.queryUrn = playableQueueItem.getQueryUrn();
            this.playbackContext = playableQueueItem.playbackContext;
            return getThis();
        }

        public T withAdData(AdData adData) {
            this.adData = Optional.of(adData);
            return getThis();
        }

        public T withoutAdData() {
            this.adData = Optional.absent();
            return getThis();
        }

        public T blocked(boolean blocked) {
            this.blocked = blocked;
            return getThis();
        }

        public T played(boolean played) {
            this.played = played;
            return getThis();
        }

        public T relatedEntity(Urn relatedEntity) {
            this.relatedEntity = relatedEntity;
            return getThis();
        }

        protected abstract T getThis();
    }
}
