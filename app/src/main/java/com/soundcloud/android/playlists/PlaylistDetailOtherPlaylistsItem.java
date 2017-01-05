package com.soundcloud.android.playlists;

import com.soundcloud.java.objects.MoreObjects;

import java.util.List;

class PlaylistDetailOtherPlaylistsItem extends PlaylistDetailItem {

    private final String creatorName;
    private final List<PlaylistItem> otherPlaylists;

    PlaylistDetailOtherPlaylistsItem(String creatorName, List<PlaylistItem> otherPlaylists) {
        super(PlaylistDetailItem.Kind.OtherPlaylists);
        this.creatorName = creatorName;
        this.otherPlaylists = otherPlaylists;
    }

    List<PlaylistItem> otherPlaylists(){
        return otherPlaylists;
    }

    String getCreatorName(){
        return creatorName;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof PlaylistDetailOtherPlaylistsItem
                && MoreObjects.equal(otherPlaylists, ((PlaylistDetailOtherPlaylistsItem) o).otherPlaylists);
    }

    @Override
    public int hashCode() {
        return MoreObjects.hashCode(otherPlaylists);
    }
}
