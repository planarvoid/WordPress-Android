package com.soundcloud.android.presentation;

import static java.lang.String.format;

import com.soundcloud.android.api.model.Timestamped;
import com.soundcloud.android.model.Entity;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.offline.OfflineState;
import com.soundcloud.android.playlists.PlaylistItem;
import com.soundcloud.android.profile.ApiPlayableSource;
import com.soundcloud.android.profile.ApiPostSource;
import com.soundcloud.android.stream.PromotedProperties;
import com.soundcloud.android.tracks.TrackItem;
import com.soundcloud.java.functions.Function;
import com.soundcloud.java.optional.Optional;

public abstract class PlayableItem implements OfflineItem, LikeableItem, RepostableItem, ListItem, Timestamped {

    public static final Function<PlayableItem, Urn> TO_URN = Entity::getUrn;

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

    public abstract Optional<String> genre();
    public abstract String title();
    public abstract Urn creatorUrn();
    public abstract String creatorName();
    public abstract String permalinkUrl();

    public abstract OfflineState offlineState();
    public abstract boolean isUserLike();
    public abstract int likesCount();
    public abstract boolean isUserRepost();
    public abstract int repostsCount();

    public abstract Optional<String> reposter();
    public abstract Optional<Urn> reposterUrn();
    public abstract boolean isPrivate();
    public abstract boolean isRepost();
    public abstract Optional<String> avatarUrlTemplate();

    public Urn getUserUrn() {
        return reposter().isPresent() && reposterUrn().isPresent() ? reposterUrn().get() : creatorUrn();
    }

    public abstract PlayableItem updateLikeState(boolean isLiked);

    public abstract PlayableItem updatedWithLikeAndRepostStatus(boolean isLikedByCurrentUser, boolean isRepostedByCurrentUser);

    public abstract PlayableItem updateWithReposter(String reposter, Urn reposterUrn);

    abstract public String getPlayableType();

    abstract public long getDuration();

    public abstract Optional<PromotedProperties> promotedProperties();

    public boolean isPromoted() {
        return promotedProperties().isPresent();
    }

    public boolean hasPromoter() {
        return promotedProperties().isPresent() && promotedProperties().get().promoterUrn().isPresent();
    }

    public String promoterName() {
        return promotedProperties().get().promoterName().get();
    }

    public Optional<Urn> promoterUrn() {
        return promotedProperties().get().promoterUrn();
    }

    public String adUrn() {
        return promotedProperties().get().adUrn();
    }
}
