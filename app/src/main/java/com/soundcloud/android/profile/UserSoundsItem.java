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
        return builder()
                    .itemType(UserSoundsItem.TYPE_PLAYLIST)
                    .collectionType(collectionType)
                    .playlistItem(Optional.fromNullable(playlistItem))
                    .trackItem(Optional.absent())
                .build();
    }

    static UserSoundsItem fromTrackItem(TrackItem trackItem, int collectionType) {
        return builder()
                    .itemType(UserSoundsItem.TYPE_TRACK)
                    .collectionType(collectionType)
                    .playlistItem(Optional.absent())
                    .trackItem(Optional.fromNullable(trackItem))
                .build();
    }

    private UserSoundsItem copyWithTrackItem(TrackItem trackItem) {
        return fromTrackItem(trackItem, collectionType());
    }

    private UserSoundsItem copyWithPlaylistItem(PlaylistItem playlistItem) {
        return fromPlaylistItem(playlistItem, collectionType());
    }

    static UserSoundsItem fromViewAll(int collectionType) {
        return builder()
                    .itemType(UserSoundsItem.TYPE_VIEW_ALL)
                    .collectionType(collectionType)
                    .playlistItem(Optional.absent())
                    .trackItem(Optional.absent())
                .build();
    }

    static UserSoundsItem fromHeader(int collectionType) {
        return builder()
                .itemType(UserSoundsItem.TYPE_HEADER)
                .collectionType(collectionType)
                .playlistItem(Optional.absent())
                .trackItem(Optional.absent())
                .build();
    }

    static UserSoundsItem fromDivider() {
        return builder()
                .itemType(UserSoundsItem.TYPE_DIVIDER)
                .collectionType(Consts.NOT_SET)
                .playlistItem(Optional.absent())
                .trackItem(Optional.absent())
                .build();
    }

    static UserSoundsItem fromEndOfListDivider() {
        return builder()
                .itemType(UserSoundsItem.TYPE_END_OF_LIST_DIVIDER)
                .collectionType(Consts.NOT_SET)
                .playlistItem(Optional.absent())
                .trackItem(Optional.absent())
                .build();
    }

    public static UserSoundsItem.Builder builder() {
        return new AutoValue_UserSoundsItem.Builder();
    }

    public abstract int itemType();

    public abstract int collectionType();

    public abstract Optional<TrackItem> trackItem();

    public abstract Optional<PlaylistItem> playlistItem();

    public abstract Builder toBuilder();

    public Optional<? extends PlayableItem> getPlayableItem() {
        if (isTrack()) return trackItem();
        else if (isPlaylist()) return playlistItem();
        else return Optional.absent();
    }

    public boolean isTrack() {
        return itemType() == TYPE_TRACK && trackItem().isPresent();
    }

    public boolean isPlaylist() {
        return itemType() == TYPE_PLAYLIST && playlistItem().isPresent();
    }

    public boolean isDivider() {
        return itemType() == TYPE_DIVIDER || itemType() == TYPE_END_OF_LIST_DIVIDER;
    }

    @Override
    public UpdatableTrackItem updatedWithTrack(Track track) {
        if (isTrack()) {
            return toBuilder().trackItem(trackItem().get().updatedWithTrack(track)).build();
        }
        return this;
    }

    public UserSoundsItem updatedWithOfflineState(OfflineState offlineState) {
        if (isTrack()) {
            return copyWithTrackItem(trackItem().get().updatedWithOfflineState(offlineState));
        } else if (isPlaylist()) {
            return copyWithPlaylistItem(playlistItem().get().updatedWithOfflineState(offlineState));
        } else {
            return this;
        }
    }

    @Override
    public UserSoundsItem updatedWithLike(LikesStatusEvent.LikeStatus likeStatus) {
        if (isTrack()) {
            return copyWithTrackItem(trackItem().get().updatedWithLike(likeStatus));
        } else if (isPlaylist()) {
            return copyWithPlaylistItem(playlistItem().get().updatedWithLike(likeStatus));
        } else {
            return this;
        }
    }

    @Override
    public UserSoundsItem updatedWithRepost(RepostsStatusEvent.RepostStatus repostStatus) {
        if (isTrack()) {
            return copyWithTrackItem(trackItem().get().updatedWithRepost(repostStatus));
        } else if (isPlaylist()) {
            return copyWithPlaylistItem(playlistItem().get().updatedWithRepost(repostStatus));
        } else {
            return this;
        }
    }

    @Override
    public UpdatablePlaylistItem updatedWithTrackCount(int trackCount) {
        if (isPlaylist()) {
            return copyWithPlaylistItem(playlistItem().get().updatedWithTrackCount(trackCount));
        }
        return this;
    }

    @Override
    public UpdatablePlaylistItem updatedWithMarkedForOffline(boolean markedForOffline) {
        if (isPlaylist()) {
            return copyWithPlaylistItem(playlistItem().get().updatedWithMarkedForOffline(markedForOffline));
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
            return trackItem().get().getUrn();
        } else if (isPlaylist()) {
            return playlistItem().get().getUrn();
        } else {
            return Urn.NOT_SET;
        }
    }

    @Override
    public Optional<String> getImageUrlTemplate() {
        if (isTrack()) {
            return trackItem().get().getImageUrlTemplate();
        } else if (isPlaylist()) {
            return playlistItem().get().getImageUrlTemplate();
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
        return newArrayList(filter(userSoundsItems, input -> input.itemType() != TYPE_DIVIDER
                && input.itemType() != TYPE_HEADER
                && input.itemType() != TYPE_VIEW_ALL
                && input.itemType() != TYPE_END_OF_LIST_DIVIDER
                && input.collectionType() == userSoundsItem.collectionType()));
    }

    @AutoValue.Builder
    public abstract static class Builder {
        public abstract Builder itemType(int value);

        public abstract Builder collectionType(int value);

        public Builder trackItem(TrackItem trackItem) {
            return trackItem(Optional.of(trackItem));
        }

        public abstract Builder trackItem(Optional<TrackItem> value);

        public abstract Builder playlistItem(Optional<PlaylistItem> value);

        public abstract UserSoundsItem build();
    }
}
