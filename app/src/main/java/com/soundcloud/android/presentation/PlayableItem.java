package com.soundcloud.android.presentation;

import com.soundcloud.android.api.model.Timestamped;
import com.soundcloud.android.model.Entity;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.offline.OfflineState;
import com.soundcloud.android.stream.PromotedProperties;
import com.soundcloud.android.stream.RepostedProperties;
import com.soundcloud.java.functions.Function;
import com.soundcloud.java.optional.Optional;

public abstract class PlayableItem implements OfflineItem, LikeableItem, RepostableItem, ListItem, Timestamped {

    public static final Function<PlayableItem, Urn> TO_URN = Entity::getUrn;

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

    public abstract boolean isPrivate();

    abstract public String getPlayableType();

    abstract public long getDuration();

    public abstract Optional<RepostedProperties> repostedProperties();

    public abstract Optional<PromotedProperties> promotedProperties();

    public Urn getUserUrn() {
        return repostedProperties().transform(RepostedProperties::reposterUrn).or(creatorUrn());
    }

    public abstract PlayableItem updateLikeState(boolean isLiked);

    public abstract PlayableItem updatedWithLikeAndRepostStatus(boolean isLikedByCurrentUser, boolean isRepostedByCurrentUser);

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

    public Optional<String> reposter() {
        return repostedProperties().transform(RepostedProperties::reposter);
    }

    public Optional<Urn> reposterUrn() {
        return repostedProperties().transform(RepostedProperties::reposterUrn);
    }
}
