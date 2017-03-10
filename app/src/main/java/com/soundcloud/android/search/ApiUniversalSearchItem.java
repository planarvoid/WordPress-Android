package com.soundcloud.android.search;

import static com.soundcloud.java.optional.Optional.fromNullable;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.soundcloud.android.api.model.ApiPlaylist;
import com.soundcloud.android.api.model.ApiTrack;
import com.soundcloud.android.api.model.ApiUser;
import com.soundcloud.java.optional.Optional;
import org.jetbrains.annotations.Nullable;

/**
 * Has either a user XOR a playlist XOR a track set, as it represents a result item from a universal search.
 */
public class ApiUniversalSearchItem {

    private final Optional<ApiUser> user;
    private final Optional<ApiPlaylist> playlist;
    private final Optional<ApiTrack> track;

    @JsonCreator
    public ApiUniversalSearchItem(@JsonProperty("user") @Nullable ApiUser user,
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
}
