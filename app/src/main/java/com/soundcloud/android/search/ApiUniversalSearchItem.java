package com.soundcloud.android.search;

import static com.soundcloud.java.optional.Optional.fromNullable;
import static java.lang.String.format;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.soundcloud.android.api.model.ApiPlaylist;
import com.soundcloud.android.api.model.ApiTrack;
import com.soundcloud.android.api.model.ApiUser;
import com.soundcloud.android.playlists.PlaylistItem;
import com.soundcloud.android.tracks.TrackItem;
import com.soundcloud.android.users.UserItem;
import com.soundcloud.java.optional.Optional;
import org.jetbrains.annotations.Nullable;

/**
 * Has either a user XOR a playlist XOR a track set, as it represents a result item from a universal search.
 */
class ApiUniversalSearchItem {

    private final Optional<ApiUser> user;
    private final Optional<ApiPlaylist> playlist;
    private final Optional<ApiTrack> track;

    @JsonCreator
    ApiUniversalSearchItem(@JsonProperty("user") @Nullable ApiUser user,
                           @JsonProperty("playlist") @Nullable ApiPlaylist playlist,
                           @JsonProperty("track") @Nullable ApiTrack track) {
        this.user = fromNullable(user);
        this.playlist = fromNullable(playlist);
        this.track = fromNullable(track);
    }

    public Optional<ApiUser> user() {
        return user;
    }

    public Optional<ApiPlaylist> playlist() {
        return playlist;
    }

    public Optional<ApiTrack> track() {
        return track;
    }


    public SearchableItem toSearchableItem() {
        if (this.track().isPresent()) {
            return TrackItem.from(this.track().get());
        } else if (this.playlist().isPresent()) {
            return PlaylistItem.from(this.playlist().get());
        } else if (this.user().isPresent()) {
            return UserItem.from(this.user().get());
        } else {
            throw new RuntimeException(format("Empty ApiUniversalSearchItem: %s", this));
        }
    }
}
