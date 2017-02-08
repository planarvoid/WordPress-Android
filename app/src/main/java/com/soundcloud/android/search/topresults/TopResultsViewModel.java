package com.soundcloud.android.search.topresults;

import com.google.auto.value.AutoValue;
import com.soundcloud.android.view.adapters.CollectionViewState;

@AutoValue
abstract class TopResultsViewModel {

    abstract CollectionViewState<TopResultsBucketViewModel> buckets();

    static TopResultsViewModel create(CollectionViewState<TopResultsBucketViewModel> buckets) {
        return new AutoValue_TopResultsViewModel(buckets);
    }
}
