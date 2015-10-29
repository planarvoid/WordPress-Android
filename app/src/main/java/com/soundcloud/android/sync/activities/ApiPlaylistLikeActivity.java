package com.soundcloud.android.sync.activities;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.soundcloud.android.api.model.ApiPlaylist;
import com.soundcloud.android.api.model.ApiUser;
import com.soundcloud.android.model.PlaylistHolder;
import com.soundcloud.android.model.Urn;

import java.util.Date;

public class ApiPlaylistLikeActivity implements PlaylistHolder, ApiEngagementActivity {

    private final ApiPlaylist playlist;
    private final ApiUser user;
    private final Date createdAt;

    @JsonCreator
    public ApiPlaylistLikeActivity(@JsonProperty("playlist") ApiPlaylist playlist,
                                   @JsonProperty("user") ApiUser user,
                                   @JsonProperty("created_at") Date createdAt) {
        this.playlist = playlist;
        this.user = user;
        this.createdAt = createdAt;
    }

    @Override
    public ApiPlaylist getPlaylist() {
        return playlist;
    }

    @Override
    public ApiUser getUser() {
        return user;
    }

    @Override
    public Date getCreatedAt() {
        return createdAt;
    }

    @Override
    public Urn getTargetUrn() {
        return playlist.getUrn();
    }

    @Override
    public Urn getUserUrn() {
        return user.getUrn();
    }
}
