package com.soundcloud.android.api.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.soundcloud.android.model.ApiEntityHolder;
import com.soundcloud.android.playlists.PlaylistRecord;
import com.soundcloud.android.playlists.PlaylistRecordHolder;

import java.util.Date;

public class ApiPlaylistRepost implements ApiEntityHolder, PlaylistRecordHolder {

    private final ApiPlaylist apiPlaylist;
    private final Date createdAt;

    @JsonCreator
    public ApiPlaylistRepost(@JsonProperty("playlist") ApiPlaylist apiPlaylist,
                             @JsonProperty("created_at") Date createdAt) {
        this.apiPlaylist = apiPlaylist;
        this.createdAt = createdAt;
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
        if (!(o instanceof ApiPlaylistRepost)) {
            return false;
        }
        ApiPlaylistRepost that = (ApiPlaylistRepost) o;
        return apiPlaylist.equals(that.apiPlaylist) && createdAt.equals(that.createdAt);
    }

    @Override
    public int hashCode() {
        int result = apiPlaylist.hashCode();
        result = 31 * result + createdAt.hashCode();
        return result;
    }
}
