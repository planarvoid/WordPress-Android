package com.soundcloud.android.tracks;

import static com.soundcloud.android.playback.Durations.getTrackPlayDuration;

import auto.parcel.AutoParcel;
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
import com.soundcloud.java.strings.Strings;
import rx.functions.Func1;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public abstract class TrackItem extends PlayableItem implements TieredTrack, UpdatableTrackItem {


    public static final TrackItem EMPTY = TrackItem.Default.builder().build();


    public static TrackItem from(Track track) {
        return TrackItem.create(track.urn(),
                                track.offlineState(),
                                track.userLike(),
                                track.likesCount(),
                                track.userRepost(),
                                track.repostsCount(),
                                track.genre(),
                                track.title(),
                                track.creatorUrn(),
                                track.creatorName(),
                                track.isPrivate(),
                                false,
                                track.createdAt(),
                                track.imageUrlTemplate(),
                                track.permalinkUrl(),
                                track.description(),
                                track.snippetDuration(),
                                track.fullDuration(),
                                track.waveformUrl(),
                                track.monetizable(),
                                track.blocked(),
                                track.snipped(),
                                track.commentable(), track.policy(),
                                track.subHighTier(),
                                track.subMidTier(),
                                track.monetizationModel(),
                                track.playCount(),
                                track.commentsCount()).build();
    }


    public static TrackItem fromTrackAndStreamEntity(Track track, StreamEntity streamEntity) {
        return TrackItem.create(track.urn(),
                                track.offlineState(),
                                track.userLike(),
                                track.likesCount(),
                                track.userRepost(),
                                track.repostsCount(),
                                track.genre(),
                                track.title(),
                                track.creatorUrn(),
                                track.creatorName(),
                                track.isPrivate(),
                                false,
                                track.createdAt(),
                                track.imageUrlTemplate(),
                                track.permalinkUrl(),
                                track.description(),
                                track.snippetDuration(),
                                track.fullDuration(),
                                track.waveformUrl(),
                                track.monetizable(),
                                track.blocked(),
                                track.snipped(),
                                track.commentable(),
                                track.policy(),
                                track.subHighTier(),
                                track.subMidTier(),
                                track.monetizationModel(),
                                track.playCount(),
                                track.commentsCount())
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

    public static TrackItem.Default from(ApiTrack apiTrack) {
        return fromApiTrackWithLikeAndRepost(apiTrack, false, false);
    }

    private static TrackItem.Default fromApiTrackWithLikeAndRepost(ApiTrack apiTrack, boolean isLiked, boolean isRepost) {
        return TrackItem.create(apiTrack.getUrn(),
                                OfflineState.NOT_OFFLINE,
                                isLiked,
                                apiTrack.getLikesCount(),
                                false,
                                apiTrack.getRepostsCount(),
                                Optional.fromNullable(apiTrack.getGenre()),
                                apiTrack.getTitle(),
                                apiTrack.getUser() != null ? apiTrack.getUser().getUrn() : Urn.NOT_SET,
                                apiTrack.getUserName(),
                                apiTrack.isPrivate(),
                                isRepost,
                                apiTrack.getCreatedAt(),
                                apiTrack.getImageUrlTemplate(),
                                apiTrack.getPermalinkUrl(),
                                apiTrack.getDescription(),
                                apiTrack.getSnippetDuration(),
                                apiTrack.getFullDuration(),
                                apiTrack.getWaveformUrl(),
                                apiTrack.isMonetizable(),
                                apiTrack.isBlocked(),
                                apiTrack.isSnipped(),
                                apiTrack.isCommentable(),
                                apiTrack.getPolicy(),
                                apiTrack.isSubHighTier().or(false),
                                apiTrack.isSubMidTier().or(false),
                                apiTrack.getMonetizationModel().or(Strings.EMPTY),
                                apiTrack.getPlaybackCount(),
                                apiTrack.getCommentsCount()).build();
    }

    public static TrackItem fromLiked(ApiTrack apiTrack, boolean liked) {
        return fromApiTrackWithLikeAndRepost(apiTrack, liked, false);
    }

    public static <T extends Iterable<ApiTrack>> Func1<T, List<TrackItem>> fromApiTracks() {
        return trackList -> {
            List<TrackItem> trackItems = new ArrayList<>();
            for (ApiTrack source1 : trackList) {
                trackItems.add(from(source1));
            }
            return trackItems;
        };
    }

    public static final String PLAYABLE_TYPE = "track";

    public abstract Optional<String> description();

    public abstract long snippetDuration();

    public abstract long fullDuration();

    public abstract String waveformUrl();

    public abstract boolean monetizable();

    public abstract boolean commentable();

    public abstract String policy();

    public abstract String monetizationModel();

    public abstract int playCount();

    public abstract int commentsCount();

    private boolean isPlaying;

    public static Default.Builder create(Urn urn,
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
                                         Optional<String> description,
                                         long snippetDuration,
                                         long fullDuration,
                                         String waveformUrl,
                                         boolean monetizable,
                                         boolean blocked,
                                         boolean snipped,
                                         boolean commentable, String policy,
                                         boolean subHighTier,
                                         boolean subMidTier,
                                         String monetizationModel,
                                         int playCount,
                                         int commentsCount) {
        return Default.builder().getUrn(urn)
                      .offlineState(offlineState)
                      .isUserLike(isUserLike)
                      .likesCount(likesCount)
                      .isUserRepost(isUserRepost)
                      .repostsCount(repostsCount)
                      .genre(genre)
                      .title(title)
                      .creatorUrn(creatorUrn)
                      .creatorName(creatorName)
                      .reposter(Optional.absent())
                      .reposterUrn(Optional.absent())
                      .avatarUrlTemplate(Optional.absent())
                      .isPrivate(isPrivate)
                      .isRepost(isRepost)
                      .getCreatedAt(createdAt)
                      .getImageUrlTemplate(imageUrlTemplate)
                      .permalinkUrl(permalinkUrl)
                      .description(description)
                      .snippetDuration(snippetDuration)
                      .fullDuration(fullDuration)
                      .waveformUrl(waveformUrl)
                      .monetizable(monetizable)
                      .isBlocked(blocked)
                      .isSnipped(snipped)
                      .commentable(commentable)
                      .policy(policy)
                      .isSubHighTier(subHighTier)
                      .isSubMidTier(subMidTier)
                      .monetizationModel(monetizationModel)
                      .playCount(playCount)
                      .commentsCount(commentsCount);
    }

    @Override
    public String getPlayableType() {
        return PLAYABLE_TYPE;
    }

    @Override
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
        return playCount() > 0;
    }

    public boolean updateNowPlaying(Urn nowPlaying) {
        final boolean isCurrent = getUrn().equals(nowPlaying);
        if (isPlaying() || isCurrent) {
            setIsPlaying(isCurrent);
            return true;
        }
        return false;
    }

    public String getDescription() {
        return description().or(Strings.EMPTY);
    }

    public boolean hasDescription() {
        return description().isPresent();
    }

    public abstract TrackItem updatedWithTrackItem(Track track);

    public abstract TrackItem updatedWithOfflineState(OfflineState offlineState);

    public abstract TrackItem updatedWithLike(LikesStatusEvent.LikeStatus likeStatus);

    public abstract TrackItem updatedWithRepost(RepostsStatusEvent.RepostStatus repostStatus);

    public abstract TrackItem updatedWithLikeAndRepostStatus(boolean isLiked, boolean isReposted);

    public abstract TrackItem updateLikeState(boolean isLiked);

    public abstract TrackItem updateWithReposter(String reposter, Urn reposterUrn);

    @VisibleForTesting
    public static TrackItem.Default.Builder builder() {
        return new AutoParcel_TrackItem_Default.Builder().getUrn(Urn.NOT_SET)
                                                         .getKind(Kind.PLAYABLE)
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
                                                         .commentsCount(0);
    }

    public static TrackItem.Default.Builder builder(TrackItem trackItem) {
        if (trackItem instanceof Default) {
            return new AutoParcel_TrackItem_Default.Builder((Default) trackItem);
        }
        throw new IllegalArgumentException("Trying to create builder from promoted track item.");
    }

    @AutoParcel
    public abstract static class Default extends TrackItem {

        @Override
        public TrackItem updatedWithTrackItem(Track track) {
            return TrackItem.from(track);
        }

        @Override
        public TrackItem.Default updatedWithOfflineState(OfflineState offlineState) {
            return new AutoParcel_TrackItem_Default.Builder(this).offlineState(offlineState).build();
        }

        public TrackItem.Default updatedWithLike(LikesStatusEvent.LikeStatus likeStatus) {
            final Default.Builder builder = new AutoParcel_TrackItem_Default.Builder(this).isUserLike(likeStatus.isUserLike());
            if (likeStatus.likeCount().isPresent()) {
                builder.likesCount(likeStatus.likeCount().get());
            }
            return builder.build();
        }

        public TrackItem.Default updatedWithRepost(RepostsStatusEvent.RepostStatus repostStatus) {
            final Default.Builder builder = new AutoParcel_TrackItem_Default.Builder(this).isUserRepost(repostStatus.isReposted());
            if (repostStatus.repostCount().isPresent()) {
                builder.repostsCount(repostStatus.repostCount().get());
            }
            return builder.build();
        }

        public TrackItem.Default updatedWithLikeAndRepostStatus(boolean isLiked, boolean isReposted) {
            return new AutoParcel_TrackItem_Default.Builder(this).isUserLike(isLiked).isUserRepost(isReposted).build();
        }

        @Override
        public TrackItem.Default updateLikeState(boolean isLiked) {
            return new AutoParcel_TrackItem_Default.Builder(this).isUserLike(isLiked).build();
        }

        @Override
        public TrackItem.Default updateWithReposter(String reposter, Urn reposterUrn) {
            return new AutoParcel_TrackItem_Default.Builder(this).reposter(Optional.of(reposter)).reposterUrn(Optional.of(reposterUrn)).build();
        }

        @AutoParcel.Builder
        public abstract static class Builder {

            public abstract Builder isBlocked(boolean isBlocked);

            public abstract Builder isSnipped(boolean isSnipped);

            public abstract Builder isSubMidTier(boolean isSubMidTier);

            public abstract Builder isSubHighTier(boolean isSubHighTier);

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

            public abstract Default build();
        }
    }
}
