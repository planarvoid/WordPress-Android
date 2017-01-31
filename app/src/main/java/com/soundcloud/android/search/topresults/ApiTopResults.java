package com.soundcloud.android.search.topresults;


import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;
import com.soundcloud.android.api.model.ModelCollection;

@AutoValue
abstract class ApiTopResults {

    abstract int totalResults();

    abstract ModelCollection<ApiTopResultsBucket> buckets();

    @JsonCreator
    public static ApiTopResults create(@JsonProperty("total_results") int totalResults,
                                       @JsonProperty("buckets") ModelCollection<ApiTopResultsBucket> buckets) {
        return new AutoValue_ApiTopResults(totalResults, buckets);
    }
}
