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
import com.soundcloud.android.stream.StreamEntity;
import com.soundcloud.annotations.VisibleForTesting;
import com.soundcloud.java.optional.Optional;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public abstract class TrackItem extends PlayableItem implements UpdatableTrackItem {

    public static final String PLAYABLE_TYPE = "track";

    public static TrackItem from(Track track) {
        return TrackItem.create(track.offlineState(),
                                track.userLike(),
                                track.likesCount(),
                                track.userRepost(),
                                track.repostsCount(),
                                track).build();
    }


    public static TrackItem fromTrackAndStreamEntity(Track track, StreamEntity streamEntity) {
        return TrackItem.create(track.offlineState(),
                                track.userLike(),
                                track.likesCount(),
                                track.userRepost(),
                                track.repostsCount(),
                                track)
                        .reposter(streamEntity.reposter())
                        .reposterUrn(streamEntity.reposterUrn())
                        .avatarUrlTemplate(streamEntity.avatarUrl()).build();
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

    private static TrackItem fromApiTrackWithLikeAndRepost(ApiTrack apiTrack, boolean isLiked, boolean isRepost) {
        return TrackItem.from(Track.from(apiTrack, isRepost, isLiked));
    }

    public static TrackItem fromLiked(ApiTrack apiTrack, boolean liked) {
        return fromApiTrackWithLikeAndRepost(apiTrack, liked, false);
    }

    abstract Track track();

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

    private boolean isPlaying;

    private static Default.Builder create(OfflineState offlineState,
                                          boolean isUserLike,
                                          int likesCount,
                                          boolean isUserRepost,
                                          int repostsCount,
                                          Track track) {
        return new AutoValue_TrackItem_Default.Builder().getKind(Kind.PLAYABLE)
                                                        .offlineState(offlineState)
                                                        .isUserLike(isUserLike)
                                                        .likesCount(likesCount)
                                                        .isUserRepost(isUserRepost)
                                                        .repostsCount(repostsCount)
                                                        .reposter(Optional.absent())
                                                        .reposterUrn(Optional.absent())
                                                        .avatarUrlTemplate(Optional.absent())
                                                        .track(track);
    }

    public String getPlayableType() {
        return PLAYABLE_TYPE;
    }

    public long getDuration() {
        return getTrackPlayDuration(this);
    }

    public boolean isUnavailableOffline() {
        return offlineState() == OfflineState.UNAVAILABLE;
    }

    public void setIsPlaying(boolean isPlaying) {
        this.isPlaying = isPlaying;
    }

    public boolean isPlaying() {
        return isPlaying;
    }

    public boolean hasPlayCount() {
        return track().playCount() > 0;
    }

    public boolean updateNowPlaying(Urn nowPlaying) {
        final boolean isCurrent = getUrn().equals(nowPlaying);
        if (isPlaying() || isCurrent) {
            setIsPlaying(isCurrent);
            return true;
        }
        return false;
    }

    public abstract TrackItem updatedWithTrackItem(Track track);

    public abstract TrackItem updatedWithOfflineState(OfflineState offlineState);

    public abstract TrackItem updatedWithLike(LikesStatusEvent.LikeStatus likeStatus);

    public abstract TrackItem updatedWithRepost(RepostsStatusEvent.RepostStatus repostStatus);

    public abstract TrackItem updatedWithLikeAndRepostStatus(boolean isLiked, boolean isReposted);

    public abstract TrackItem updateLikeState(boolean isLiked);

    public abstract TrackItem updateWithReposter(String reposter, Urn reposterUrn);

    @VisibleForTesting
    public static TrackItem.Default.Builder builder(TrackItem trackItem) {
        if (trackItem instanceof Default) {
            return new AutoValue_TrackItem_Default.Builder((Default) trackItem);
        }
        throw new IllegalArgumentException("Trying to create builder from promoted track item.");
    }

    @AutoValue
    public abstract static class Default extends TrackItem {

        @Override
        public TrackItem updatedWithTrackItem(Track track) {
            return TrackItem.from(track);
        }

        @Override
        public TrackItem.Default updatedWithOfflineState(OfflineState offlineState) {
            return new AutoValue_TrackItem_Default.Builder(this).offlineState(offlineState).build();
        }

        public TrackItem.Default updatedWithLike(LikesStatusEvent.LikeStatus likeStatus) {
            final Default.Builder builder = new AutoValue_TrackItem_Default.Builder(this).isUserLike(likeStatus.isUserLike());
            if (likeStatus.likeCount().isPresent()) {
                builder.likesCount(likeStatus.likeCount().get());
            }
            return builder.build();
        }

        public TrackItem.Default updatedWithRepost(RepostsStatusEvent.RepostStatus repostStatus) {
            final Default.Builder builder = new AutoValue_TrackItem_Default.Builder(this).isUserRepost(repostStatus.isReposted());
            if (repostStatus.repostCount().isPresent()) {
                builder.repostsCount(repostStatus.repostCount().get());
            }
            return builder.build();
        }

        public TrackItem.Default updatedWithLikeAndRepostStatus(boolean isLiked, boolean isReposted) {
            return new AutoValue_TrackItem_Default.Builder(this).isUserLike(isLiked).isUserRepost(isReposted).build();
        }

        @Override
        public TrackItem.Default updateLikeState(boolean isLiked) {
            return new AutoValue_TrackItem_Default.Builder(this).isUserLike(isLiked).build();
        }

        @Override
        public TrackItem.Default updateWithReposter(String reposter, Urn reposterUrn) {
            return new AutoValue_TrackItem_Default.Builder(this).reposter(Optional.of(reposter)).reposterUrn(Optional.of(reposterUrn)).build();
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

            public abstract Default build();
        }
    }
}
