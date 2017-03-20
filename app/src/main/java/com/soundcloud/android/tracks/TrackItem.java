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
import com.soundcloud.android.stream.RepostedProperties;
import com.soundcloud.android.stream.StreamEntity;
import com.soundcloud.java.optional.Optional;

import java.util.Date;

@AutoValue
public abstract class TrackItem extends PlayableItem implements UpdatableTrackItem {

    public static final String PLAYABLE_TYPE = "track";

    public static TrackItem from(ApiTrack apiTrack) {
        return  from(Track.from(apiTrack));
    }

    public static TrackItem from(Track track) {
        return builder(track).build();
    }

    public static TrackItem from(Track track, StreamEntity streamEntity) {
        final Builder builder = builder(track);
        if (streamEntity.isPromoted()) {
            builder.promotedProperties(streamEntity.promotedProperties());
        }
        if (streamEntity.isReposted()) {
            builder.repostedProperties(streamEntity.repostedProperties());
        }
        return builder.build();
    }

    public TrackItem withPlayingState(boolean isPlaying) {
        return toBuilder().isPlaying(isPlaying).build();
    }

    public static Builder builder(Track track) {
        return builder().offlineState(track.offlineState())
                        .isUserLike(track.userLike())
                        .likesCount(track.likesCount())
                        .isUserRepost(track.userRepost())
                        .repostsCount(track.repostsCount())
                        .track(track)
                        .isPlaying(false)
                        .repostedProperties(Optional.absent())
                        .promotedProperties(Optional.absent());
    }

    private static TrackItem.Builder builder() {
        return new AutoValue_TrackItem.Builder();
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

    @Override
    public TrackItem updatedWithTrack(Track track) {
        return toBuilder().track(track).build();
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

    @AutoValue.Builder
    public abstract static class Builder {

        public abstract Builder offlineState(OfflineState offlineState);

        public abstract Builder isUserLike(boolean isUserLike);

        public abstract Builder isPlaying(boolean isPlaying);

        public abstract Builder likesCount(int likesCount);

        public abstract Builder isUserRepost(boolean isUserRepost);

        public abstract Builder repostsCount(int repostsCount);

        public abstract Builder track(Track track);

        public Builder promotedProperties(PromotedProperties promotedProperties) {
            return promotedProperties(Optional.of(promotedProperties));
        }

        public abstract Builder promotedProperties(Optional<PromotedProperties> promotedProperties);

        public Builder repostedProperties(RepostedProperties repostedProperties) {
            return repostedProperties(Optional.of(repostedProperties));
        }

        public abstract Builder repostedProperties(Optional<RepostedProperties> repostedProperties);

        public abstract TrackItem build();
    }
}
