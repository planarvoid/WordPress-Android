package com.soundcloud.android.tracks;

import com.google.auto.value.AutoValue;
import com.soundcloud.android.events.LikesStatusEvent;
import com.soundcloud.android.events.RepostsStatusEvent;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.offline.OfflineState;
import com.soundcloud.android.presentation.PromotedListItem;
import com.soundcloud.android.stream.PromotedProperties;
import com.soundcloud.android.stream.StreamEntity;
import com.soundcloud.java.optional.Optional;
import com.soundcloud.java.strings.Strings;

import java.util.Date;
import java.util.List;

@AutoValue
public abstract class PromotedTrackItem extends TrackItem implements PromotedListItem {

    public abstract PromotedProperties promotedProperties();

    @Override
    public String getAdUrn() {
        return promotedProperties().adUrn();
    }

    @Override
    public boolean hasPromoter() {
        return promotedProperties().promoterUrn().isPresent();
    }

    @Override
    public Optional<String> getPromoterName() {
        return promotedProperties().promoterName();
    }

    @Override
    public Optional<Urn> getPromoterUrn() {
        return promotedProperties().promoterUrn();
    }

    @Override
    public List<String> getClickUrls() {
        return promotedProperties().trackClickedUrls();
    }

    @Override
    public List<String> getImpressionUrls() {
        return promotedProperties().trackImpressionUrls();
    }

    @Override
    public List<String> getPromoterClickUrls() {
        return promotedProperties().promoterClickedUrls();
    }

    @Override
    public List<String> getPlayUrls() {
        return promotedProperties().trackPlayedUrls();
    }

    @Override
    public Optional<String> getAvatarUrlTemplate() {
        return avatarUrlTemplate();
    }

    @Override
    public TrackItem updatedWithTrackItem(Track track) {
        return TrackItem.from(track);
    }

    @Override
    public TrackItem updatedWithOfflineState(OfflineState offlineState) {
        return builder(this).offlineState(offlineState).build();
    }

    public TrackItem updatedWithLike(LikesStatusEvent.LikeStatus likeStatus) {
        final Builder builder = builder(this).isUserLike(likeStatus.isUserLike());
        if (likeStatus.likeCount().isPresent()) {
            builder.likesCount(likeStatus.likeCount().get());
        }
        return builder.build();
    }

    public TrackItem updatedWithRepost(RepostsStatusEvent.RepostStatus repostStatus) {
        final Builder builder = builder(this).isUserRepost(repostStatus.isReposted());
        if (repostStatus.repostCount().isPresent()) {
            builder.repostsCount(repostStatus.repostCount().get());
        }
        return builder.build();
    }

    public TrackItem updatedWithLikeAndRepostStatus(boolean isLiked, boolean isReposted) {
        return builder(this).isUserLike(isLiked).isUserRepost(isReposted).build();
    }

    @Override
    public TrackItem updateLikeState(boolean isLiked) {
        return builder(this).isUserLike(isLiked).build();
    }

    @Override
    public TrackItem updateWithReposter(String reposter, Urn reposterUrn) {
        return builder(this).reposter(Optional.of(reposter)).reposterUrn(Optional.of(reposterUrn)).build();
    }

    public static PromotedTrackItem from(Track track, StreamEntity streamEntity, PromotedProperties promotedProperties) {
        return PromotedTrackItem.builder(promotedProperties)
                                .getUrn(track.urn())
                                .offlineState(track.offlineState())
                                .isUserLike(track.userLike())
                                .likesCount(track.likesCount())
                                .isUserRepost(track.userRepost())
                                .repostsCount(track.repostsCount())
                                .genre(track.genre())
                                .title(track.title())
                                .creatorUrn(track.creatorUrn())
                                .creatorName((track.creatorName()))
                                .isPrivate(track.isPrivate())
                                .isRepost(false)
                                .getCreatedAt(track.createdAt())
                                .getImageUrlTemplate(track.imageUrlTemplate())
                                .permalinkUrl(track.permalinkUrl())
                                .description(track.description())
                                .snippetDuration(track.snippetDuration())
                                .fullDuration(track.fullDuration())
                                .waveformUrl(track.waveformUrl())
                                .monetizable(track.monetizable())
                                .isBlocked(track.blocked())
                                .isSnipped(track.snipped())
                                .commentable(track.commentable())
                                .policy(track.policy())
                                .isSubHighTier(track.subHighTier())
                                .isSubMidTier(track.subMidTier())
                                .monetizationModel(track.monetizationModel())
                                .playCount(track.playCount())
                                .commentsCount(track.commentsCount())
                                .reposter(streamEntity.reposter())
                                .reposterUrn(streamEntity.reposterUrn())
                                .avatarUrlTemplate(streamEntity.avatarUrl()).build();
    }

    public static PromotedTrackItem.Builder builder(PromotedProperties promotedProperties) {
        return new AutoValue_PromotedTrackItem.Builder().getUrn(Urn.NOT_SET)
                                                         .getKind(Kind.PROMOTED)
                                                         .offlineState(OfflineState.NOT_OFFLINE)
                                                         .isUserLike(false)
                                                         .likesCount(0)
                                                         .isUserRepost(false)
                                                         .repostsCount(0)
                                                         .genre(Optional.absent())
                                                         .title(Strings.EMPTY)
                                                         .creatorUrn(Urn.NOT_SET)
                                                         .creatorName(Strings.EMPTY)
                                                         .reposter(Optional.absent())
                                                         .reposterUrn(Optional.absent())
                                                         .avatarUrlTemplate(Optional.absent())
                                                         .isPrivate(false)
                                                         .isRepost(false)
                                                         .getCreatedAt(new Date())
                                                         .getImageUrlTemplate(Optional.absent())
                                                         .permalinkUrl(Strings.EMPTY)
                                                         .description(Optional.absent())
                                                         .snippetDuration(0)
                                                         .fullDuration(0)
                                                         .waveformUrl(Strings.EMPTY)
                                                         .monetizable(false)
                                                         .isBlocked(false)
                                                         .isSnipped(false)
                                                         .commentable(false)
                                                         .policy(Strings.EMPTY)
                                                         .isSubHighTier(false)
                                                         .isSubMidTier(false)
                                                         .monetizationModel(Strings.EMPTY)
                                                         .playCount(0)
                                                         .commentsCount(0)
                                                         .promotedProperties(promotedProperties);
    }


    public static PromotedTrackItem.Builder builder(PromotedTrackItem trackItem) {
        return new AutoValue_PromotedTrackItem.Builder(trackItem);
    }

    @AutoValue.Builder
    public abstract static class Builder {

        public abstract Builder getImageUrlTemplate(Optional<String> getImageUrlTemplate);

        public abstract Builder getCreatedAt(Date getCreatedAt);

        public abstract Builder getKind(Kind getKind);

        public abstract Builder isBlocked(boolean isBlocked);

        public abstract Builder isSnipped(boolean isSnipped);

        public abstract Builder isSubMidTier(boolean isSubMidTier);

        public abstract Builder isSubHighTier(boolean isSubHighTier);

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

        public abstract Builder description(Optional<String> description);

        public abstract Builder snippetDuration(long snippetDuration);

        public abstract Builder fullDuration(long fullDuration);

        public abstract Builder waveformUrl(String waveformUrl);

        public abstract Builder monetizable(boolean monetizable);

        public abstract Builder commentable(boolean commentable);

        public abstract Builder policy(String policy);

        public abstract Builder monetizationModel(String monetizationModel);

        public abstract Builder playCount(int playCount);

        public abstract Builder commentsCount(int commentsCount);

        public abstract Builder promotedProperties(PromotedProperties promotedProperties);

        public abstract PromotedTrackItem build();
    }
}
