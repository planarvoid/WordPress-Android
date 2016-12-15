package com.soundcloud.android.presentation;

import static com.soundcloud.android.presentation.TypedListItem.Kind.PLAYABLE;

import com.soundcloud.android.model.EntityProperty;
import com.soundcloud.android.model.PlayableProperty;
import com.soundcloud.android.model.PostProperty;
import com.soundcloud.android.model.PromotedItemProperty;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.offline.OfflineProperty;
import com.soundcloud.android.offline.OfflineState;
import com.soundcloud.android.playlists.PlaylistItem;
import com.soundcloud.android.playlists.PromotedPlaylistItem;
import com.soundcloud.android.stream.SoundStreamProperty;
import com.soundcloud.android.tracks.PromotedTrackItem;
import com.soundcloud.android.tracks.TrackItem;
import com.soundcloud.java.collections.PropertySet;
import com.soundcloud.java.functions.Function;
import com.soundcloud.java.optional.Optional;
import com.soundcloud.java.strings.Strings;

import java.util.Date;

public abstract class PlayableItem implements TypedListItem, OfflineItem, UpdatableItem, LikeableItem {

    protected final PropertySet source;

    public static final Function<PlayableItem, Urn> TO_URN = new Function<PlayableItem, Urn>() {
        @Override
        public Urn apply(PlayableItem item) {
            return item.getUrn();
        }
    };

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

    public Urn getUserUrn() {
        return getReposter().isPresent() ? getReposterUrn() : getCreatorUrn();
    }

    @Override
    public PlayableItem updated(PropertySet playableData) {
        this.source.update(playableData);
        return this;
    }

    @Override
    public PlayableItem updatedWithOfflineState(OfflineState offlineState) {
        this.source.put(OfflineProperty.OFFLINE_STATE, offlineState);
        return this;
    }

    @Override
    public Kind getKind() {
        return PLAYABLE;
    }

    abstract public String getPlayableType();

    public String getTitle() {
        return source.getOrElse(PlayableProperty.TITLE, Strings.EMPTY);
    }

    public Urn getCreatorUrn() {
        return source.getOrElse(PlayableProperty.CREATOR_URN, Urn.NOT_SET);
    }

    public String getCreatorName() {
        return source.getOrElse(PlayableProperty.CREATOR_NAME, Strings.EMPTY);
    }

    public Optional<String> getReposter() {
        return Optional.fromNullable(source.getOrElseNull(PostProperty.REPOSTER));
    }

    public boolean isPrivate() {
        return source.getOrElse(PlayableProperty.IS_PRIVATE, false);
    }

    public boolean isLiked() {
        return source.getOrElse(PlayableProperty.IS_USER_LIKE, false);
    }

    public boolean isRepostedByCurrentUser() {
        return source.getOrElse(PlayableProperty.IS_USER_REPOST, false);
    }

    public boolean isRepost() {
        return source.getOrElse(PostProperty.IS_REPOST, false);
    }

    public Urn getReposterUrn() {
        return source.get(PostProperty.REPOSTER_URN);
    }

    @Override
    public Date getCreatedAt() {
        return source.get(PlayableProperty.CREATED_AT);
    }

    public int getLikesCount() {
        return source.getOrElse(PlayableProperty.LIKES_COUNT, 0);
    }

    public int getRepostCount() {
        return source.getOrElse(PlayableProperty.REPOSTS_COUNT, 0);
    }

    @Override
    public Optional<String> getImageUrlTemplate() {
        return source.getOrElse(EntityProperty.IMAGE_URL_TEMPLATE, Optional.<String>absent());
    }

    public Optional<String> getAvatarUrlTemplate() {
        return source.getOrElse(SoundStreamProperty.AVATAR_URL_TEMPLATE, Optional.<String>absent());
    }

    public PropertySet getSource() {
        return source;
    }
}
