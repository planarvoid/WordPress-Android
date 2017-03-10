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
        return builder(Playlist.from(apiPlaylist)).build();
    }

    public static Builder builder(Playlist playlist) {
        return new AutoValue_PlaylistItem.Builder().offlineState(playlist.offlineState().or(OfflineState.NOT_OFFLINE))
                                                   .isUserLike(playlist.isLikedByCurrentUser().or(false))
                                                   .likesCount(playlist.likesCount())
                                                   .isUserRepost(playlist.isRepostedByCurrentUser().or(false))
                                                   .repostsCount(playlist.repostCount())
                                                   .trackCount(playlist.trackCount())
                                                   .isMarkedForOffline(playlist.isMarkedForOffline())
                                                   .promotedProperties(Optional.absent())
                                                   .reposter(Optional.absent())
                                                   .reposterUrn(Optional.absent())
                                                   .avatarUrlTemplate(Optional.absent())
                                                   .playlist(playlist);
    }

    @Override
    public PlaylistItem updatedWithPlaylist(Playlist playlist) {
        return from(playlist);
    }

    public abstract Playlist playlist();

    public abstract int trackCount();

    public abstract Optional<Boolean> isMarkedForOffline();

    public Optional<String> getImageUrlTemplate() {
        return playlist().imageUrlTemplate();
    }

    public Date getCreatedAt() {
        return playlist().createdAt();
    }

    public Urn getUrn() {
        return playlist().urn();
    }

    public Optional<String> genre() {
        return playlist().genre();
    }

    public String title() {
        return playlist().title();
    }

    public Urn creatorUrn() {
        return playlist().creatorUrn();
    }

    public String creatorName() {
        return playlist().creatorName();
    }

    public String permalinkUrl() {
        return playlist().permalinkUrl().or(Strings.EMPTY);
    }

    public boolean isPrivate() {
        return playlist().isPrivate();
    }

    public Optional<String> setType() {
        return playlist().setType();
    }

    public boolean isAlbum() {
        return playlist().isAlbum();
    }

    public Optional<List<String>> tags() {
        return playlist().tags();
    }

    public String releaseDate() {
        return playlist().releaseDate().or(Strings.EMPTY);
    }

    @Override
    public String getPlayableType() {
        if (isAlbum()) {
            return setType().or(TYPE_ALBUM);
        } else {
            return TYPE_PLAYLIST;
        }
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
        return playlist().duration();
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
        public abstract Builder offlineState(OfflineState offlineState);

        public abstract Builder isUserLike(boolean isUserLike);

        public abstract Builder likesCount(int likesCount);

        public abstract Builder isUserRepost(boolean isUserRepost);

        public abstract Builder repostsCount(int repostsCount);

        public abstract Builder reposter(Optional<String> reposter);

        public abstract Builder reposterUrn(Optional<Urn> reposterUrn);

        public abstract Builder avatarUrlTemplate(Optional<String> avatarUrlTemplate);

        public abstract Builder trackCount(int trackCount);

        public abstract Builder isMarkedForOffline(Optional<Boolean> isMarkedForOffline);

        public abstract Builder playlist(Playlist playlist);

        public Builder promotedProperties(PromotedProperties promotedProperties) {
            return promotedProperties(Optional.of(promotedProperties));
        }

        public abstract Builder promotedProperties(Optional<PromotedProperties> promotedProperties);

        public abstract PlaylistItem build();
    }
}
