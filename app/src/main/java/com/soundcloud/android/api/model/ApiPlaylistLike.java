package com.soundcloud.android.api.model;

import com.soundcloud.android.likes.LikeProperty;
import com.soundcloud.android.model.PropertySetSource;
import com.soundcloud.android.playlists.PlaylistRecord;
import com.soundcloud.android.playlists.PlaylistRecordHolder;
import com.soundcloud.propeller.PropertySet;

import java.util.Date;

public class ApiPlaylistLike implements PropertySetSource, PlaylistRecordHolder {

    private final ApiPlaylist apiPlaylist;
    private final Date createdAt;

    public ApiPlaylistLike(ApiPlaylist apiPlaylist, Date createdAt) {
        this.apiPlaylist = apiPlaylist;
        this.createdAt = createdAt;
    }

    @Override
    public PropertySet toPropertySet() {
        return apiPlaylist.toPropertySet().put(LikeProperty.CREATED_AT, createdAt);
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
        if (!(o instanceof ApiPlaylistLike)) {
            return false;
        }
        if (!apiPlaylist.equals(((ApiPlaylistLike) o).apiPlaylist)) {
            return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
        return apiPlaylist.hashCode();
    }
}
