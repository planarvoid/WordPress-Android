package com.soundcloud.android.stations;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.auto.value.AutoValue;
import com.soundcloud.android.model.Urn;

import java.util.List;

@AutoValue
@JsonDeserialize
abstract class LikedStationsPostBody {

    static LikedStationsPostBody create(List<Urn> unlikedStations, List<Urn> likedStations) {
        return new AutoValue_LikedStationsPostBody(unlikedStations, likedStations);
    }

    @JsonProperty("unliked")
    abstract List<Urn> unlikedStations();

    @JsonProperty("liked")
    abstract List<Urn> likedStations();

}
