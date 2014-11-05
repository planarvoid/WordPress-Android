package com.soundcloud.android.api.model.stream;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.soundcloud.android.api.model.ApiPlaylist;

public class ApiPlaylistPost {

    private final ApiPlaylist apiPlaylist;

    public ApiPlaylistPost(@JsonProperty("playlist") ApiPlaylist apiPlaylist) {
        this.apiPlaylist = apiPlaylist;
    }

    public ApiPlaylist getApiPlaylist() {
        return apiPlaylist;
    }
}
