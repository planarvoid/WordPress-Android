package com.soundcloud.android.search.topresults;


import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;
import com.soundcloud.android.api.model.ModelCollection;
import com.soundcloud.android.model.Urn;

@AutoValue
abstract public class ApiTopResults {
    abstract Urn queryUrn();

    abstract int totalResults();

    abstract ModelCollection<ApiTopResultsBucket> buckets();

    @JsonCreator
    public static ApiTopResults create(@JsonProperty("query_urn") Urn queryUrn, @JsonProperty("total_results") int totalResults, ModelCollection<ApiTopResultsBucket> buckets) {
        return new AutoValue_ApiTopResults(queryUrn, totalResults, buckets);
    }
}
