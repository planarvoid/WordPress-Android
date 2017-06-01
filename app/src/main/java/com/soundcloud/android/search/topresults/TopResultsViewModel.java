package com.soundcloud.android.search.topresults;

import com.google.auto.value.AutoValue;
import com.soundcloud.android.model.Urn;
import com.soundcloud.java.optional.Optional;

import java.util.List;

@AutoValue
public abstract class TopResultsViewModel {

    abstract Optional<Urn> queryUrn();

    abstract List<TopResultsBucketViewModel> buckets();

    public static TopResultsViewModel create(Optional<Urn> queryUrn, List<TopResultsBucketViewModel> buckets) {
        return new AutoValue_TopResultsViewModel(queryUrn, buckets);
    }
}
