package com.soundcloud.android.playlists;


public class PlaylistDetailItem {
    private final PlaylistDetailItem.Kind kind;

    PlaylistDetailItem(PlaylistDetailItem.Kind kind) {
        this.kind = kind;
    }

    public enum Kind {
        HeaderItem,
        TrackItem,
        EditItem,
        UpsellItem,
        OtherPlaylists
    }

    public PlaylistDetailItem.Kind getPlaylistItemKind() {
        return kind;
    }
}
