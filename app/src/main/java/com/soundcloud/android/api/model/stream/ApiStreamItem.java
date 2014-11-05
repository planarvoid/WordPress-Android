package com.soundcloud.android.api.model.stream;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Optional;
import com.soundcloud.android.api.model.ApiPlaylist;
import com.soundcloud.android.api.model.ApiTrack;
import com.soundcloud.android.api.model.ApiUser;

import java.util.Date;

public class ApiStreamItem {

    private ApiTrackPost apiTrackPost;
    private ApiTrackRepost apiTrackRepost;
    private ApiPlaylistPost apiPlaylistPost;
    private ApiPlaylistRepost apiPlaylistRepost;

    public ApiStreamItem(@JsonProperty("track_post") ApiTrackPost apiTrackPost) {
        this.apiTrackPost = apiTrackPost;
    }

    public ApiStreamItem(@JsonProperty("track_repost") ApiTrackRepost apiTrackRepost) {
        this.apiTrackRepost = apiTrackRepost;
    }

    public ApiStreamItem(@JsonProperty("playlist_post") ApiPlaylistPost apiPlaylistPost) {
        this.apiPlaylistPost = apiPlaylistPost;
    }

    public ApiStreamItem(@JsonProperty("playlist_repost") ApiPlaylistRepost apiPlaylistRepost) {
        this.apiPlaylistRepost = apiPlaylistRepost;
    }

    public Optional<ApiTrack> getTrack() {
        if (apiTrackPost != null){
            return Optional.of(apiTrackPost.getApiTrack());

        } else if (apiTrackRepost != null) {
            return Optional.of(apiTrackRepost.getApiTrack());

        } else {
            return Optional.absent();
        }
    }

    public Optional<ApiPlaylist> getPlaylist() {
        if (apiPlaylistPost != null){
            return Optional.of(apiPlaylistPost.getApiPlaylist());

        } else if (apiPlaylistRepost != null) {
            return Optional.of(apiPlaylistRepost.getApiPlaylist());

        } else {
            return Optional.absent();
        }
    }

    public Optional<ApiUser> getReposter() {
        if (apiTrackRepost != null){
            return Optional.of(apiTrackRepost.getReposter());

        } else if (apiPlaylistRepost != null) {
            return Optional.of(apiPlaylistRepost.getReposter());

        } else {
            return Optional.absent();
        }
    }

    public Date getCreatedAt() {

        if (apiTrackPost != null){
            return apiTrackPost.getApiTrack().getCreatedAt();

        } else if (apiTrackRepost != null) {
            return apiTrackRepost.getCreatedAt();

        } else if (apiPlaylistPost != null){
            return apiPlaylistPost.getApiPlaylist().getCreatedAt();

        } else if (apiPlaylistRepost != null) {
            return apiPlaylistRepost.getCreatedAt();

        } else {
            throw new IllegalArgumentException("Unknown stream item type when fecthing creation date");
        }
    }
}
