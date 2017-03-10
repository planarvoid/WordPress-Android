package com.soundcloud.android.playlists;

import com.soundcloud.java.objects.MoreObjects;

import java.util.List;

class PlaylistDetailOtherPlaylistsItem extends PlaylistDetailItem {

    private final String creatorName;
    private final List<PlaylistItem> otherPlaylists;
    private final boolean isAlbum;

    PlaylistDetailOtherPlaylistsItem(String creatorName, List<PlaylistItem> otherPlaylists, boolean isAlbum) {
        super(PlaylistDetailItem.Kind.OtherPlaylists);
        this.creatorName = creatorName;
        this.otherPlaylists = otherPlaylists;
        this.isAlbum = isAlbum;
    }

    List<PlaylistItem> otherPlaylists(){
        return otherPlaylists;
    }

    String getCreatorName(){
        return creatorName;
    }

    boolean isAlbum() {
        return isAlbum;
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

    @Override
    public String toString() {
        return "PlaylistDetailOtherPlaylistsItem{" + "creatorName='" + creatorName + '\'' +
                ", otherPlaylists=" + otherPlaylists +
                '}';
    }
}
