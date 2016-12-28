package com.soundcloud.android.playback;

import com.soundcloud.android.ads.AdData;
import com.soundcloud.android.model.Urn;
import com.soundcloud.java.optional.Optional;

public class PlaylistQueueItem extends PlayableQueueItem {

    PlaylistQueueItem(Urn playlistUrn,
                      Urn reposter,
                      Urn relatedEntity,
                      String source,
                      String sourceVersion,
                      Optional<AdData> adData,
                      boolean shouldPersist,
                      Urn sourceUrn,
                      Urn queryUrn,
                      boolean blocked,
                      PlaybackContext playbackContext,
                      boolean played) {
        super(playlistUrn,
              reposter,
              source,
              sourceVersion,
              queryUrn,
              relatedEntity,
              blocked,
              shouldPersist,
              sourceUrn,
              adData,
              playbackContext,
              played);
    }

    @Override
    public Kind getKind() {
        return Kind.PLAYLIST;
    }

    public static class Builder extends PlayableQueueItem.Builder<Builder> {

        public Builder(Urn urn) {
            super(urn);
        }

        public Builder(PlayableWithReposter playableAndReposter) {
            super(playableAndReposter);
        }

        @Override
        protected Builder getThis() {
            return this;
        }

        public PlaylistQueueItem build() {
            return new PlaylistQueueItem(playable,
                                         reposter,
                                         relatedEntity,
                                         source,
                                         sourceVersion,
                                         adData,
                                         shouldPersist,
                                         sourceUrn,
                                         queryUrn,
                                         blocked,
                                         playbackContext,
                                         played);
        }
    }
}
