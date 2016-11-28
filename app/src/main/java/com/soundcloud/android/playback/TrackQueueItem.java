package com.soundcloud.android.playback;

import com.soundcloud.android.ads.AdData;
import com.soundcloud.android.model.Urn;
import com.soundcloud.java.collections.PropertySet;
import com.soundcloud.java.objects.MoreObjects;
import com.soundcloud.java.optional.Optional;

public class TrackQueueItem extends PlayableQueueItem {

    public TrackQueueItem(Urn trackUrn,
                          Urn reposter,
                          Urn relatedEntity,
                          String source,
                          String sourceVersion,
                          Optional<AdData> adData,
                          boolean shouldPersist,
                          Urn sourceUrn,
                          Urn queryUrn,
                          boolean blocked,
                          PlaybackContext playbackContext) {
        super(trackUrn,
              reposter,
              source,
              sourceVersion,
              queryUrn,
              relatedEntity,
              blocked,
              shouldPersist,
              sourceUrn,
              adData,
              playbackContext);
    }

    @Override
    public Kind getKind() {
        return Kind.TRACK;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                          .add("urn", urn)
                          .add("reposter", reposter)
                          .add("relatedEntity", relatedEntity)
                          .add("source", source)
                          .add("sourceVersion", sourceVersion)
                          .add("sourceUrn", sourceUrn)
                          .add("queryUrn", queryUrn)
                          .add("shouldPersist", shouldPersist)
                          .add("blocked", blocked)
                          .toString();
    }

    public static class Builder extends PlayableQueueItem.Builder<Builder> {

        public Builder(Urn track) {
            super(track);
        }

        public Builder(PropertySet track) {
            super(track);
        }

        public Builder(Urn playable, Urn reposter) {
            super(playable, reposter);
        }

        public Builder(TrackQueueItem monetizableItem) {
            super(monetizableItem.urn);
            reposter = monetizableItem.reposter;
            relatedEntity = monetizableItem.relatedEntity;
            shouldPersist = monetizableItem.shouldPersist;
            blocked = monetizableItem.blocked;
            adData = monetizableItem.adData;
            playbackContext = monetizableItem.playbackContext;
            copySourceAndPlaybackContext(monetizableItem);
        }

        @Override
        protected Builder getThis() {
            return this;
        }

        public TrackQueueItem build() {
            return new TrackQueueItem(playable, reposter, relatedEntity, source, sourceVersion, adData, shouldPersist,
                                      sourceUrn, queryUrn, blocked, playbackContext);
        }
    }
}
