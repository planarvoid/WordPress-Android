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

    public abstract Urn queryUrn();

    public abstract ModelCollection<ApiUniversalSearchItem> collection();

    @JsonCreator
    public static ApiTopResultsBucket create(Urn urn, @JsonProperty("query_urn") Urn queryUrn, @JsonProperty("total_results") int totalResults, ModelCollection<ApiUniversalSearchItem> collection) {
        return new AutoValue_ApiTopResultsBucket(urn, totalResults, queryUrn, collection);
    }
}
