package com.soundcloud.android.sync.activities;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.soundcloud.android.api.model.ApiPlaylist;
import com.soundcloud.android.api.model.ApiUser;

import java.util.Date;

public class ApiPlaylistLike {

    private final ApiPlaylist playlist;
    private final ApiUser user;
    private final Date createdAt;

    @JsonCreator
    public ApiPlaylistLike(@JsonProperty("playlist") ApiPlaylist playlist,
                           @JsonProperty("user") ApiUser user,
                           @JsonProperty("created_at") Date createdAt) {
        this.playlist = playlist;
        this.user = user;
        this.createdAt = createdAt;
    }

    public ApiPlaylist getPlaylist() {
        return playlist;
    }

    public ApiUser getUser() {
        return user;
    }

    public Date getCreatedAt() {
        return createdAt;
    }
}
