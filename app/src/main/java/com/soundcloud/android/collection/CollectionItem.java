package com.soundcloud.android.collection;

import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playlists.PlaylistItem;
import com.soundcloud.android.presentation.ListItem;
import com.soundcloud.java.collections.PropertySet;
import com.soundcloud.java.objects.MoreObjects;

import java.util.List;

public class CollectionItem implements ListItem {

    static final int TYPE_COLLECTIONS_PREVIEW = 0;
    static final int TYPE_PLAYLIST_HEADER = 1;
    static final int TYPE_PLAYLIST_ITEM = 2;
    static final int TYPE_REMOVE_FILTER = 3;
    static final int TYPE_EMPTY_PLAYLISTS = 4;
    static final int TYPE_ONBOARDING = 5;

    private final int type;
    private final LikesItem likes;
    private final List<Urn> stations;
    private final PlaylistItem playlistItem;

    private CollectionItem(int type, LikesItem likes, List<Urn> stations, PlaylistItem playlistItem) {
        this.type = type;
        this.likes = likes;
        this.stations = stations;
        this.playlistItem = playlistItem;
    }

    // TODO avoid null (CollectionItem<T> { T getEntity() ; getType()}) or use @nullable
    public static CollectionItem fromCollectionsPreview(LikesItem likes, List<Urn> stations) {
        return new CollectionItem(CollectionItem.TYPE_COLLECTIONS_PREVIEW, likes, stations, null);
    }

    public static CollectionItem fromPlaylistHeader() {
        return new CollectionItem(CollectionItem.TYPE_PLAYLIST_HEADER, null, null, null);
    }

    public static CollectionItem fromKillFilter() {
        return new CollectionItem(CollectionItem.TYPE_REMOVE_FILTER, null, null, null);
    }

    public static CollectionItem fromEmptyPlaylists() {
        return new CollectionItem(CollectionItem.TYPE_EMPTY_PLAYLISTS, null, null, null);
    }

    public static CollectionItem fromPlaylistItem(PlaylistItem playlistItem) {
        return new CollectionItem(CollectionItem.TYPE_PLAYLIST_ITEM, null, null, playlistItem);
    }

    public static CollectionItem fromOnboarding() {
        return new CollectionItem(CollectionItem.TYPE_ONBOARDING, null, null, null);
    }

    public int getType() {
        return type;
    }

    public PlaylistItem getPlaylistItem() {
        return playlistItem;
    }

    public LikesItem getLikes() {
        return likes;
    }

    public List<Urn> getStations() {
        return stations;
    }

    public boolean isPlaylistItem() {
        return type == TYPE_PLAYLIST_ITEM;
    }

    public boolean isCollectionPreview() {
        return type == TYPE_COLLECTIONS_PREVIEW;
    }

    @Override
    public ListItem update(PropertySet sourceSet) {
        if (type == TYPE_PLAYLIST_ITEM) {
            playlistItem.update(sourceSet);
        } else if (type == TYPE_COLLECTIONS_PREVIEW) {
            likes.update(sourceSet);
        }
        return this;
    }

    @Override
    public Urn getEntityUrn() {
        return type == TYPE_PLAYLIST_ITEM ? playlistItem.getEntityUrn() : Urn.NOT_SET;
    }

    @Override
    public final boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof CollectionItem)) {
            return false;
        }
        CollectionItem that = (CollectionItem) o;
        return MoreObjects.equal(type, that.type) &&
                MoreObjects.equal(likes, that.likes) &&
                MoreObjects.equal(playlistItem, that.playlistItem);
    }

    @Override
    public final int hashCode() {
        return MoreObjects.hashCode(type, likes, playlistItem);
    }
}
