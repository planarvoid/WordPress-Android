package com.soundcloud.android.events;

import com.soundcloud.android.api.model.ApiPlaylist;
import com.soundcloud.android.api.model.ApiUser;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playback.ui.PlayerTrackState;
import com.soundcloud.android.presentation.PlayableItem;
import com.soundcloud.android.tracks.Track;
import com.soundcloud.android.users.User;
import com.soundcloud.java.objects.MoreObjects;
import com.soundcloud.java.strings.Strings;

import android.support.annotation.Nullable;

public class EntityMetadata {
    public static final EntityMetadata EMPTY = new EntityMetadata(Strings.EMPTY,
                                                                  Urn.NOT_SET,
                                                                  Strings.EMPTY,
                                                                  Urn.NOT_SET);

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

    public static EntityMetadata fromUser(@Nullable ApiUser apiUser) {
        if (apiUser == null) {
            return EMPTY;
        }

        return new EntityMetadata(
                apiUser.getUsername(),
                apiUser.getUrn(),
                Strings.EMPTY,
                Urn.NOT_SET);
    }

    public static EntityMetadata fromUser(@Nullable User user) {
        if (user == null) {
            return EMPTY;
        }

        return new EntityMetadata(
                user.username(),
                user.urn(),
                Strings.EMPTY,
                Urn.NOT_SET);
    }

    public static EntityMetadata from(@Nullable PlayableItem playable) {
        if (playable == null) {
            return EMPTY;
        }

        return new EntityMetadata(
                playable.creatorName(),
                playable.creatorUrn(),
                playable.title(),
                playable.getUrn());
    }

    public static EntityMetadata from(@Nullable Track track) {
        if (track == null) {
            return EMPTY;
        }

        return new EntityMetadata(
                track.creatorName(),
                track.creatorUrn(),
                track.title(),
                track.urn());
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

    public static EntityMetadata from(String creatorName, Urn creatorUrn, String title, Urn urn) {
        return new EntityMetadata(creatorName, creatorUrn, title, urn);
    }

    String getPlayableType() {
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
