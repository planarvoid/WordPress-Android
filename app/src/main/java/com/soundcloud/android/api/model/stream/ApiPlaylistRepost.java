package com.soundcloud.android.api.model.stream;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.soundcloud.android.api.model.ApiPlaylist;
import com.soundcloud.android.api.model.ApiUser;

import java.util.Date;

public class ApiPlaylistRepost {

    private final ApiPlaylist apiPlaylist;
    private final ApiUser reposter;
    private final Date createdAt;

    public ApiPlaylistRepost(@JsonProperty("playlist") ApiPlaylist apiPlaylist,
                             @JsonProperty("reposter") ApiUser reposter,
                             @JsonProperty("created_at") Date createdAt) {
        this.apiPlaylist = apiPlaylist;
        this.reposter = reposter;
        this.createdAt = createdAt;
    }

    public ApiPlaylist getApiPlaylist() {
        return apiPlaylist;
    }

    public ApiUser getReposter() {
        return reposter;
    }

    public Date getCreatedAt() {
        return createdAt;
    }
}
