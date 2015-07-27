package com.soundcloud.android.playlists;

import com.soundcloud.android.Consts;
import com.soundcloud.android.model.Urn;
import com.soundcloud.java.objects.MoreObjects;

final class AddTrackToPlaylistItem {
    public final Urn playlistUrn;
    public final String title;
    public final int trackCount;
    public final boolean isPrivate;
    public final boolean isOffline;
    public final boolean isTrackAdded;

    public AddTrackToPlaylistItem(Urn urn, String title, int trackCount,
                                  boolean isPrivate, boolean isOffline, boolean isTrackAdded) {
        this.playlistUrn = urn;
        this.title = title;
        this.trackCount = trackCount;
        this.isPrivate = isPrivate;
        this.isOffline = isOffline;
        this.isTrackAdded = isTrackAdded;
    }

    public static AddTrackToPlaylistItem createNewPlaylistItem() {
        return new AddTrackToPlaylistItem(Urn.NOT_SET, null, Consts.NOT_SET, false, false, false);
    }

    @Override
    public boolean equals(Object o) {

        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        AddTrackToPlaylistItem that = (AddTrackToPlaylistItem) o;

        return MoreObjects.equal(playlistUrn, that.playlistUrn) &&
                MoreObjects.equal(title, that.title) &&
                MoreObjects.equal(trackCount, that.trackCount) &&
                MoreObjects.equal(isPrivate, that.isPrivate) &&
                MoreObjects.equal(isOffline, that.isOffline) &&
                MoreObjects.equal(isTrackAdded, that.isTrackAdded);
    }

    @Override
    public int hashCode() {
        return MoreObjects.hashCode(playlistUrn, title, trackCount, isPrivate, isOffline, isTrackAdded);
    }
}
