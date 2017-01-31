package com.soundcloud.android.search.topresults;

import com.google.auto.value.AutoValue;

import java.util.List;

@AutoValue
abstract class TopResultsViewModel {

    abstract List<TopResultsBucketViewModel> buckets();

    static TopResultsViewModel create(List<TopResultsBucketViewModel> buckets) {
        return new AutoValue_TopResultsViewModel(buckets);
    }
}
