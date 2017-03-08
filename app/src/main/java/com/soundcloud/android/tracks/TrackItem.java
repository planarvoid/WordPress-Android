package com.soundcloud.android.tracks;

import static com.soundcloud.android.playback.Durations.getTrackPlayDuration;

import com.google.auto.value.AutoValue;
import com.soundcloud.android.api.model.ApiTrack;
import com.soundcloud.android.events.LikesStatusEvent;
import com.soundcloud.android.events.RepostsStatusEvent;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.offline.OfflineState;
import com.soundcloud.android.presentation.PlayableItem;
import com.soundcloud.android.presentation.UpdatableTrackItem;
import com.soundcloud.android.stream.PromotedProperties;
import com.soundcloud.android.stream.StreamEntity;
import com.soundcloud.java.optional.Optional;
import com.soundcloud.java.strings.Strings;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@AutoValue
public abstract class TrackItem extends PlayableItem implements UpdatableTrackItem {

    public static final String PLAYABLE_TYPE = "track";

    public static TrackItem from(Track track) {
        return builder(track).build();
    }

    public static TrackItem from(Track track, StreamEntity streamEntity, PromotedProperties promotedProperties) {
        return builder(track)
                .getKind(Kind.PROMOTED)
                .promotedProperties(promotedProperties)
                .reposter(streamEntity.reposter())
                .reposterUrn(streamEntity.reposterUrn())
                .avatarUrlTemplate(streamEntity.avatarUrl()).build();
    }

    public static TrackItem fromTrackAndStreamEntity(Track track, StreamEntity streamEntity) {
        return builder(track)
                .reposter(streamEntity.reposter())
                .reposterUrn(streamEntity.reposterUrn())
                .avatarUrlTemplate(streamEntity.avatarUrl()).build();
    }

    public TrackItem withPlayingState(boolean isPlaying) {
        return toBuilder().isPlaying(isPlaying).build();
    }

    private static Builder builder(Track track) {
        return builder().getKind(Kind.PLAYABLE)
                        .offlineState(track.offlineState())
                        .isUserLike(track.userLike())
                        .likesCount(track.likesCount())
                        .isUserRepost(track.userRepost())
                        .repostsCount(track.repostsCount())
                        .reposter(Optional.absent())
                        .reposterUrn(Optional.absent())
                        .avatarUrlTemplate(Optional.absent())
                        .track(track)
                        .isPlaying(false)
                        .promotedProperties(Optional.absent());
    }

    public static TrackItem.Builder builder() {
        return new AutoValue_TrackItem.Builder();
    }

    public static Map<Urn, TrackItem> convertMap(Map<Urn, Track> map) {
        final Map<Urn, TrackItem> result = new HashMap<>(map.size());
        for (Track track : map.values()) {
            result.put(track.urn(), from(track));
        }
        return result;
    }

    public static TrackItem from(ApiTrack apiTrack, boolean repost) {
        return TrackItem.fromApiTrackWithLikeAndRepost(apiTrack, false, repost);
    }

    public static TrackItem from(ApiTrack apiTrack) {
        return fromApiTrackWithLikeAndRepost(apiTrack, false, false);
    }

    @Override
    public boolean isPromoted() {
        return promotedProperties().isPresent();
    }

    private static TrackItem fromApiTrackWithLikeAndRepost(ApiTrack apiTrack, boolean isLiked, boolean isRepost) {
        return TrackItem.from(Track.from(apiTrack, isRepost, isLiked));
    }

    public static TrackItem fromLiked(ApiTrack apiTrack, boolean liked) {
        return fromApiTrackWithLikeAndRepost(apiTrack, liked, false);
    }

    public abstract Track track();

    public abstract boolean isPlaying();

    public Urn getUrn() {
        return track().urn();
    }

    public Optional<String> getImageUrlTemplate() {
        return track().imageUrlTemplate();
    }

    public Date getCreatedAt() {
        return track().createdAt();
    }

    public Optional<String> genre() {
        return track().genre();
    }

    public String title() {
        return track().title();
    }

    public Urn creatorUrn() {
        return track().creatorUrn();
    }

    public String creatorName() {
        return track().creatorName();
    }

    public Date createdAt() {
        return track().createdAt();
    }

    public String permalinkUrl() {
        return track().permalinkUrl();
    }

    public boolean isPrivate() {
        return track().isPrivate();
    }

    public boolean isRepost() {
        return track().userRepost();
    }

    public boolean isBlocked() {
        return track().blocked();
    }

    public boolean isSnipped() {
        return track().snipped();
    }

    public boolean isSubMidTier() {
        return track().subMidTier();
    }

    public boolean isSubHighTier() {
        return track().subHighTier();
    }

    public boolean isCommentable() {
        return track().commentable();
    }

    public String monetizationModel() {
        return track().monetizationModel();
    }

    public String policy() {
        return track().policy();
    }

    public int playCount() {
        return track().playCount();
    }

    public int commentsCount() {
        return track().commentsCount();
    }

    public long fullDuration() {
        return track().fullDuration();
    }

    public long snippetDuration() {
        return track().snippetDuration();
    }

    public String waveformUrl() {
        return track().waveformUrl();
    }

    public Optional<String> description() {
        return track().description();
    }

    @Override
    public abstract Optional<PromotedProperties> promotedProperties();

    public abstract TrackItem.Builder toBuilder();

    public String getPlayableType() {
        return PLAYABLE_TYPE;
    }

    public long getDuration() {
        return getTrackPlayDuration(this);
    }

    public boolean isUnavailableOffline() {
        return offlineState() == OfflineState.UNAVAILABLE;
    }

    public boolean hasPlayCount() {
        return track().playCount() > 0;
    }

    public TrackItem updateNowPlaying(Urn nowPlaying) {
        final boolean isCurrent = getUrn().equals(nowPlaying);
        if (isPlaying() || isCurrent) {
            return withPlayingState(isCurrent);
        } else {
            return this;
        }
    }

    public String getDescription() {
        return description().or(Strings.EMPTY);
    }

    public boolean hasDescription() {
        return description().isPresent();
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
        return isPromoted() ? promotedProperties().get().promoterUrn() : Optional.absent();
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

    @Override
    public TrackItem updatedWithTrackItem(Track track) {
        return TrackItem.from(track);
    }

    @Override
    public TrackItem updatedWithOfflineState(OfflineState offlineState) {
        return toBuilder().offlineState(offlineState).build();
    }

    public TrackItem updatedWithLike(LikesStatusEvent.LikeStatus likeStatus) {
        final Builder builder = toBuilder().isUserLike(likeStatus.isUserLike());
        if (likeStatus.likeCount().isPresent()) {
            builder.likesCount(likeStatus.likeCount().get());
        }
        return builder.build();
    }

    public TrackItem updatedWithRepost(RepostsStatusEvent.RepostStatus repostStatus) {
        final Builder builder = toBuilder().isUserRepost(repostStatus.isReposted());
        if (repostStatus.repostCount().isPresent()) {
            builder.repostsCount(repostStatus.repostCount().get());
        }
        return builder.build();
    }

    public TrackItem updatedWithLikeAndRepostStatus(boolean isLiked, boolean isReposted) {
        return toBuilder().isUserLike(isLiked).isUserRepost(isReposted).build();
    }

    @Override
    public TrackItem updateLikeState(boolean isLiked) {
        return toBuilder().isUserLike(isLiked).build();
    }

    @Override
    public TrackItem updateWithReposter(String reposter, Urn reposterUrn) {
        return toBuilder().reposter(Optional.of(reposter)).reposterUrn(Optional.of(reposterUrn)).build();
    }

    @AutoValue.Builder
    public abstract static class Builder {

        public abstract Builder getKind(Kind getKind);

        public abstract Builder offlineState(OfflineState offlineState);

        public abstract Builder isUserLike(boolean isUserLike);

        public abstract Builder isPlaying(boolean isPlaying);

        public abstract Builder likesCount(int likesCount);

        public abstract Builder isUserRepost(boolean isUserRepost);

        public abstract Builder repostsCount(int repostsCount);

        public abstract Builder reposter(Optional<String> reposter);

        public abstract Builder reposterUrn(Optional<Urn> reposterUrn);

        public abstract Builder avatarUrlTemplate(Optional<String> avatarUrlTemplate);

        public abstract Builder track(Track track);

        public Builder promotedProperties(PromotedProperties promotedProperties) {
            return promotedProperties(Optional.of(promotedProperties));
        }

        public abstract Builder promotedProperties(Optional<PromotedProperties> promotedProperties);

        public abstract TrackItem build();
    }
}
