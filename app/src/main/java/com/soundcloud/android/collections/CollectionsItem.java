package com.soundcloud.android.collections;

import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playlists.PlaylistItem;
import com.soundcloud.android.presentation.ListItem;
import com.soundcloud.java.collections.PropertySet;

import java.util.List;

public class CollectionsItem implements ListItem {

    static final int TYPE_LIKES = 0;
    static final int TYPE_PLAYLIST_HEADER = 1;
    static final int TYPE_PLAYLIST_ITEM = 2;

    private final int type;
    private final List<Urn> likes;
    private final PlaylistItem playlistItem;

    private CollectionsItem(int type, List<Urn> likes, PlaylistItem playlistItem) {
        this.type = type;
        this.likes = likes;
        this.playlistItem = playlistItem;
    }

    public static CollectionsItem fromLikes(List<Urn> likes) {
        return new CollectionsItem(CollectionsItem.TYPE_LIKES, likes, null);
    }

    public static CollectionsItem fromPlaylistHeader() {
        return new CollectionsItem(CollectionsItem.TYPE_PLAYLIST_HEADER, null, null);
    }

    public static CollectionsItem fromPlaylistItem(PlaylistItem playlistItem) {
        return new CollectionsItem(CollectionsItem.TYPE_PLAYLIST_ITEM, null, playlistItem);
    }

    public int getType() {
        return type;
    }

    public PlaylistItem getPlaylistItem() {
        return playlistItem;
    }

    public List<Urn> getLikes() {
        return likes;
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
