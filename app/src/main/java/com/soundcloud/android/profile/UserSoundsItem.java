package com.soundcloud.android.profile;

import static com.soundcloud.java.collections.Iterables.filter;
import static com.soundcloud.java.collections.Lists.newArrayList;

import com.google.auto.value.AutoValue;
import com.soundcloud.android.Consts;
import com.soundcloud.android.events.LikesStatusEvent;
import com.soundcloud.android.events.RepostsStatusEvent;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.offline.OfflineState;
import com.soundcloud.android.playlists.Playlist;
import com.soundcloud.android.playlists.PlaylistItem;
import com.soundcloud.android.presentation.LikeableItem;
import com.soundcloud.android.presentation.ListItem;
import com.soundcloud.android.presentation.PlayableItem;
import com.soundcloud.android.presentation.RepostableItem;
import com.soundcloud.android.presentation.UpdatablePlaylistItem;
import com.soundcloud.android.presentation.UpdatableTrackItem;
import com.soundcloud.android.tracks.Track;
import com.soundcloud.android.tracks.TrackItem;
import com.soundcloud.java.optional.Optional;

import java.util.List;

@AutoValue
abstract class UserSoundsItem implements ListItem, UpdatableTrackItem, UpdatablePlaylistItem, LikeableItem, RepostableItem {
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

    private UserSoundsItem copyWithTrackItem(TrackItem trackItem) {
        return new AutoValue_UserSoundsItem(getItemType(), getCollectionType(), Optional.of(trackItem), getPlaylistItem());
    }

    private UserSoundsItem copyWithPlaylistItem(PlaylistItem playlistItem) {
        return new AutoValue_UserSoundsItem(getItemType(), getCollectionType(), getTrackItem(), Optional.of(playlistItem));
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
    public UserSoundsItem updatedWithTrackItem(Track trackItem) {
        if (isTrack()) {
            return copyWithTrackItem(TrackItem.from(trackItem));
        }
        return this;
    }

    public UserSoundsItem updatedWithOfflineState(OfflineState offlineState) {
        if (isTrack()) {
            return copyWithTrackItem(getTrackItem().get().updatedWithOfflineState(offlineState));
        } else if (isPlaylist()) {
            return copyWithPlaylistItem(getPlaylistItem().get().updatedWithOfflineState(offlineState));
        } else {
            return this;
        }
    }

    @Override
    public UserSoundsItem updatedWithLike(LikesStatusEvent.LikeStatus likeStatus) {
        if (isTrack()) {
            return copyWithTrackItem(getTrackItem().get().updatedWithLike(likeStatus));
        } else if (isPlaylist()) {
            return copyWithPlaylistItem(getPlaylistItem().get().updatedWithLike(likeStatus));
        } else {
            return this;
        }
    }

    @Override
    public UserSoundsItem updatedWithRepost(RepostsStatusEvent.RepostStatus repostStatus) {
        if (isTrack()) {
            return copyWithTrackItem(getTrackItem().get().updatedWithRepost(repostStatus));
        } else if (isPlaylist()) {
            return copyWithPlaylistItem(getPlaylistItem().get().updatedWithRepost(repostStatus));
        } else {
            return this;
        }
    }

    @Override
    public UpdatablePlaylistItem updatedWithTrackCount(int trackCount) {
        if (isPlaylist()) {
            return copyWithPlaylistItem(getPlaylistItem().get().updatedWithTrackCount(trackCount));
        }
        return this;
    }

    @Override
    public UpdatablePlaylistItem updatedWithMarkedForOffline(boolean markedForOffline) {
        if (isPlaylist()) {
            return copyWithPlaylistItem(getPlaylistItem().get().updatedWithMarkedForOffline(markedForOffline));
        }
        return this;
    }

    @Override
    public UpdatablePlaylistItem updatedWithPlaylist(Playlist playlist) {
        if (isPlaylist()) {
            return copyWithPlaylistItem(PlaylistItem.from(playlist));
        }
        return this;
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
        return newArrayList(filter(userSoundsItems, input -> input.getItemType() != TYPE_DIVIDER
                && input.getItemType() != TYPE_HEADER
                && input.getItemType() != TYPE_VIEW_ALL
                && input.getItemType() != TYPE_END_OF_LIST_DIVIDER
                && input.getCollectionType() == userSoundsItem.getCollectionType()));
    }
}
