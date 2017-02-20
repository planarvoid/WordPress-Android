package com.soundcloud.android.playlists;


public class PlaylistDetailItem {
    private final PlaylistDetailItem.Kind kind;

    PlaylistDetailItem(PlaylistDetailItem.Kind kind) {
        this.kind = kind;
    }

    public enum Kind {
        HeaderItem,
        TrackItem,
        UpsellItem,
        OtherPlaylists,
        EmptyItem
    }

    PlaylistDetailItem.Kind getPlaylistItemKind() {
        return kind;
    }

    public boolean isTrackItem() {
        return kind == Kind.TrackItem;
    }

    static boolean isTheSameItem(PlaylistDetailItem item1, PlaylistDetailItem item2) {
        if (isTrackItem(item1) && isTrackItem(item2)) {
            return ((PlaylistDetailTrackItem) item1).getUrn().equals(((PlaylistDetailTrackItem) item2).getUrn());
        } else {
            return item1.getPlaylistItemKind() == item2.getPlaylistItemKind();
        }
    }

    private static boolean isTrackItem(PlaylistDetailItem item1) {
        return item1.getPlaylistItemKind() == Kind.TrackItem;
    }
}
