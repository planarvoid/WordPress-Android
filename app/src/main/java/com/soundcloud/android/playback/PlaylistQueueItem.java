package com.soundcloud.android.playback;

import com.soundcloud.android.ads.AdData;
import com.soundcloud.android.model.Urn;
import com.soundcloud.java.collections.PropertySet;
import com.soundcloud.java.optional.Optional;

public class PlaylistQueueItem extends EntityQueueItem {

    PlaylistQueueItem(Urn trackUrn,
                      Urn reposter,
                      Urn relatedEntity,
                      String source,
                      String sourceVersion,
                      Optional<AdData> adData,
                      boolean shouldPersist,
                      Urn sourceUrn,
                      Urn queryUrn,
                      boolean blocked) {
        super(trackUrn, reposter, source, sourceVersion, queryUrn, relatedEntity, blocked, shouldPersist, sourceUrn, adData);
    }

    @Override
    public Kind getKind() {
        return Kind.PLAYLIST;
    }

    public static class Builder extends EntityQueueItem.Builder<Builder> {

        public Builder(Urn track) {
            super(track);
        }

        public Builder(PropertySet track) {
            super(track);
        }

        public Builder(Urn playable, Urn reposter) {
            super(playable, reposter);
        }

        @Override
        protected Builder getThis() {
            return this;
        }

        public PlaylistQueueItem build() {
            return new PlaylistQueueItem(playable, reposter, relatedEntity, source, sourceVersion, adData, shouldPersist,
                    sourceUrn, queryUrn, blocked);
        }
    }
}
