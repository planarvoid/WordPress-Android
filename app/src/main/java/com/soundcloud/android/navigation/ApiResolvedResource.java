package com.soundcloud.android.navigation;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.soundcloud.android.api.model.ApiPlaylist;
import com.soundcloud.android.api.model.ApiTrack;
import com.soundcloud.android.api.model.ApiUser;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.stations.ApiStation;
import com.soundcloud.java.optional.Optional;

class ApiResolvedResource {

    private final Optional<ApiTrack> optionalTrack;
    private final Optional<ApiPlaylist> optionalPlaylist;
    private final Optional<ApiUser> optionalUser;
    private final Optional<ApiStation> optionalStation;

    @JsonCreator
    ApiResolvedResource(@JsonProperty("track") ApiTrack track,
                        @JsonProperty("playlist") ApiPlaylist playlist,
                        @JsonProperty("user") ApiUser user,
                        @JsonProperty("station") ApiStation station) {
        this.optionalTrack = Optional.fromNullable(track);
        this.optionalPlaylist = Optional.fromNullable(playlist);
        this.optionalUser = Optional.fromNullable(user);
        this.optionalStation = Optional.fromNullable(station);
    }

    Urn getUrn() {
        if (optionalTrack.isPresent()) {
            return optionalTrack.get().getUrn();
        } else if (optionalPlaylist.isPresent()) {
            return optionalPlaylist.get().getUrn();
        } else if (optionalUser.isPresent()) {
            return optionalUser.get().getUrn();
        } else if (optionalStation.isPresent()) {
            return optionalStation.get().getUrn();
        }
        return Urn.NOT_SET;
    }

    Optional<ApiTrack> getOptionalTrack() {
        return optionalTrack;
    }

    Optional<ApiPlaylist> getOptionalPlaylist() {
        return optionalPlaylist;
    }

    Optional<ApiUser> getOptionalUser() {
        return optionalUser;
    }

    Optional<ApiStation> getOptionalStation() {
        return optionalStation;
    }
}