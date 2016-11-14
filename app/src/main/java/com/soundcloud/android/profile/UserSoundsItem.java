package com.soundcloud.android.profile;

import static com.soundcloud.java.collections.Iterables.filter;
import static com.soundcloud.java.collections.Lists.newArrayList;

import com.google.auto.value.AutoValue;
import com.soundcloud.android.Consts;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playlists.PlaylistItem;
import com.soundcloud.android.presentation.ListItem;
import com.soundcloud.android.presentation.PlayableItem;
import com.soundcloud.android.tracks.TrackItem;
import com.soundcloud.java.collections.PropertySet;
import com.soundcloud.java.functions.Predicate;
import com.soundcloud.java.optional.Optional;

import java.util.List;

@AutoValue
abstract class UserSoundsItem implements ListItem {
    static final int TYPE_DIVIDER = 0;
    static final int TYPE_HEADER = 1;
    static final int TYPE_VIEW_ALL = 2;
    static final int TYPE_TRACK = 3;
    static final int TYPE_PLAYLIST = 4;
    static final int TYPE_END_OF_LIST_DIVIDER = 5;

    static UserSoundsItem fromPlaylistItem(PlaylistItem playlistItem, int collectionType) {
        return new AutoValue_UserSoundsItem(
                UserSoundsItem.TYPE_PLAYLIST,
                collectionType,
                Optional.<TrackItem>absent(),
                Optional.fromNullable(playlistItem));
    }

    static UserSoundsItem fromTrackItem(TrackItem trackItem, int collectionType) {
        return new AutoValue_UserSoundsItem(
                UserSoundsItem.TYPE_TRACK,
                collectionType,
                Optional.fromNullable(trackItem),
                Optional.<PlaylistItem>absent());
    }

    static UserSoundsItem fromViewAll(int collectionType) {
        return new AutoValue_UserSoundsItem(
                TYPE_VIEW_ALL,
                collectionType,
                Optional.<TrackItem>absent(),
                Optional.<PlaylistItem>absent());
    }

    static UserSoundsItem fromHeader(int collectionType) {
        return new AutoValue_UserSoundsItem(
                TYPE_HEADER,
                collectionType,
                Optional.<TrackItem>absent(),
                Optional.<PlaylistItem>absent());
    }

    static UserSoundsItem fromDivider() {
        return new AutoValue_UserSoundsItem(
                TYPE_DIVIDER,
                Consts.NOT_SET,
                Optional.<TrackItem>absent(),
                Optional.<PlaylistItem>absent());
    }

    static UserSoundsItem fromEndOfListDivider() {
        return new AutoValue_UserSoundsItem(
                TYPE_END_OF_LIST_DIVIDER,
                Consts.NOT_SET,
                Optional.<TrackItem>absent(),
                Optional.<PlaylistItem>absent());
    }

    public abstract int getItemType();

    public abstract int getCollectionType();

    public abstract Optional<TrackItem> getTrackItem();

    public abstract Optional<PlaylistItem> getPlaylistItem();

    public Optional<? extends PlayableItem> getPlayableItem() {
        if (isTrack()) return getTrackItem();
        else if (isPlaylist()) return getPlaylistItem();
        else return Optional.absent();
    }

    public boolean isTrack() {
        return getItemType() == TYPE_TRACK && getTrackItem().isPresent();
    }

    public boolean isPlaylist() {
        return getItemType() == TYPE_PLAYLIST && getPlaylistItem().isPresent();
    }

    public boolean isDivider() {
        return getItemType() == TYPE_DIVIDER || getItemType() == TYPE_END_OF_LIST_DIVIDER;
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

    public static int getPositionInModule(List<UserSoundsItem> userSoundsItems, UserSoundsItem clickedItem) {
        final List<UserSoundsItem> itemsInModule = filterItemsInModule(userSoundsItems, clickedItem);
        return itemsInModule.indexOf(clickedItem);
    }

    private static List<UserSoundsItem> filterItemsInModule(final List<UserSoundsItem> userSoundsItems,
                                                            final UserSoundsItem userSoundsItem) {
        return newArrayList(filter(userSoundsItems, new Predicate<UserSoundsItem>() {
            @Override
            public boolean apply(UserSoundsItem input) {
                return input.getItemType() != TYPE_DIVIDER
                        && input.getItemType() != TYPE_HEADER
                        && input.getItemType() != TYPE_VIEW_ALL
                        && input.getItemType() != TYPE_END_OF_LIST_DIVIDER
                        && input.getCollectionType() == userSoundsItem.getCollectionType();
            }
        }));
    }
}