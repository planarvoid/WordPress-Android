package com.soundcloud.android.sync.posts;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.soundcloud.android.api.model.ApiPlaylist;

class ApiPlaylistWrapper {

    private final ApiPlaylist apiPlaylist;

    @JsonCreator
    ApiPlaylistWrapper(@JsonProperty("playlist") ApiPlaylist apiPlaylist) {
        this.apiPlaylist = apiPlaylist;
    }

    ApiPlaylist getApiPlaylist() {
        return apiPlaylist;
    }
}
