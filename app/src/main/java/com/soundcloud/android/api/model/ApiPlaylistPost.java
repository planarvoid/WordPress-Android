package com.soundcloud.android.api.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.soundcloud.android.model.ApiEntityHolder;
import com.soundcloud.android.playlists.PlaylistRecord;
import com.soundcloud.android.playlists.PlaylistRecordHolder;

public class ApiPlaylistPost implements ApiEntityHolder, PlaylistRecordHolder {

    private final ApiPlaylist apiPlaylist;

    @JsonCreator
    public ApiPlaylistPost(@JsonProperty("playlist") ApiPlaylist apiPlaylist) {
        this.apiPlaylist = apiPlaylist;
    }

    @Override
    public PlaylistRecord getPlaylistRecord() {
        return apiPlaylist;
    }

    public ApiPlaylist getApiPlaylist() {
        return apiPlaylist;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ApiPlaylistPost)) {
            return false;
        }
        return apiPlaylist.equals(((ApiPlaylistPost) o).apiPlaylist);
    }

    @Override
    public int hashCode() {
        return apiPlaylist.hashCode();
    }
}
