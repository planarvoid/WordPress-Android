package com.soundcloud.android.search;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.soundcloud.android.api.model.ApiPlaylist;
import com.soundcloud.android.api.model.ApiTrack;
import com.soundcloud.android.api.model.ApiUser;
import com.soundcloud.android.model.PropertySetSource;
import com.soundcloud.java.collections.PropertySet;
import org.jetbrains.annotations.Nullable;

/**
 * Has either a user XOR a playlist XOR a track set, as it represents a result item from a universal search.
 */
class ApiUniversalSearchItem implements PropertySetSource {

    private final ApiUser user;
    private final ApiPlaylist playlist;
    private final ApiTrack track;

    @JsonCreator
    ApiUniversalSearchItem(@JsonProperty("user") @Nullable ApiUser user,
                           @JsonProperty("playlist") @Nullable ApiPlaylist playlist,
                           @JsonProperty("track") @Nullable ApiTrack track) {
        this.user = user;
        this.playlist = playlist;
        this.track = track;
    }

    static ApiUniversalSearchItem forTrack(ApiTrack track) {
        return new ApiUniversalSearchItem(null, null, track);
    }

    static ApiUniversalSearchItem forPlaylist(ApiPlaylist playlist) {
        return new ApiUniversalSearchItem(null, playlist, null);
    }

    static ApiUniversalSearchItem forUser(ApiUser user) {
        return new ApiUniversalSearchItem(user, null, null);
    }

    @Override
    public PropertySet toPropertySet() {
        if (user != null) {
            return user.toPropertySet();
        }
        if (playlist != null) {
            return playlist.toPropertySet();
        }
        if (track != null) {
            return track.toPropertySet();
        }
        throw new IllegalStateException("missing wrapped search result entity");
    }

    @Nullable
    public ApiUser getUser() {
        return user;
    }

    @Nullable
    public ApiPlaylist getPlaylist() {
        return playlist;
    }

    @Nullable
    public ApiTrack getTrack() {
        return track;
    }

    public boolean isUser() {
        return user != null;
    }

    public boolean isPlaylist() {
        return playlist != null;
    }

    public boolean isTrack() {
        return track != null;
    }
}
