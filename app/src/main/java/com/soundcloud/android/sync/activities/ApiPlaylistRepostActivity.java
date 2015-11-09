package com.soundcloud.android.sync.activities;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.soundcloud.android.api.model.ApiPlaylist;
import com.soundcloud.android.api.model.ApiUser;
import com.soundcloud.android.model.PlaylistHolder;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.users.UserRecord;

import java.util.Date;

public class ApiPlaylistRepostActivity implements PlaylistHolder, ApiEngagementActivity {

    private final ApiPlaylist playlist;
    private final ApiUser reposter;
    private final Date createdAt;

    @JsonCreator
    public ApiPlaylistRepostActivity(@JsonProperty("playlist") ApiPlaylist playlist,
                                     @JsonProperty("reposter") ApiUser reposter,
                                     @JsonProperty("created_at") Date createdAt) {
        this.playlist = playlist;
        this.reposter = reposter;
        this.createdAt = createdAt;
    }

    @Override
    public ApiPlaylist getPlaylist() {
        return playlist;
    }

    @Override
    public Date getCreatedAt() {
        return createdAt;
    }

    @Override
    public UserRecord getUser() {
        return reposter;
    }

    @Override
    public Urn getTargetUrn() {
        return playlist.getUrn();
    }

    @Override
    public Urn getUserUrn() {
        return reposter.getUrn();
    }
}
