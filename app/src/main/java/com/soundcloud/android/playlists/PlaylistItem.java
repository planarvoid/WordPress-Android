package com.soundcloud.android.playlists;

import auto.parcel.AutoParcel;
import com.soundcloud.android.R;
import com.soundcloud.android.api.model.ApiPlaylist;
import com.soundcloud.android.events.LikesStatusEvent;
import com.soundcloud.android.events.RepostsStatusEvent;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.offline.OfflineState;
import com.soundcloud.android.presentation.PlayableItem;
import com.soundcloud.android.presentation.UpdatablePlaylistItem;
import com.soundcloud.android.stream.StreamEntity;
import com.soundcloud.annotations.VisibleForTesting;
import com.soundcloud.java.optional.Optional;
import com.soundcloud.java.strings.Strings;

import android.content.res.Resources;

import java.util.Collections;
import java.util.Date;
import java.util.List;

public abstract class PlaylistItem extends PlayableItem implements UpdatablePlaylistItem {

    public static final String TYPE_PLAYLIST = "playlist";
    public static final String TYPE_ALBUM = "album";
    public static final String TYPE_EP = "ep";
    public static final String TYPE_SINGLE = "single";
    public static final String TYPE_COMPILATION = "compilation";

    @VisibleForTesting
    public static int getSetTypeTitle(String playableType) {
        switch (playableType) {
            case PlaylistItem.TYPE_PLAYLIST:
                return R.string.set_type_default_label;
            case PlaylistItem.TYPE_ALBUM:
                return R.string.set_type_album_label;
            case PlaylistItem.TYPE_EP:
                return R.string.set_type_ep_label;
            case PlaylistItem.TYPE_SINGLE:
                return R.string.set_type_single_label;
            case PlaylistItem.TYPE_COMPILATION:
                return R.string.set_type_compilation_label;
            default:
                return R.string.set_type_default_label;
        }
    }

    public static int getSetTypeLabel(String playableType) {
        switch (playableType) {
            case PlaylistItem.TYPE_PLAYLIST:
                return R.string.set_type_default_label_for_text;
            case PlaylistItem.TYPE_ALBUM:
                return R.string.set_type_album_label_for_text;
            case PlaylistItem.TYPE_EP:
                return R.string.set_type_ep_label_for_text;
            case PlaylistItem.TYPE_SINGLE:
                return R.string.set_type_single_label_for_text;
            case PlaylistItem.TYPE_COMPILATION:
                return R.string.set_type_compilation_label_for_text;
            default:
                return R.string.set_type_default_label_for_text;
        }
    }

    public static PlaylistItem from(ApiPlaylist apiPlaylist, boolean repost) {
        return fromLikedAndRepost(apiPlaylist, false, repost);
    }

    public static PlaylistItem from(Playlist playlist) {
        return PlaylistItem.create(playlist.urn(),
                                   playlist.offlineState().or(OfflineState.NOT_OFFLINE),
                                   playlist.isLikedByCurrentUser().or(false),
                                   playlist.likesCount(),
                                   playlist.isRepostedByCurrentUser().or(false),
                                   playlist.repostCount(),
                                   playlist.genre(),
                                   playlist.title(),
                                   playlist.creatorUrn(),
                                   playlist.creatorName(),
                                   playlist.isPrivate(),
                                   false,
                                   playlist.createdAt(),
                                   playlist.imageUrlTemplate(),
                                   playlist.permalinkUrl().or(Strings.EMPTY),
                                   playlist.duration(),
                                   playlist.trackCount(),
                                   playlist.tags(),
                                   playlist.isAlbum(),
                                   playlist.setType(),
                                   playlist.releaseDate().or(Strings.EMPTY),
                                   playlist.isMarkedForOffline()).build();
    }

    public static PlaylistItem fromPlaylistAndStreamEntity(Playlist playlist, StreamEntity streamEntity) {
        return PlaylistItem.create(playlist.urn(),
                                   playlist.offlineState().or(OfflineState.NOT_OFFLINE),
                                   playlist.isLikedByCurrentUser().or(false),
                                   playlist.likesCount(),
                                   playlist.isRepostedByCurrentUser().or(false),
                                   playlist.repostCount(),
                                   playlist.genre(),
                                   playlist.title(),
                                   playlist.creatorUrn(),
                                   playlist.creatorName(),
                                   playlist.isPrivate(),
                                   false,
                                   playlist.createdAt(),
                                   playlist.imageUrlTemplate(),
                                   playlist.permalinkUrl().or(Strings.EMPTY),
                                   playlist.duration(),
                                   playlist.trackCount(),
                                   playlist.tags(),
                                   playlist.isAlbum(),
                                   playlist.setType(),
                                   playlist.releaseDate().or(Strings.EMPTY),
                                   playlist.isMarkedForOffline())
                           .reposter(streamEntity.reposter())
                           .reposterUrn(streamEntity.reposterUrn())
                           .avatarUrlTemplate(streamEntity.avatarUrl())
                           .build();
    }

    public static PlaylistItem from(ApiPlaylist apiPlaylist) {
        return fromLikedAndRepost(apiPlaylist, false, false);
    }

    public static PlaylistItem fromLiked(ApiPlaylist apiPlaylist, boolean isLiked) {
        return fromLikedAndRepost(apiPlaylist, isLiked, false);
    }

    public static PlaylistItem fromLikedAndRepost(ApiPlaylist apiPlaylist, boolean isLiked, boolean isRepost) {
        return PlaylistItem.create(
                apiPlaylist.getUrn(),
                OfflineState.NOT_OFFLINE,
                isLiked,
                apiPlaylist.getStats().getLikesCount(),
                false,
                apiPlaylist.getStats().getRepostsCount(),
                Optional.fromNullable(apiPlaylist.getGenre()),
                apiPlaylist.getTitle(),
                apiPlaylist.getUser() != null ? apiPlaylist.getUser().getUrn() : Urn.NOT_SET,
                apiPlaylist.getUsername(),
                !apiPlaylist.isPublic(),
                isRepost,
                apiPlaylist.getCreatedAt(),
                apiPlaylist.getImageUrlTemplate(),
                apiPlaylist.getPermalinkUrl(),
                apiPlaylist.getDuration(),
                apiPlaylist.getTrackCount(),
                Optional.fromNullable(apiPlaylist.getTags()),
                apiPlaylist.isAlbum(),
                Optional.fromNullable(apiPlaylist.getSetType()),
                apiPlaylist.getReleaseDate(),
                Optional.absent()
        ).build();
    }

    public abstract PlaylistItem updatedWithOfflineState(OfflineState offlineState);

    public abstract PlaylistItem updatedWithLike(LikesStatusEvent.LikeStatus likeStatus);

    public abstract PlaylistItem updateLikeState(boolean isLiked);

    public abstract PlaylistItem updatedWithRepost(RepostsStatusEvent.RepostStatus repostStatus);

    public abstract PlaylistItem updatedWithTrackCount(int trackCount);

    public abstract PlaylistItem updatedWithMarkedForOffline(boolean markedForOffline);

    public abstract PlaylistItem updatedWithLikeAndRepostStatus(boolean isLiked, boolean isReposted);

    public abstract PlaylistItem updateWithReposter(String reposter, Urn reposterUrn);

    @Override
    public PlaylistItem updatedWithPlaylist(Playlist playlist) {
        return from(playlist);
    }


    public abstract Optional<String> setType();

    public abstract boolean isAlbum();

    public abstract int trackCount();

    public abstract Optional<Boolean> isMarkedForOffline();

    public abstract Optional<List<String>> tags();

    public abstract long duration();

    public abstract String releaseDate();

    protected static PlaylistItem.Default.Builder create(Urn urn,
                                                         OfflineState offlineState,
                                                         boolean isUserLike,
                                                         int likesCount,
                                                         boolean isUserRepost,
                                                         int repostsCount,
                                                         Optional<String> genre,
                                                         String title,
                                                         Urn creatorUrn,
                                                         String creatorName,
                                                         boolean isPrivate,
                                                         boolean isRepost,
                                                         Date createdAt,
                                                         Optional<String> imageUrlTemplate,
                                                         String permalinkUrl,
                                                         long duration,
                                                         int trackCount,
                                                         Optional<List<String>> tags,
                                                         boolean isAlbum,
                                                         Optional<String> setType,
                                                         String releaseDate,
                                                         Optional<Boolean> isMarkedForOffline) {
        return builder().getImageUrlTemplate(imageUrlTemplate)
                        .getCreatedAt(createdAt)
                        .getUrn(urn)
                        .genre(genre)
                        .title(title)
                        .creatorUrn(creatorUrn)
                        .creatorName(creatorName)
                        .permalinkUrl(permalinkUrl)
                        .offlineState(offlineState)
                        .isUserLike(isUserLike)
                        .likesCount(likesCount)
                        .isUserRepost(isUserRepost)
                        .repostsCount(repostsCount)
                        .isPrivate(isPrivate)
                        .isRepost(isRepost)
                        .setType(setType)
                        .isAlbum(isAlbum)
                        .trackCount(trackCount)
                        .isMarkedForOffline(isMarkedForOffline)
                        .tags(tags)
                        .duration(duration)
                        .releaseDate(releaseDate);
    }

    @Override
    public String getPlayableType() {
        if (isAlbum()) {
            return setType().or(TYPE_ALBUM);
        } else {
            return TYPE_PLAYLIST;
        }
    }

    public Optional<String> getSetType() {
        return setType();
    }

    public int getTrackCount() {
        return trackCount();
    }

    public OfflineState getDownloadState() {
        return offlineState();
    }

    public List<String> getTags() {
        final Optional<List<String>> optionalTags = tags();
        return optionalTags.isPresent() ? optionalTags.get() : Collections.emptyList();
    }

    @Override
    public long getDuration() {
        return duration();
    }

    public boolean isPublic() {
        return !isPrivate();
    }

    public String getReleaseDate() {
        return releaseDate();
    }

    public String getLabel(Resources resources) {
        return PlaylistUtils.formatPlaylistTitle(resources, getPlayableType(), isAlbum(), getReleaseDate());
    }

    @VisibleForTesting
    public static Default.Builder builder() {
        return new AutoParcel_PlaylistItem_Default.Builder().getImageUrlTemplate(Optional.absent())
                                                            .getCreatedAt(new Date())
                                                            .getKind(Kind.PLAYABLE)
                                                            .getUrn(Urn.NOT_SET)
                                                            .genre(Optional.absent())
                                                            .title(Strings.EMPTY)
                                                            .creatorUrn(Urn.NOT_SET)
                                                            .creatorName(Strings.EMPTY)
                                                            .permalinkUrl(Strings.EMPTY)
                                                            .offlineState(OfflineState.NOT_OFFLINE)
                                                            .isUserLike(false)
                                                            .likesCount(0)
                                                            .isUserRepost(false)
                                                            .repostsCount(0)
                                                            .reposter(Optional.absent())
                                                            .reposterUrn(Optional.absent())
                                                            .isPrivate(false)
                                                            .isRepost(false)
                                                            .avatarUrlTemplate(Optional.absent())
                                                            .setType(Optional.absent())
                                                            .isAlbum(false)
                                                            .trackCount(0)
                                                            .isMarkedForOffline(Optional.absent())
                                                            .tags(Optional.absent())
                                                            .duration(0)
                                                            .releaseDate(Strings.EMPTY);
    }

    @VisibleForTesting
    public static Default.Builder builder(PlaylistItem playlistItem) {
        if (playlistItem instanceof Default) {
            return new AutoParcel_PlaylistItem_Default.Builder((Default) playlistItem);
        }
        throw new IllegalArgumentException("Trying to create builder from promoted track item.");
    }

    @AutoParcel
    public static abstract class Default extends PlaylistItem {

        @Override
        public PlaylistItem updatedWithOfflineState(OfflineState offlineState) {
            return new AutoParcel_PlaylistItem_Default.Builder(this).offlineState(offlineState).build();
        }

        public PlaylistItem updatedWithLike(LikesStatusEvent.LikeStatus likeStatus) {
            final Default.Builder builder = new AutoParcel_PlaylistItem_Default.Builder(this).isUserLike(likeStatus.isUserLike());
            if (likeStatus.likeCount().isPresent()) {
                builder.likesCount(likeStatus.likeCount().get());
            }
            return builder.build();
        }

        public PlaylistItem updateLikeState(boolean isLiked) {
            return new AutoParcel_PlaylistItem_Default.Builder(this).isUserLike(isLiked).build();
        }


        public PlaylistItem updatedWithRepost(RepostsStatusEvent.RepostStatus repostStatus) {
            final Default.Builder builder = new AutoParcel_PlaylistItem_Default.Builder(this).isUserRepost(repostStatus.isReposted());
            if (repostStatus.repostCount().isPresent()) {
                builder.repostsCount(repostStatus.repostCount().get());
            }
            return builder.build();
        }

        @Override
        public PlaylistItem updatedWithTrackCount(int trackCount) {
            return new AutoParcel_PlaylistItem_Default.Builder(this).trackCount(trackCount).build();
        }

        @Override
        public PlaylistItem updatedWithMarkedForOffline(boolean markedForOffline) {
            return new AutoParcel_PlaylistItem_Default.Builder(this).isMarkedForOffline(Optional.of(markedForOffline)).build();
        }

        public PlaylistItem updatedWithLikeAndRepostStatus(boolean isLiked, boolean isReposted) {
            return new AutoParcel_PlaylistItem_Default.Builder(this).isUserLike(isLiked).isUserRepost(isReposted).build();
        }

        @Override
        public PlaylistItem updateWithReposter(String reposter, Urn reposterUrn) {
            return new AutoParcel_PlaylistItem_Default.Builder(this).reposter(Optional.of(reposter)).reposterUrn(Optional.of(reposterUrn)).build();
        }

        @AutoParcel.Builder
        public abstract static class Builder {
            public abstract Builder getImageUrlTemplate(Optional<String> getImageUrlTemplate);

            public abstract Builder getCreatedAt(Date getCreatedAt);

            public abstract Builder getKind(Kind getKind);

            public abstract Builder getUrn(Urn getUrn);

            public abstract Builder genre(Optional<String> genre);

            public abstract Builder title(String title);

            public abstract Builder creatorUrn(Urn creatorUrn);

            public abstract Builder creatorName(String creatorName);

            public abstract Builder permalinkUrl(String permalinkUrl);

            public abstract Builder offlineState(OfflineState offlineState);

            public abstract Builder isUserLike(boolean isUserLike);

            public abstract Builder likesCount(int likesCount);

            public abstract Builder isUserRepost(boolean isUserRepost);

            public abstract Builder repostsCount(int repostsCount);

            public abstract Builder reposter(Optional<String> reposter);

            public abstract Builder reposterUrn(Optional<Urn> reposterUrn);

            public abstract Builder isPrivate(boolean isPrivate);

            public abstract Builder isRepost(boolean isRepost);

            public abstract Builder avatarUrlTemplate(Optional<String> avatarUrlTemplate);

            public abstract Builder setType(Optional<String> setType);

            public abstract Builder isAlbum(boolean isAlbum);

            public abstract Builder trackCount(int trackCount);

            public abstract Builder isMarkedForOffline(Optional<Boolean> isMarkedForOffline);

            public abstract Builder tags(Optional<List<String>> tags);

            public abstract Builder duration(long duration);

            public abstract Builder releaseDate(String releaseDate);

            public abstract Default build();
        }
    }
}
