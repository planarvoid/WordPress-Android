package com.soundcloud.android.profile;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.soundcloud.android.api.model.ApiPlaylistPost;
import com.soundcloud.android.api.model.ApiTrackPost;
import com.soundcloud.android.api.model.ApiUser;
import com.soundcloud.android.api.model.ModelCollection;

class ApiUserProfile implements UserProfileRecord {
    private final ApiUser user;
    private final ModelCollection<ApiPlayableSource> spotlight;
    private final ModelCollection<ApiTrackPost> tracks;
    private final ModelCollection<ApiPlaylistPost> albums;
    private final ModelCollection<ApiPlaylistPost> playlists;
    private final ModelCollection<ApiPlayableSource> reposts;
    private final ModelCollection<ApiPlayableSource> likes;

    public ApiUserProfile(
            @JsonProperty("user") ApiUser user,
            @JsonProperty("spotlight") ModelCollection<ApiPlayableSource> spotlight,
            @JsonProperty("tracks") ModelCollection<ApiTrackPost> tracks,
            @JsonProperty("albums") ModelCollection<ApiPlaylistPost> albums,
            @JsonProperty("playlists") ModelCollection<ApiPlaylistPost> playlists,
            @JsonProperty("reposts") ModelCollection<ApiPlayableSource> reposts,
            @JsonProperty("likes") ModelCollection<ApiPlayableSource> likes) {
        this.user = user;
        this.spotlight = spotlight;
        this.tracks = tracks;
        this.albums = albums;
        this.playlists = playlists;
        this.reposts = reposts;
        this.likes = likes;
    }

    public ApiUser getUser() {
        return user;
    }

    public ModelCollection<ApiPlayableSource> getSpotlight() {
        return spotlight;
    }

    public ModelCollection<ApiTrackPost> getTracks() {
        return tracks;
    }

    public ModelCollection<ApiPlaylistPost> getAlbums() {
        return albums;
    }

    public ModelCollection<ApiPlaylistPost> getPlaylists() {
        return playlists;
    }

    public ModelCollection<ApiPlayableSource> getReposts() {
        return reposts;
    }

    public ModelCollection<ApiPlayableSource> getLikes() {
        return likes;
    }
}
