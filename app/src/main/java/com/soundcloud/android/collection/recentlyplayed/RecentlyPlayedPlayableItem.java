package com.soundcloud.android.collection.recentlyplayed;

import com.google.auto.value.AutoValue;
import com.soundcloud.android.image.ImageResource;
import com.soundcloud.android.model.Urn;
import com.soundcloud.java.optional.Optional;

@AutoValue
public abstract class RecentlyPlayedPlayableItem extends RecentlyPlayedItem implements ImageResource {

    public static RecentlyPlayedPlayableItem create(Urn urn,
                                                    Optional<String> imageUrl,
                                                    String title,
                                                    int trackCount,
                                                    boolean isAlbum) {
        return new AutoValue_RecentlyPlayedPlayableItem(urn, imageUrl, kindFor(urn), title, trackCount, isAlbum);
    }

    public abstract String getTitle();

    public abstract int getTrackCount();

    public abstract boolean isAlbum();

    private static Kind kindFor(Urn urn) {
        if (urn.isPlaylist()) {
            return Kind.RecentlyPlayedPlaylist;
        } else if (urn.isStation()) {
            return Kind.RecentlyPlayedStation;
        } else if (urn.isUser()) {
            return Kind.RecentlyPlayedProfile;
        } else {
            throw new IllegalArgumentException("Unexpected urn: " + urn);
        }
    }

}
