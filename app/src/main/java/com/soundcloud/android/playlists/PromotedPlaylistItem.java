package com.soundcloud.android.playlists;

import com.google.auto.value.AutoValue;
import com.soundcloud.android.events.LikesStatusEvent;
import com.soundcloud.android.events.RepostsStatusEvent;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.offline.OfflineState;
import com.soundcloud.android.stream.PromotedProperties;
import com.soundcloud.android.stream.StreamEntity;
import com.soundcloud.annotations.VisibleForTesting;
import com.soundcloud.java.optional.Optional;
import com.soundcloud.java.strings.Strings;

import java.util.Date;
import java.util.List;

@AutoValue
public abstract class PromotedPlaylistItem extends PlaylistItem {

    @Override
    public abstract Optional<PromotedProperties> promotedProperties();

    public static PromotedPlaylistItem from(Playlist playlist, StreamEntity streamEntity, PromotedProperties promotedStreamProperties) {
        return builder(promotedStreamProperties).getImageUrlTemplate(playlist.imageUrlTemplate())
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
                                                .reposter(streamEntity.reposter())
                                                .reposterUrn(streamEntity.reposterUrn())
                                                .avatarUrlTemplate(streamEntity.avatarUrl())
                                                .build();
    }

    public PlaylistItem updatedWithLike(LikesStatusEvent.LikeStatus likeStatus) {
        final Builder builder = builder(this).isUserLike(likeStatus.isUserLike());
        if (likeStatus.likeCount().isPresent()) {
            builder.likesCount(likeStatus.likeCount().get());
        }
        return builder.build();
    }

    public PlaylistItem updatedWithOfflineState(OfflineState offlineState) {
        return builder(this).offlineState(offlineState).build();
    }


    public PlaylistItem updateLikeState(boolean isLiked) {
        return builder(this).isUserLike(isLiked).build();
    }


    public PlaylistItem updatedWithRepost(RepostsStatusEvent.RepostStatus repostStatus) {
        final Builder builder = builder(this).isUserRepost(repostStatus.isReposted());
        if (repostStatus.repostCount().isPresent()) {
            builder.repostsCount(repostStatus.repostCount().get());
        }
        return builder.build();
    }

    public PlaylistItem updatedWithTrackCount(int trackCount) {
        return builder(this).trackCount(trackCount).build();
    }

    public PlaylistItem updatedWithMarkedForOffline(boolean markedForOffline) {
        return builder(this).isMarkedForOffline(Optional.of(markedForOffline)).build();
    }

    public PlaylistItem updatedWithLikeAndRepostStatus(boolean isLiked, boolean isReposted) {
        return builder(this).isUserLike(isLiked).isUserRepost(isReposted).build();
    }


    public PlaylistItem updateWithReposter(String reposter, Urn reposterUrn) {
        return builder(this).reposter(Optional.of(reposter)).reposterUrn(Optional.of(reposterUrn)).build();
    }
    @Override
    public String getAdUrn() {
        return promotedProperties().get().adUrn();
    }

    @Override
    public boolean hasPromoter() {
        return isPromoted() && promotedProperties().get().promoterUrn().isPresent();
    }

    @Override
    public Optional<String> promoterName() {
        return promotedProperties().get().promoterName();
    }

    @Override
    public Optional<Urn> promoterUrn() {
        return promotedProperties().get().promoterUrn();
    }

    @Override
    public List<String> clickUrls() {
        return promotedProperties().get().trackClickedUrls();
    }

    @Override
    public List<String> impressionUrls() {
        return promotedProperties().get().trackImpressionUrls();
    }

    @Override
    public List<String> promoterClickUrls() {
        return promotedProperties().get().promoterClickedUrls();
    }

    @Override
    public List<String> playUrls() {
        return promotedProperties().get().trackPlayedUrls();
    }

    @Override
    public Optional<String> getAvatarUrlTemplate() {
        return avatarUrlTemplate();
    }

    @VisibleForTesting
    public static Builder builder(PromotedProperties promotedProperties) {
        return new AutoValue_PromotedPlaylistItem.Builder().getImageUrlTemplate(Optional.absent())
                                                            .getCreatedAt(new Date())
                                                            .getKind(Kind.PROMOTED)
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
                                                            .promotedProperties(promotedProperties);
    }

    public boolean isPromoted() {
        return promotedProperties().isPresent();
    }

    private static Builder builder(PromotedPlaylistItem promotedPlaylistItem) {
        return new AutoValue_PromotedPlaylistItem.Builder(promotedPlaylistItem);
    }

    @AutoValue.Builder
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

        public Builder promotedProperties(PromotedProperties promotedProperties) {
            return promotedProperties(Optional.of(promotedProperties));
        }

        public abstract Builder promotedProperties(Optional<PromotedProperties> promotedProperties);

        public abstract PromotedPlaylistItem build();
    }
}
