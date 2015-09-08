package com.soundcloud.android.collections;

import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playlists.PlaylistItem;
import com.soundcloud.android.presentation.ListItem;
import com.soundcloud.java.collections.PropertySet;

public class CollectionsItem implements ListItem {

    static final int TYPE_LIKES = 0;
    static final int TYPE_PLAYLIST_HEADER = 1;
    static final int TYPE_PLAYLIST_ITEM = 2;

    private final int type;
    private final int likesCount;
    private final PlaylistItem playlistItem;

    private CollectionsItem(int type, int likesCount, PlaylistItem playlistItem) {
        this.type = type;
        this.likesCount = likesCount;
        this.playlistItem = playlistItem;
    }

    public static CollectionsItem fromLikes(int likesCount) {
        return new CollectionsItem(CollectionsItem.TYPE_LIKES, likesCount, null);
    }

    public static CollectionsItem fromPlaylistHeader() {
        return new CollectionsItem(CollectionsItem.TYPE_PLAYLIST_HEADER, 0, null);
    }

    public static CollectionsItem fromPlaylistItem(PlaylistItem playlistItem) {
        return new CollectionsItem(CollectionsItem.TYPE_PLAYLIST_ITEM, 0, playlistItem);
    }

    public int getType() {
        return type;
    }

    public PlaylistItem getPlaylistItem() {
        return playlistItem;
    }

    public int getLikesCount() {
        return likesCount;
    }

    public boolean isPlaylistItem() {
        return type == TYPE_PLAYLIST_ITEM;
    }

    @Override
    public ListItem update(PropertySet sourceSet) {
        if (type == TYPE_PLAYLIST_ITEM) {
            playlistItem.update(sourceSet);
        }
        return this;
    }

    @Override
    public Urn getEntityUrn() {
        return type == TYPE_PLAYLIST_ITEM ? playlistItem.getEntityUrn() : Urn.NOT_SET;
    }
}
