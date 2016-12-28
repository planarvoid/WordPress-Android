package com.soundcloud.android.api.model;

import com.soundcloud.android.model.ApiEntityHolder;
import com.soundcloud.android.playlists.PlaylistRecord;
import com.soundcloud.android.playlists.PlaylistRecordHolder;

import java.util.Date;

public class ApiPlaylistLike implements ApiEntityHolder, PlaylistRecordHolder {

    private final ApiPlaylist apiPlaylist;
    private final Date createdAt;

    public ApiPlaylistLike(ApiPlaylist apiPlaylist, Date createdAt) {
        this.apiPlaylist = apiPlaylist;
        this.createdAt = createdAt;
    }

    @Override
    public PlaylistRecord getPlaylistRecord() {
        return apiPlaylist;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ApiPlaylistLike)) {
            return false;
        }
        return apiPlaylist.equals(((ApiPlaylistLike) o).apiPlaylist);
    }

    @Override
    public int hashCode() {
        return apiPlaylist.hashCode();
    }

}
