package com.soundcloud.android.api.model;

import com.soundcloud.android.model.PropertySetSource;
import com.soundcloud.android.playlists.PlaylistRecord;
import com.soundcloud.android.playlists.PlaylistRecordHolder;
import com.soundcloud.android.sync.posts.PostProperty;
import com.soundcloud.java.collections.PropertySet;

import java.util.Date;

public class ApiPlaylistRepost implements PropertySetSource, PlaylistRecordHolder {

    private final ApiPlaylist apiPlaylist;
    private final Date createdAt;

    public ApiPlaylistRepost(ApiPlaylist apiPlaylist, Date createdAt) {
        this.apiPlaylist = apiPlaylist;
        this.createdAt = createdAt;
    }

    @Override
    public PropertySet toPropertySet() {
        return apiPlaylist.toPropertySet()
                .put(PostProperty.IS_REPOST, true)
                .put(PostProperty.CREATED_AT, createdAt);
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
