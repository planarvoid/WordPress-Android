package com.soundcloud.android.sync.recommendations;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.soundcloud.android.api.model.ApiTrack;
import com.soundcloud.android.api.model.ModelCollection;

import android.support.annotation.Nullable;

import java.util.HashMap;
import java.util.Map;

class ApiRecommendation {

    final static Map<String, Reason> relationKeys = new HashMap<>(Reason.values().length);

    static {
        relationKeys.put("liked", Reason.LIKED);
        relationKeys.put("listened_to", Reason.LISTENED_TO);
    }

    enum Reason {
        LIKED, LISTENED_TO, UNKNOWN
    }

    private final ApiTrack seedTrack;
    private final String relationKey;
    private final ModelCollection<ApiTrack> recommendedTracks;

    @JsonCreator
    public ApiRecommendation(@JsonProperty("seed_track") ApiTrack seedTrack,
                             @JsonProperty("relation_key") String relationKey,
                             @JsonProperty("track_suggestion") ModelCollection<ApiTrack> recommendedTracks) {
        this.seedTrack = seedTrack;
        this.recommendedTracks = recommendedTracks;
        this.relationKey = relationKey;
    }

    ApiTrack getSeedTrack() {
        return seedTrack;
    }

    Iterable<ApiTrack> getRecommendations() {
        return recommendedTracks;
    }

    Reason getRecommendationReason() {
        return (relationKeys.containsKey(relationKey) ? relationKeys.get(relationKey) : Reason.UNKNOWN);
    }
}
