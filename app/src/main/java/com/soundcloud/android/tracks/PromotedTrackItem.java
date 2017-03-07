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
        return PromotedTrackItem.builder(track, promotedProperties)
                                .reposter(streamEntity.reposter())
                                .reposterUrn(streamEntity.reposterUrn())
                                .avatarUrlTemplate(streamEntity.avatarUrl()).build();
    }

    public static PromotedTrackItem.Builder builder(Track track, PromotedProperties promotedProperties) {
        return new AutoValue_PromotedTrackItem.Builder().getKind(Kind.PROMOTED)
                                                        .offlineState(track.offlineState())
                                                        .isUserLike(track.userLike())
                                                        .likesCount(track.likesCount())
                                                        .isUserRepost(track.userRepost())
                                                        .repostsCount(track.repostsCount())
                                                        .promotedProperties(promotedProperties)
                                                        .track(track)
                                                        .reposter(Optional.absent())
                                                        .reposterUrn(Optional.absent())
                                                        .avatarUrlTemplate(Optional.absent());
    }

    public static PromotedTrackItem.Builder builder(PromotedTrackItem trackItem) {
        return new AutoValue_PromotedTrackItem.Builder(trackItem);
    }

    @AutoValue.Builder
    public abstract static class Builder {

        public abstract Builder getKind(Kind getKind);

        public abstract Builder offlineState(OfflineState offlineState);

        public abstract Builder isUserLike(boolean isUserLike);

        public abstract Builder likesCount(int likesCount);

        public abstract Builder isUserRepost(boolean isUserRepost);

        public abstract Builder repostsCount(int repostsCount);

        public abstract Builder reposter(Optional<String> reposter);

        public abstract Builder reposterUrn(Optional<Urn> reposterUrn);

        public abstract Builder avatarUrlTemplate(Optional<String> avatarUrlTemplate);

        public abstract Builder track(Track track);

        public abstract Builder promotedProperties(PromotedProperties promotedProperties);

        public abstract PromotedTrackItem build();
    }
}
