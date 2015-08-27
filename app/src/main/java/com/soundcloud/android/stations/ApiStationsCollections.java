package com.soundcloud.android.stations;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;
import com.soundcloud.android.api.model.ModelCollection;

import java.util.List;

@AutoValue
public abstract class ApiStationsCollections {
    @JsonCreator
    public static ApiStationsCollections create(
            @JsonProperty("recent") ModelCollection<ApiStationMetadata> recents,
            @JsonProperty("saved") ModelCollection<ApiStationMetadata> saved,
            @JsonProperty("track_recommended") ModelCollection<ApiStationMetadata> trackRecommendations,
            @JsonProperty("genre_recommended") ModelCollection<ApiStationMetadata> genreRecommendations,
            @JsonProperty("curator_recommended") ModelCollection<ApiStationMetadata> curatorRecommendations) {

        return new AutoValue_ApiStationsCollections(
                recents.getCollection(),
                saved.getCollection(),
                trackRecommendations.getCollection(),
                genreRecommendations.getCollection(),
                curatorRecommendations.getCollection()
        );
    }

    public abstract List<ApiStationMetadata> getRecents();

    public abstract List<ApiStationMetadata> getSaved();

    public abstract List<ApiStationMetadata> getTrackRecommendations();

    public abstract List<ApiStationMetadata> getGenreRecommendations();

    public abstract List<ApiStationMetadata> getCuratorRecommendations();
}
