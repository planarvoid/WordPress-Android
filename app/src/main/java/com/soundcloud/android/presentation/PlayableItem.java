package com.soundcloud.android.presentation;

import com.soundcloud.android.model.EntityProperty;
import com.soundcloud.android.model.PlayableProperty;
import com.soundcloud.android.model.PostProperty;
import com.soundcloud.android.model.PromotedItemProperty;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playlists.PlaylistItem;
import com.soundcloud.android.playlists.PromotedPlaylistItem;
import com.soundcloud.android.tracks.PromotedTrackItem;
import com.soundcloud.android.tracks.TrackItem;
import com.soundcloud.android.utils.ScTextUtils;
import com.soundcloud.java.collections.PropertySet;
import com.soundcloud.java.optional.Optional;

import android.content.Entity;

import java.util.Date;

public abstract class PlayableItem implements ListItem {

    protected final PropertySet source;

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
    public Urn getEntityUrn() {
        return source.get(PlayableProperty.URN);
    }

    @Override
    public PlayableItem update(PropertySet trackData) {
        this.source.update(trackData);
        return this;
    }

    public String getTitle() {
        return source.getOrElse(PlayableProperty.TITLE, ScTextUtils.EMPTY_STRING);
    }

    public String getCreatorName() {
        return source.getOrElse(PlayableProperty.CREATOR_NAME, ScTextUtils.EMPTY_STRING);
    }

    public Optional<String> getReposter() {
        return Optional.fromNullable(source.getOrElseNull(PlayableProperty.REPOSTER));
    }

    public boolean isPrivate() {
        return source.getOrElse(PlayableProperty.IS_PRIVATE, false);
    }

    public boolean isLiked() {
        return source.getOrElse(PlayableProperty.IS_LIKED, false);
    }

    public boolean isRepost() {
        return source.getOrElse(PostProperty.IS_REPOST, false);
    }

    public Urn getReposterUrn() {
        return source.get(PlayableProperty.REPOSTER_URN);
    }

    public Date getCreatedAt() {
        return source.get(PlayableProperty.CREATED_AT);
    }

    public boolean hasAdUrn() {
        return source.contains(PromotedItemProperty.AD_URN);
    }

    public PropertySet getPropertySet() {
        return source;
    }
}
