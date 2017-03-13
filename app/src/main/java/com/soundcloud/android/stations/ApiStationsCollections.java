package com.soundcloud.android.stations;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;
import com.soundcloud.android.api.model.ModelCollection;

import java.util.Collections;
import java.util.List;

@AutoValue
public abstract class ApiStationsCollections {
    @JsonCreator
    public static ApiStationsCollections create(@JsonProperty("recent") ModelCollection<ApiStationMetadata> recents) {

        return new AutoValue_ApiStationsCollections(getCollection(recents));
    }

    private static List<ApiStationMetadata> getCollection(ModelCollection<ApiStationMetadata> stationsCollection) {
        return stationsCollection == null ?
               Collections.emptyList() :
               stationsCollection.getCollection();
    }

    public abstract List<ApiStationMetadata> getRecents();
}
