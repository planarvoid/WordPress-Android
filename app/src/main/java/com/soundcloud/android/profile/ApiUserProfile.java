package com.soundcloud.android.profile;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;
import com.soundcloud.android.api.model.ApiPlaylistPost;
import com.soundcloud.android.api.model.ApiTrackPost;
import com.soundcloud.android.api.model.ApiUser;
import com.soundcloud.android.api.model.ModelCollection;

@AutoValue
abstract class ApiUserProfile {
    @JsonCreator
    public static ApiUserProfile create(
            @JsonProperty("user") ApiUser user,
            @JsonProperty("spotlight") ModelCollection<ApiPlayableSource> spotlight,
            @JsonProperty("tracks") ModelCollection<ApiTrackPost> tracks,
            @JsonProperty("releases") ModelCollection<ApiPlaylistPost> releases,
            @JsonProperty("playlists") ModelCollection<ApiPlaylistPost> playlists,
            @JsonProperty("reposts") ModelCollection<ApiPlayableSource> reposts,
            @JsonProperty("likes") ModelCollection<ApiPlayableSource> likes) {

        return new AutoValue_ApiUserProfile(
                user,
                spotlight,
                tracks,
                releases,
                playlists,
                reposts,
                likes
        );
    }

    public abstract ApiUser getUser();

    public abstract ModelCollection<? extends ApiEntityHolderSource> getSpotlight();

    public abstract ModelCollection<ApiTrackPost> getTracks();

    public abstract ModelCollection<ApiPlaylistPost> getReleases();

    public abstract ModelCollection<ApiPlaylistPost> getPlaylists();

    public abstract ModelCollection<? extends ApiEntityHolderSource> getReposts();

    public abstract ModelCollection<? extends ApiEntityHolderSource> getLikes();
}
