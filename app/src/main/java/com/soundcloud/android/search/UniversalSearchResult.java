package com.soundcloud.android.search;

import com.soundcloud.android.api.model.ApiPlaylist;
import com.soundcloud.android.api.model.ApiTrack;
import com.soundcloud.android.api.model.ApiUser;
import com.soundcloud.android.model.PropertySetSource;
import com.soundcloud.propeller.PropertySet;

public class UniversalSearchResult implements PropertySetSource {

    private ApiUser user;
    private ApiPlaylist playlist;
    private ApiTrack track;

    UniversalSearchResult() { /* for Deserialization */ }

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

    public void setUser(ApiUser user) {
        this.user = user;
    }

    public ApiPlaylist getPlaylist() {
        return playlist;
    }

    public void setPlaylist(ApiPlaylist playlist) {
        this.playlist = playlist;
    }

    public ApiTrack getTrack() {
        return track;
    }

    public void setTrack(ApiTrack track) {
        this.track = track;
    }
}
