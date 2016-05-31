package com.soundcloud.android.collection;

import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playlists.PlaylistItem;
import com.soundcloud.android.presentation.ListItem;
import com.soundcloud.android.stations.StationRecord;
import com.soundcloud.android.tracks.TrackItem;
import com.soundcloud.java.collections.PropertySet;
import com.soundcloud.java.objects.MoreObjects;
import com.soundcloud.java.optional.Optional;

import java.util.List;

class CollectionItem implements ListItem {

    static final int TYPE_COLLECTIONS_PREVIEW = 0;
    static final int TYPE_PLAYLIST_HEADER = 1;
    static final int TYPE_PLAYLIST_ITEM = 2;
    static final int TYPE_REMOVE_FILTER = 3;
    static final int TYPE_EMPTY_PLAYLISTS = 4;
    static final int TYPE_ONBOARDING = 5;
    static final int TYPE_PLAY_HISTORY_TRACKS_HEADER = 6;
    static final int TYPE_PLAY_HISTORY_TRACKS_ITEM = 7;
    static final int TYPE_PLAY_HISTORY_TRACKS_VIEW_ALL = 8;

    private final int type;
    private final LikesItem likes;
    private final List<StationRecord> stations;
    private final PlaylistItem playlistItem;
    private final TrackItem trackItem;

    private CollectionItem(int type, LikesItem likes, List<StationRecord> stations, PlaylistItem playlistItem, TrackItem trackItem) {
        this.type = type;
        this.likes = likes;
        this.stations = stations;
        this.playlistItem = playlistItem;
        this.trackItem = trackItem;
    }

    // TODO avoid null (CollectionItem<T> { T getEntity() ; getType()}) or use @nullable
    static CollectionItem fromCollectionsPreview(LikesItem likes, List<StationRecord> stations) {
        return new CollectionItem(CollectionItem.TYPE_COLLECTIONS_PREVIEW, likes, stations, null, null);
    }

    static CollectionItem fromPlaylistHeader() {
        return new CollectionItem(CollectionItem.TYPE_PLAYLIST_HEADER, null, null, null, null);
    }

    static CollectionItem fromKillFilter() {
        return new CollectionItem(CollectionItem.TYPE_REMOVE_FILTER, null, null, null, null);
    }

    static CollectionItem fromEmptyPlaylists() {
        return new CollectionItem(CollectionItem.TYPE_EMPTY_PLAYLISTS, null, null, null, null);
    }

    static CollectionItem fromPlaylistItem(PlaylistItem playlistItem) {
        return new CollectionItem(CollectionItem.TYPE_PLAYLIST_ITEM, null, null, playlistItem, null);
    }

    static CollectionItem fromOnboarding() {
        return new CollectionItem(CollectionItem.TYPE_ONBOARDING, null, null, null, null);
    }

    static CollectionItem fromPlayHistoryTracksHeader() {
        return new CollectionItem(CollectionItem.TYPE_PLAY_HISTORY_TRACKS_HEADER, null, null, null, null);
    }

    static CollectionItem fromPlayHistoryTracksItem(TrackItem trackItem) {
        return new CollectionItem(CollectionItem.TYPE_PLAY_HISTORY_TRACKS_ITEM, null, null, null, trackItem);
    }

    static CollectionItem fromPlayHistoryTracksViewAll() {
        return new CollectionItem(CollectionItem.TYPE_PLAY_HISTORY_TRACKS_VIEW_ALL, null, null, null, null);
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

    public TrackItem getTrackItem() {
        return trackItem;
    }

    public List<StationRecord> getStations() {
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
    public Urn getUrn() {
        switch(type) {
            case TYPE_PLAYLIST_ITEM:
                return playlistItem.getUrn();
            case TYPE_PLAY_HISTORY_TRACKS_ITEM:
                return trackItem.getUrn();
            default:
                return Urn.NOT_SET;
        }
    }

    @Override
    public Optional<String> getImageUrlTemplate() {
        return type == TYPE_PLAYLIST_ITEM ? playlistItem.getImageUrlTemplate() : Optional.<String>absent();
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
                MoreObjects.equal(playlistItem, that.playlistItem) &&
                MoreObjects.equal(trackItem, that.trackItem);
    }

    @Override
    public final int hashCode() {
        return MoreObjects.hashCode(type, likes, playlistItem, trackItem);
    }
}
