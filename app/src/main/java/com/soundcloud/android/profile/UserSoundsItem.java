package com.soundcloud.android.profile;

import com.google.auto.value.AutoValue;
import com.soundcloud.android.Consts;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playlists.PlaylistItem;
import com.soundcloud.android.presentation.ListItem;
import com.soundcloud.android.tracks.TrackItem;
import com.soundcloud.java.collections.PropertySet;
import com.soundcloud.java.optional.Optional;

@AutoValue
public abstract class UserSoundsItem implements ListItem {
    static final int TYPE_DIVIDER = 0;
    static final int TYPE_HEADER = 1;
    static final int TYPE_VIEW_ALL = 2;
    static final int TYPE_TRACK = 3;
    static final int TYPE_PLAYLIST = 4;

    public static UserSoundsItem fromPlaylistItem(PlaylistItem playlistItem, int collectionType) {
        return new AutoValue_UserSoundsItem(
                UserSoundsItem.TYPE_PLAYLIST,
                collectionType,
                Optional.<TrackItem>absent(),
                Optional.fromNullable(playlistItem));
    }

    public static UserSoundsItem fromTrackItem(TrackItem trackItem, int collectionType) {
        return new AutoValue_UserSoundsItem(
                UserSoundsItem.TYPE_TRACK,
                collectionType,
                Optional.fromNullable(trackItem),
                Optional.<PlaylistItem>absent());
    }

    public static UserSoundsItem fromViewAll(int collectionType) {
        return new AutoValue_UserSoundsItem(
                TYPE_VIEW_ALL,
                collectionType,
                Optional.<TrackItem>absent(),
                Optional.<PlaylistItem>absent());
    }

    public static UserSoundsItem fromHeader(int collectionType) {
        return new AutoValue_UserSoundsItem(
                TYPE_HEADER,
                collectionType,
                Optional.<TrackItem>absent(),
                Optional.<PlaylistItem>absent());
    }

    public static UserSoundsItem fromDivider() {
        return new AutoValue_UserSoundsItem(
                TYPE_DIVIDER,
                Consts.NOT_SET,
                Optional.<TrackItem>absent(),
                Optional.<PlaylistItem>absent());
    }

    public abstract int getItemType();

    public abstract int getCollectionType();

    public abstract Optional<TrackItem> getTrackItem();

    public abstract Optional<PlaylistItem> getPlaylistItem();

    public boolean isTrack() {
        return getItemType() == TYPE_TRACK && getTrackItem().isPresent();
    }

    public boolean isPlaylist() {
        return getItemType() == TYPE_PLAYLIST && getPlaylistItem().isPresent();
    }

    @Override
    public ListItem update(PropertySet sourceSet) {
        if (getItemType() == TYPE_TRACK && getTrackItem().isPresent()) {
            return getTrackItem().get().update(sourceSet);
        }
        if (getItemType() == TYPE_PLAYLIST && getPlaylistItem().isPresent()) {
            return getPlaylistItem().get().update(sourceSet);
        }
        return this;
    }

    @Override
    public Urn getEntityUrn() {
        if (getItemType() == TYPE_TRACK && getTrackItem().isPresent()) {
            return getTrackItem().get().getEntityUrn();
        }
        if (getItemType() == TYPE_PLAYLIST && getPlaylistItem().isPresent()) {
            return getPlaylistItem().get().getEntityUrn();
        }
        return Urn.NOT_SET;
    }
}
