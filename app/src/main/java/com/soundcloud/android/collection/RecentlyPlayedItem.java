package com.soundcloud.android.collection;

import com.google.auto.value.AutoValue;
import com.soundcloud.android.image.ImageResource;
import com.soundcloud.android.model.Urn;
import com.soundcloud.java.optional.Optional;

@AutoValue
public abstract class RecentlyPlayedItem implements ImageResource {

    public static RecentlyPlayedItem create(Urn urn, Optional<String> imageUrl, String title, int trackCount, boolean isAlbum) {
        return new AutoValue_RecentlyPlayedItem(urn, imageUrl, title, trackCount, isAlbum);
    }

    public abstract String getTitle();

    public abstract int getTrackCount();

    public abstract boolean isAlbum();
}
