package com.soundcloud.android.profile;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;
import com.soundcloud.android.api.model.ApiPlaylistPost;
import com.soundcloud.android.api.model.ApiTrackPost;
import com.soundcloud.android.api.model.ApiUser;
import com.soundcloud.android.api.model.ModelCollection;
import com.soundcloud.android.model.ApiEntityHolder;

@AutoValue
abstract class ApiUserProfile implements UserProfileRecord {
    @JsonCreator
    public static ApiUserProfile create(
            @JsonProperty("user") ApiUser user,
            @JsonProperty("spotlight") ModelCollection<ApiPlayableSource> spotlight,
            @JsonProperty("tracks") ModelCollection<ApiTrackPost> tracks,
            @JsonProperty("albums") ModelCollection<ApiPlaylistPost> albums,
            @JsonProperty("playlists") ModelCollection<ApiPlaylistPost> playlists,
            @JsonProperty("reposts") ModelCollection<ApiPlayableSource> reposts,
            @JsonProperty("likes") ModelCollection<ApiPlayableSource> likes) {

        return new AutoValue_ApiUserProfile(
                user,
                spotlight,
                tracks,
                albums,
                playlists,
                reposts,
                likes
        );
    }

    public abstract ApiEntityHolder getUser();

    public abstract ModelCollection<? extends ApiEntityHolderSource> getSpotlight();

    public abstract ModelCollection<? extends ApiEntityHolder> getTracks();

    public abstract ModelCollection<? extends ApiEntityHolder> getAlbums();

    public abstract ModelCollection<? extends ApiEntityHolder> getPlaylists();

    public abstract ModelCollection<? extends ApiEntityHolderSource> getReposts();

    public abstract ModelCollection<? extends ApiEntityHolderSource> getLikes();
}
