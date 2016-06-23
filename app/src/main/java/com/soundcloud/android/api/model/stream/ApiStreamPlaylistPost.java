package com.soundcloud.android.api.model.stream;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.soundcloud.android.api.model.ApiPlaylist;

import java.util.Date;

public class ApiStreamPlaylistPost {

    private final ApiPlaylist apiPlaylist;
    private final long createdAtTime;

    public ApiStreamPlaylistPost(@JsonProperty("playlist") ApiPlaylist apiPlaylist,
                                 @JsonProperty("created_at") Date createdAt) {
        this.apiPlaylist = apiPlaylist;
        this.createdAtTime = createdAt.getTime();
    }

    public ApiPlaylist getApiPlaylist() {
        return apiPlaylist;
    }

    public long getCreatedAtTime() {
        return createdAtTime;
    }
}
