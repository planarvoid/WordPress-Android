package com.soundcloud.android.collection.recentlyplayed;

import com.soundcloud.android.image.ImageResource;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.offline.OfflineState;
import com.soundcloud.java.objects.MoreObjects;
import com.soundcloud.java.optional.Optional;

public class RecentlyPlayedPlayableItem extends RecentlyPlayedItem implements ImageResource {

    private final Urn urn;
    private final Optional<String> imageUrl;
    private final String title;
    private final int trackCount;
    private final boolean isAlbum;
    private Optional<OfflineState> offlineState;
    private final long timestamp;

    public RecentlyPlayedPlayableItem(Urn urn,
                                      Optional<String> imageUrl,
                                      String title,
                                      int trackCount,
                                      boolean isAlbum,
                                      Optional<OfflineState> offlineState,
                                      long timestamp) {

        this.urn = urn;
        this.imageUrl = imageUrl;
        this.title = title;
        this.trackCount = trackCount;
        this.isAlbum = isAlbum;
        this.offlineState = offlineState;
        this.timestamp = timestamp;
    }

    public String getTitle() {
        return title;
    }

    public int getTrackCount() {
        return trackCount;
    }

    public boolean isAlbum() {
        return isAlbum;
    }

    public Optional<OfflineState> getOfflineState() {
        return offlineState;
    }

    public void setOfflineState(OfflineState offlineState) {
        this.offlineState = Optional.of(offlineState);
    }

    @Override
    public Urn getUrn() {
        return urn;
    }

    @Override
    public Optional<String> getImageUrlTemplate() {
        return imageUrl;
    }

    public long getTimestamp() {
        return timestamp;
    }

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

    @Override
    Kind getKind() {
        return kindFor(getUrn());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        RecentlyPlayedPlayableItem that = (RecentlyPlayedPlayableItem) o;
        return trackCount == that.trackCount &&
                isAlbum == that.isAlbum &&
                MoreObjects.equal(urn, that.urn) &&
                MoreObjects.equal(imageUrl, that.imageUrl) &&
                MoreObjects.equal(title, that.title) &&
                MoreObjects.equal(offlineState, that.offlineState);
    }

    @Override
    public int hashCode() {
        return MoreObjects.hashCode(urn, imageUrl, title, trackCount, isAlbum, offlineState);
    }
}
