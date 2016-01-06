package com.soundcloud.android.api.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.soundcloud.android.model.Banana;
import com.soundcloud.android.model.PostProperty;
import com.soundcloud.android.playlists.PlaylistRecord;
import com.soundcloud.android.playlists.PlaylistRecordHolder;
import com.soundcloud.java.collections.PropertySet;

public class ApiPlaylistPost implements Banana, PlaylistRecordHolder {

    private final ApiPlaylist apiPlaylist;

    @JsonCreator
    public ApiPlaylistPost(@JsonProperty("playlist") ApiPlaylist apiPlaylist) {
        this.apiPlaylist = apiPlaylist;
    }

    @Override
    public PropertySet toPropertySet() {
        return apiPlaylist.toPropertySet()
                .put(PostProperty.IS_REPOST, false)
                .put(PostProperty.CREATED_AT, apiPlaylist.getCreatedAt());
    }

    @Override
    public PlaylistRecord getPlaylistRecord() {
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
