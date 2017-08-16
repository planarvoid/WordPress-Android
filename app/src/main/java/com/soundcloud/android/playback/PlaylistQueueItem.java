package com.soundcloud.android.playback;

import com.soundcloud.android.ads.AdData;
import com.soundcloud.android.model.Urn;
import com.soundcloud.java.objects.MoreObjects;
import com.soundcloud.java.optional.Optional;

public class PlaylistQueueItem extends PlayableQueueItem {

    PlaylistQueueItem(Urn playlistUrn,
                      Urn reposter,
                      Urn relatedEntity,
                      String source,
                      String sourceVersion,
                      Optional<AdData> adData,
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
              sourceUrn,
              adData,
              playbackContext,
              played);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                          .add("urn", urn)
                          .add("reposter", reposter)
                          .add("source", source)
                          .add("sourceVersion", sourceVersion)
                          .add("queryUrn", queryUrn)
                          .add("relatedEntity", relatedEntity)
                          .add("blocked", blocked)
                          .add("sourceUrn", sourceUrn)
                          .add("adData", adData)
                          .add("playbackContext", playbackContext)
                          .add("played", played)
                          .toString();
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
                                         sourceUrn,
                                         queryUrn,
                                         blocked,
                                         playbackContext,
                                         played);
        }
    }
}
