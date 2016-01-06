package com.soundcloud.android.profile;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;
import com.soundcloud.android.api.model.ApiPlaylistPost;
import com.soundcloud.android.api.model.ApiTrackPost;
import com.soundcloud.android.api.model.ApiUser;
import com.soundcloud.android.api.model.ModelCollection;
import com.soundcloud.android.playlists.PlaylistRecordHolder;
import com.soundcloud.android.tracks.TrackRecordHolder;
import com.soundcloud.android.users.UserRecord;

@AutoValue
abstract class ApiUserProfile implements UserProfileRecord {
    @JsonCreator
    public static ApiUserProfile create(
            @JsonProperty("user") ApiUser user,
            @JsonProperty("spotlight") ModelCollection<ApiPlayableHolder> spotlight,
            @JsonProperty("tracks") ModelCollection<ApiTrackPost> tracks,
            @JsonProperty("releases") ModelCollection<ApiPlaylistPost> releases,
            @JsonProperty("playlists") ModelCollection<ApiPlaylistPost> playlists,
            @JsonProperty("reposts") ModelCollection<ApiPlayableHolder> reposts,
            @JsonProperty("likes") ModelCollection<ApiPlayableHolder> likes) {

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

    public abstract UserRecord getUser();

    public abstract ModelCollection<? extends BananaHolder> getSpotlight();

    public abstract ModelCollection<? extends TrackRecordHolder> getTracks();

    public abstract ModelCollection<? extends PlaylistRecordHolder> getReleases();

    public abstract ModelCollection<? extends PlaylistRecordHolder> getPlaylists();

    public abstract ModelCollection<? extends BananaHolder> getReposts();

    public abstract ModelCollection<? extends BananaHolder> getLikes();
}
