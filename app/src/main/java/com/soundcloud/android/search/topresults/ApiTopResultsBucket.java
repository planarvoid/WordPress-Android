package com.soundcloud.android.search.topresults;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;
import com.soundcloud.android.api.model.ModelCollection;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.search.ApiUniversalSearchItem;

@AutoValue
public abstract class ApiTopResultsBucket {
    public abstract Urn urn();

    public abstract int totalResults();

    public abstract ModelCollection<ApiUniversalSearchItem> collection();

    @JsonCreator
    public static ApiTopResultsBucket create(@JsonProperty("urn") Urn urn,
                                             @JsonProperty("totalResults") int totalResults,
                                             @JsonProperty("results")  ModelCollection<ApiUniversalSearchItem> collection) {
        return new AutoValue_ApiTopResultsBucket(urn, totalResults, collection);
    }
}
