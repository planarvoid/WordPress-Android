package com.soundcloud.android.presentation;

import static com.soundcloud.android.presentation.TypedListItem.Kind.PLAYABLE;
import static java.lang.String.format;

import com.soundcloud.android.events.LikesStatusEvent;
import com.soundcloud.android.events.RepostsStatusEvent;
import com.soundcloud.android.model.EntityProperty;
import com.soundcloud.android.model.PlayableProperty;
import com.soundcloud.android.model.PostProperty;
import com.soundcloud.android.model.PromotedItemProperty;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.offline.OfflineProperty;
import com.soundcloud.android.offline.OfflineState;
import com.soundcloud.android.playlists.PlaylistItem;
import com.soundcloud.android.playlists.PlaylistProperty;
import com.soundcloud.android.playlists.PromotedPlaylistItem;
import com.soundcloud.android.profile.ApiPlayableSource;
import com.soundcloud.android.profile.ApiPostSource;
import com.soundcloud.android.search.SearchableItem;
import com.soundcloud.android.stream.SoundStreamProperty;
import com.soundcloud.android.tracks.PromotedTrackItem;
import com.soundcloud.android.tracks.TrackItem;
import com.soundcloud.java.collections.Property;
import com.soundcloud.java.collections.PropertySet;
import com.soundcloud.java.functions.Function;
import com.soundcloud.java.optional.Optional;
import com.soundcloud.java.strings.Strings;

import android.support.annotation.VisibleForTesting;

import java.util.Date;

public abstract class PlayableItem implements TypedListItem, OfflineItem, LikeableItem, SearchableItem, RepostableItem  {

    protected final PropertySet source;

    public static final Function<PlayableItem, Urn> TO_URN = item -> item.getUrn();

    public static PlayableItem from(ApiPlayableSource apiPlayableSource) {
        if (apiPlayableSource.getTrack().isPresent()) {
            return TrackItem.from(apiPlayableSource.getTrack().get());
        } else {
            return PlaylistItem.from(apiPlayableSource.getPlaylist().get());
        }
    }

    public static PlayableItem from(ApiPlayableSource source, boolean repost) {
        if (source.getTrack().isPresent()) {
            return TrackItem.from(source.getTrack().get(), repost);
        } else if (source.getPlaylist().isPresent()) {
            return PlaylistItem.from(source.getPlaylist().get(), repost);
        } else {
            throw new RuntimeException(format("Empty ApiPlayableSource: %s", source));
        }
    }

    public static PlayableItem from(ApiPostSource source) {
        if (source.getTrackPost().isPresent()) {
            return TrackItem.from(source.getTrackPost().get().getApiTrack(), false);
        } else if (source.getTrackRepost().isPresent()) {
            return TrackItem.from(source.getTrackRepost().get().getApiTrack(), true);
        } else if (source.getPlaylistPost().isPresent()) {
            return PlaylistItem.from(source.getPlaylistPost().get().getApiPlaylist(), false);
        } else if (source.getPlaylistRepost().isPresent()) {
            return PlaylistItem.from(source.getPlaylistRepost().get().getApiPlaylist(), true);
        } else {
            throw new RuntimeException(format("Empty ApiPostSource: %s", source));
        }
    }

    public static PlayableItem from(PropertySet source) {
        final Urn urn = source.get(EntityProperty.URN);
        final boolean hasAdUrn = source.contains(PromotedItemProperty.AD_URN);

        if (urn.isTrack()) {
            if (hasAdUrn) {
                return PromotedTrackItem.from(source);
            } else {
                return TrackItem.from(source);
            }
        } else if (urn.isPlaylist()) {
            if (hasAdUrn) {
                return PromotedPlaylistItem.from(source);
            } else {
                return PlaylistItem.from(source);
            }
        } else {
            throw new IllegalArgumentException("Unknown playable item: " + urn);
        }
    }

    protected PlayableItem(PropertySet source) {
        this.source = source;
    }

    @Override
    public Urn getUrn() {
        return source.get(PlayableProperty.URN);
    }

    public void setUrn(Urn urn) {
        source.put(PlayableProperty.URN, urn);
    }

    public Urn getUserUrn() {
        return getReposter().isPresent() && getReposterUrn().isPresent() ? getReposterUrn().get() : getCreatorUrn();
    }

    @Override
    public PlayableItem updatedWithOfflineState(OfflineState offlineState) {
        this.source.put(OfflineProperty.OFFLINE_STATE, offlineState);
        return this;
    }

    public PlayableItem updatedWithLike(LikesStatusEvent.LikeStatus likeStatus) {
        this.source.put(PlayableProperty.IS_USER_LIKE, likeStatus.isUserLike());
        if (likeStatus.likeCount().isPresent()) {
            this.source.put(PlayableProperty.LIKES_COUNT, likeStatus.likeCount().get());
        }
        return this;
    }

    @Override
    public PlayableItem updatedWithRepost(RepostsStatusEvent.RepostStatus repostStatus) {
        this.source.put(PlayableProperty.IS_USER_REPOST, repostStatus.isReposted());
        if (repostStatus.repostCount().isPresent()) {
            this.source.put(PlayableProperty.REPOSTS_COUNT, repostStatus.repostCount().get());
        }
        return null;
    }

    @Override
    public Kind getKind() {
        return PLAYABLE;
    }

    abstract public String getPlayableType();

    abstract public long getDuration();

    public String getGenre() {
        return source.getOrElse(PlayableProperty.GENRE, Strings.EMPTY);
    }

    public String getTitle() {
        return source.getOrElse(PlayableProperty.TITLE, Strings.EMPTY);
    }

    public Urn getCreatorUrn() {
        return source.getOrElse(PlayableProperty.CREATOR_URN, Urn.NOT_SET);
    }

    public String getCreatorName() {
        return source.getOrElse(PlayableProperty.CREATOR_NAME, Strings.EMPTY);
    }

    public void setCreatorName(String creatorName) {
        source.put(PlayableProperty.CREATOR_NAME, creatorName);
    }

    public Optional<String> getReposter() {
        return Optional.fromNullable(source.getOrElseNull(PostProperty.REPOSTER));
    }

    public boolean isPrivate() {
        return source.getOrElse(PlayableProperty.IS_PRIVATE, false);
    }

    public void setPrivate(boolean isPrivate) {
        source.put(PlayableProperty.IS_PRIVATE, isPrivate);
    }

    public boolean isLikedByCurrentUser() {
        return source.getOrElse(PlayableProperty.IS_USER_LIKE, false);
    }

    public void setLikedByCurrentUser(boolean liked) {
        source.put(PlayableProperty.IS_USER_LIKE, liked);
    }

    public boolean isRepost() {
        return source.getOrElse(PostProperty.IS_REPOST, false);
    }

    public boolean isRepostedByCurrentUser() {
        return source.getOrElse(PlaylistProperty.IS_USER_REPOST, false);
    }

    public void setRepostedByCurrentUser(boolean isRepostedByUser) {
        source.put(PlaylistProperty.IS_USER_REPOST, isRepostedByUser);
    }

    public Optional<Urn> getReposterUrn() {
        return Optional.fromNullable(source.getOrElseNull(PostProperty.REPOSTER_URN));
    }

    @Override
    public Date getCreatedAt() {
        return source.get(PlayableProperty.CREATED_AT);
    }

    public void setCreatedAt(Date createdAt) {
        source.put(PlayableProperty.CREATED_AT, createdAt);
    }

    public int getLikesCount() {
        return source.getOrElse(PlayableProperty.LIKES_COUNT, 0);
    }

    public void setLikesCount(int likesCount) {
        source.put(PlayableProperty.LIKES_COUNT, likesCount);
    }

    public int getRepostCount() {
        return source.getOrElse(PlayableProperty.REPOSTS_COUNT, 0);
    }

    public void setRepostsCount(int repostsCount) {
        source.put(PlayableProperty.REPOSTS_COUNT, repostsCount);
    }

    @Override
    public Optional<String> getImageUrlTemplate() {
        return source.getOrElse(EntityProperty.IMAGE_URL_TEMPLATE, Optional.<String>absent());
    }

    public Optional<String> getAvatarUrlTemplate() {
        return source.getOrElse(SoundStreamProperty.AVATAR_URL_TEMPLATE, Optional.<String>absent());
    }

    public void setRepost(boolean repost) {
        source.put(PostProperty.IS_REPOST, repost);
    }

    public void setReposter(String reposter) {
        source.put(PostProperty.REPOSTER, reposter);
    }

    public void setReposterUrn(Urn reposterUrn) {
        source.put(PostProperty.REPOSTER_URN, reposterUrn);
    }

    public String getPermalinkUrl() {
        return source.get(PlayableProperty.PERMALINK_URL);
    }

    @VisibleForTesting
    public PropertySet slice(Property... properties) {
        return source.slice(properties);
    }
}
