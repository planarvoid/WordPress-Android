package com.soundcloud.android.events;

import com.soundcloud.android.api.model.ApiPlaylist;
import com.soundcloud.android.model.PlayableProperty;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playback.ui.PlayerTrackState;
import com.soundcloud.android.playlists.PlaylistHeaderItem;
import com.soundcloud.android.playlists.PlaylistWithTracks;
import com.soundcloud.android.presentation.PlayableItem;
import com.soundcloud.android.users.UserProperty;
import com.soundcloud.java.collections.PropertySet;
import com.soundcloud.java.objects.MoreObjects;
import com.soundcloud.java.strings.Strings;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

public class EntityMetadata {
    public static final EntityMetadata EMPTY = new EntityMetadata(Strings.EMPTY,
                                                                  Urn.NOT_SET,
                                                                  Strings.EMPTY,
                                                                  Urn.NOT_SET);

    public static final String KEY_CREATOR_NAME = "creator_display_name";
    public static final String KEY_CREATOR_URN = "creator_urn";
    public static final String KEY_PLAYABLE_TITLE = "playable_title";
    public static final String KEY_PLAYABLE_URN = "playable_urn";
    public static final String KEY_PLAYABLE_TYPE = "playable_type";

    private static final String TYPE_TRACK = "track";
    private static final String TYPE_PLAYLIST = "playlist";
    private static final String TYPE_STATION = "station";
    private static final String TYPE_OTHER = "other";

    public final String creatorName;
    public final Urn creatorUrn;
    public final String playableTitle;
    public final Urn playableUrn;

    EntityMetadata(String creatorName, Urn creatorUrn, String playableTitle, Urn playableUrn) {
        this.creatorName = creatorName;
        this.creatorUrn = creatorUrn;
        this.playableTitle = playableTitle;
        this.playableUrn = playableUrn;
    }

    public static EntityMetadata fromUser(@Nullable PropertySet userProperties) {
        if (userProperties == null) {
            return EMPTY;
        }

        return new EntityMetadata(
                userProperties.getOrElse(UserProperty.USERNAME, Strings.EMPTY),
                userProperties.getOrElse(UserProperty.URN, Urn.NOT_SET),
                Strings.EMPTY,
                Urn.NOT_SET);
    }

    public static EntityMetadata from(@Nullable PropertySet playableProperties) {
        if (playableProperties == null) {
            return EMPTY;
        }

        return new EntityMetadata(
                playableProperties.getOrElse(PlayableProperty.CREATOR_NAME, Strings.EMPTY),
                playableProperties.getOrElse(PlayableProperty.CREATOR_URN, Urn.NOT_SET),
                playableProperties.getOrElse(PlayableProperty.TITLE, Strings.EMPTY),
                playableProperties.getOrElse(PlayableProperty.URN, Urn.NOT_SET));
    }

    public static EntityMetadata from(@Nullable PlayableItem playable) {
        if (playable == null) {
            return EMPTY;
        }

        return new EntityMetadata(
                playable.getCreatorName(),
                playable.getCreatorUrn(),
                playable.getTitle(),
                playable.getUrn());
    }

    public static EntityMetadata from(@Nullable PlayerTrackState track) {
        if (track == null) {
            return EMPTY;
        }
        return new EntityMetadata(
                track.getUserName(),
                track.getUserUrn(),
                track.getTitle(),
                track.getTrackUrn());
    }

    public static EntityMetadata from(@Nullable PlaylistWithTracks playlist) {
        if (playlist == null) {
            return EMPTY;
        }
        return new EntityMetadata(
                playlist.getCreatorName(),
                playlist.getCreatorUrn(),
                playlist.getTitle(),
                playlist.getUrn());
    }

    public static EntityMetadata from(@Nullable PlaylistHeaderItem playlist) {
        if (playlist == null) {
            return EMPTY;
        }
        return new EntityMetadata(
                playlist.getCreatorName(),
                playlist.getCreatorUrn(),
                playlist.getTitle(),
                playlist.getUrn());
    }

    public static EntityMetadata from(ApiPlaylist playlist) {
        if (playlist == null) {
            return EMPTY;
        }
        return new EntityMetadata(
                playlist.getUsername(),
                playlist.getUser().getUrn(),
                playlist.getTitle(),
                playlist.getUrn());
    }

    void addToTrackingEvent(@NonNull LegacyTrackingEvent event) {
        event.put(KEY_CREATOR_NAME, creatorName)
             .put(KEY_CREATOR_URN, creatorUrn.toString())
             .put(KEY_PLAYABLE_TITLE, playableTitle)
             .put(KEY_PLAYABLE_URN, playableUrn.toString())
             .put(KEY_PLAYABLE_TYPE, getPlayableType());
    }

    private String getPlayableType() {
        if (playableUrn.isTrack()) {
            return TYPE_TRACK;
        } else if (playableUrn.isPlaylist()) {
            return TYPE_PLAYLIST;
        } else if (playableUrn.isStation()) {
            return TYPE_STATION;
        } else {
            return TYPE_OTHER;
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        EntityMetadata that = (EntityMetadata) o;
        return MoreObjects.equal(creatorName, that.creatorName) &&
                MoreObjects.equal(creatorUrn, that.creatorUrn) &&
                MoreObjects.equal(playableTitle, that.playableTitle) &&
                MoreObjects.equal(playableUrn, that.playableUrn);
    }

    @Override
    public int hashCode() {
        return MoreObjects.hashCode(creatorName, creatorUrn, playableTitle, playableUrn);
    }
}
