package com.soundcloud.android.playlists;

import com.google.auto.value.AutoValue;
import com.soundcloud.android.R;
import com.soundcloud.android.api.model.ApiPlaylist;
import com.soundcloud.android.events.LikesStatusEvent;
import com.soundcloud.android.events.RepostsStatusEvent;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.offline.OfflineState;
import com.soundcloud.android.presentation.PlayableItem;
import com.soundcloud.android.presentation.UpdatablePlaylistItem;
import com.soundcloud.android.stream.PromotedProperties;
import com.soundcloud.android.stream.StreamEntity;
import com.soundcloud.annotations.VisibleForTesting;
import com.soundcloud.java.optional.Optional;
import com.soundcloud.java.strings.Strings;

import android.content.res.Resources;

import java.util.Collections;
import java.util.Date;
import java.util.List;

@AutoValue
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
        return builder(playlist).build();
    }

    public static PlaylistItem from(Playlist playlist, StreamEntity streamEntity) {
        final Builder builder = builder(playlist).reposter(streamEntity.reposter())
                                                 .reposterUrn(streamEntity.reposterUrn())
                                                 .avatarUrlTemplate(streamEntity.avatarUrl());
        if (streamEntity.promotedProperties().isPresent()) {
            builder.promotedProperties(streamEntity.promotedProperties());
        }
        return builder.build();
    }

    public static PlaylistItem from(ApiPlaylist apiPlaylist) {
        return fromLikedAndRepost(apiPlaylist, false, false);
    }

    public static PlaylistItem fromLiked(ApiPlaylist apiPlaylist, boolean isLiked) {
        return fromLikedAndRepost(apiPlaylist, isLiked, false);
    }

    private static PlaylistItem fromLikedAndRepost(ApiPlaylist apiPlaylist, boolean isLiked, boolean isRepost) {
        Urn creatorUrn = apiPlaylist.getUser() != null ? apiPlaylist.getUser().getUrn() : Urn.NOT_SET;
        boolean isPrivate = !apiPlaylist.isPublic();
        return builder().getImageUrlTemplate(apiPlaylist.getImageUrlTemplate())
                        .getCreatedAt(apiPlaylist.getCreatedAt())
                        .getUrn(apiPlaylist.getUrn())
                        .genre(Optional.fromNullable(apiPlaylist.getGenre()))
                        .title(apiPlaylist.getTitle())
                        .creatorUrn(creatorUrn)
                        .creatorName(apiPlaylist.getUsername())
                        .permalinkUrl(apiPlaylist.getPermalinkUrl())
                        .offlineState(OfflineState.NOT_OFFLINE)
                        .isUserLike(isLiked)
                        .likesCount(apiPlaylist.getStats().getLikesCount())
                        .isUserRepost(false)
                        .repostsCount(apiPlaylist.getStats().getRepostsCount())
                        .isPrivate(isPrivate)
                        .isRepost(isRepost)
                        .setType(Optional.fromNullable(apiPlaylist.getSetType()))
                        .isAlbum(apiPlaylist.isAlbum())
                        .trackCount(apiPlaylist.getTrackCount())
                        .isMarkedForOffline(Optional.absent())
                        .tags(Optional.fromNullable(apiPlaylist.getTags()))
                        .duration(apiPlaylist.getDuration())
                        .releaseDate(apiPlaylist.getReleaseDate())
                        .promotedProperties(Optional.absent()).build();
    }

    private static Builder builder(Playlist playlist) {
        return builder().getImageUrlTemplate(playlist.imageUrlTemplate())
                        .getCreatedAt(playlist.createdAt())
                        .getUrn(playlist.urn())
                        .genre(playlist.genre())
                        .title(playlist.title())
                        .creatorUrn(playlist.creatorUrn())
                        .creatorName(playlist.creatorName())
                        .permalinkUrl(playlist.permalinkUrl().or(Strings.EMPTY))
                        .offlineState(playlist.offlineState().or(OfflineState.NOT_OFFLINE))
                        .isUserLike(playlist.isLikedByCurrentUser().or(false))
                        .likesCount(playlist.likesCount())
                        .isUserRepost(playlist.isRepostedByCurrentUser().or(false))
                        .repostsCount(playlist.repostCount())
                        .isPrivate(playlist.isPrivate())
                        .isRepost(false)
                        .setType(playlist.setType())
                        .isAlbum(playlist.isAlbum())
                        .trackCount(playlist.trackCount())
                        .isMarkedForOffline(playlist.isMarkedForOffline())
                        .tags(playlist.tags())
                        .duration(playlist.duration())
                        .releaseDate(playlist.releaseDate().or(Strings.EMPTY))
                        .promotedProperties(Optional.absent());
    }

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
    public static Builder builder() {
        return new AutoValue_PlaylistItem.Builder().getImageUrlTemplate(Optional.absent())
                                                   .getCreatedAt(new Date())
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
                                                   .releaseDate(Strings.EMPTY)
                                                   .promotedProperties(Optional.absent());
    }

    public abstract PlaylistItem.Builder toBuilder();

    @Override
    public PlaylistItem updatedWithOfflineState(OfflineState offlineState) {
        return toBuilder().offlineState(offlineState).build();
    }

    public PlaylistItem updatedWithLike(LikesStatusEvent.LikeStatus likeStatus) {
        final Builder builder = toBuilder().isUserLike(likeStatus.isUserLike());
        if (likeStatus.likeCount().isPresent()) {
            builder.likesCount(likeStatus.likeCount().get());
        }
        return builder.build();
    }

    public PlaylistItem updateLikeState(boolean isLiked) {
        return toBuilder().isUserLike(isLiked).build();
    }


    public PlaylistItem updatedWithRepost(RepostsStatusEvent.RepostStatus repostStatus) {
        final Builder builder = toBuilder().isUserRepost(repostStatus.isReposted());
        if (repostStatus.repostCount().isPresent()) {
            builder.repostsCount(repostStatus.repostCount().get());
        }
        return builder.build();
    }

    @Override
    public PlaylistItem updatedWithTrackCount(int trackCount) {
        return toBuilder().trackCount(trackCount).build();
    }

    @Override
    public PlaylistItem updatedWithMarkedForOffline(boolean markedForOffline) {
        return toBuilder().isMarkedForOffline(Optional.of(markedForOffline)).build();
    }

    public PlaylistItem updatedWithLikeAndRepostStatus(boolean isLiked, boolean isReposted) {
        return toBuilder().isUserLike(isLiked).isUserRepost(isReposted).build();
    }

    @Override
    public PlaylistItem updateWithReposter(String reposter, Urn reposterUrn) {
        return toBuilder().reposter(Optional.of(reposter)).reposterUrn(Optional.of(reposterUrn)).build();
    }

    @AutoValue.Builder
    public abstract static class Builder {
        public abstract Builder getImageUrlTemplate(Optional<String> getImageUrlTemplate);

        public abstract Builder getCreatedAt(Date getCreatedAt);

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

        public Builder promotedProperties(PromotedProperties promotedProperties) {
            return promotedProperties(Optional.of(promotedProperties));
        }

        public abstract Builder promotedProperties(Optional<PromotedProperties> promotedProperties);

        public abstract PlaylistItem build();
    }
}
