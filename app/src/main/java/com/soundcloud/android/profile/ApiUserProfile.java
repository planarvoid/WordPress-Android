package com.soundcloud.android.profile;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;
import com.soundcloud.android.api.model.ApiPlaylistPost;
import com.soundcloud.android.api.model.ApiTrackPost;
import com.soundcloud.android.api.model.ApiUser;
import com.soundcloud.android.api.model.ModelCollection;
import com.soundcloud.java.optional.Optional;

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
                Optional.fromNullable(spotlight),
                Optional.fromNullable(tracks),
                Optional.fromNullable(releases),
                Optional.fromNullable(playlists),
                Optional.fromNullable(reposts),
                Optional.fromNullable(likes)
        );
    }

    public abstract ApiUser getUser();

    public abstract Optional<ModelCollection<ApiPlayableSource>> getSpotlight();

    public abstract Optional<ModelCollection<ApiTrackPost>> getTracks();

    public abstract Optional<ModelCollection<ApiPlaylistPost>> getReleases();

    public abstract Optional<ModelCollection<ApiPlaylistPost>> getPlaylists();

    public abstract Optional<ModelCollection<ApiPlayableSource>> getReposts();

    public abstract Optional<ModelCollection<ApiPlayableSource>> getLikes();
}
