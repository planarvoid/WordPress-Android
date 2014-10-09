package com.soundcloud.android.search;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.soundcloud.android.api.model.ApiPlaylist;
import com.soundcloud.android.api.model.ApiTrack;
import com.soundcloud.android.api.model.ApiUser;
import com.soundcloud.android.model.PropertySetSource;
import com.soundcloud.propeller.PropertySet;

public class UniversalSearchResult implements PropertySetSource {

    private final ApiUser user;
    private final ApiPlaylist playlist;
    private final ApiTrack track;

    @JsonCreator
    UniversalSearchResult(@JsonProperty("user") ApiUser user,
                          @JsonProperty("playlist") ApiPlaylist playlist,
                          @JsonProperty("track") ApiTrack track) {
        this.user = user;
        this.playlist = playlist;
        this.track = track;
    }

    static UniversalSearchResult forTrack(ApiTrack track) {
        return new UniversalSearchResult(null, null, track);
    }

    static UniversalSearchResult forPlaylist(ApiPlaylist playlist) {
        return new UniversalSearchResult(null, playlist, null);
    }

    static UniversalSearchResult forUser(ApiUser user) {
        return new UniversalSearchResult(user, null, null);
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

    public ApiUser getUser() {
        return user;
    }

    public ApiPlaylist getPlaylist() {
        return playlist;
    }

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
