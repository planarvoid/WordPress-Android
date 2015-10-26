package com.soundcloud.android.sync.activities;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.soundcloud.android.api.model.ApiPlaylist;
import com.soundcloud.android.api.model.ApiUser;

import java.util.Date;

public class ApiPlaylistRepostActivity {

    private final ApiPlaylist playlist;
    private final ApiUser reposter;
    private final Date createdAt;

    @JsonCreator
    public ApiPlaylistRepostActivity(@JsonProperty("playlist") ApiPlaylist playlist,
                                     @JsonProperty("user") ApiUser reposter,
                                     @JsonProperty("created_at") Date createdAt) {
        this.playlist = playlist;
        this.reposter = reposter;
        this.createdAt = createdAt;
    }

    public ApiPlaylist getPlaylist() {
        return playlist;
    }

    public ApiUser getReposter() {
        return reposter;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

}
