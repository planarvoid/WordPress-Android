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
        if (isTrack()) {
            return getTrackItem().get().update(sourceSet);
        } else if (isPlaylist()) {
            return getPlaylistItem().get().update(sourceSet);
        } else {
            return this;
        }
    }

    @Override
    public Urn getUrn() {
        if (isTrack()) {
            return getTrackItem().get().getUrn();
        } else if (isPlaylist()) {
            return getPlaylistItem().get().getUrn();
        } else {
            return Urn.NOT_SET;
        }
    }

    @Override
    public Optional<String> getImageUrlTemplate() {
        if (isTrack()) {
            return getTrackItem().get().getImageUrlTemplate();
        } else if (isPlaylist()) {
            return getPlaylistItem().get().getImageUrlTemplate();
        } else {
            return Optional.absent();
        }
    }
}
