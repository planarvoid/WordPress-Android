package com.soundcloud.android.collection.recentlyplayed;

import com.google.auto.value.AutoValue;
import com.soundcloud.android.image.ImageResource;
import com.soundcloud.android.model.Urn;
import com.soundcloud.java.optional.Optional;

@AutoValue
public abstract class RecentlyPlayedItem implements ImageResource {

    static int TYPE_RECENTLY_PLAYED_PLAYLIST = 1;
    static int TYPE_RECENTLY_PLAYED_STATION = 2;
    static int TYPE_RECENTLY_PLAYED_PROFILE = 3;

    public static RecentlyPlayedItem create(Urn urn,
                                            Optional<String> imageUrl,
                                            String title,
                                            int trackCount,
                                            boolean isAlbum) {
        return new AutoValue_RecentlyPlayedItem(urn, imageUrl, title, trackCount, isAlbum);
    }


    public abstract String getTitle();

    public abstract int getTrackCount();

    public abstract boolean isAlbum();

    int getType() {
        Urn urn = getUrn();

        if (urn.isPlaylist()) {
            return TYPE_RECENTLY_PLAYED_PLAYLIST;
        } else if (urn.isStation()) {
            return TYPE_RECENTLY_PLAYED_STATION;
        } else if (urn.isUser()) {
            return TYPE_RECENTLY_PLAYED_PROFILE;
        } else {
            throw new IllegalArgumentException("Unexpected urn: " + urn);
        }
    }

}
