package com.soundcloud.android.search.topresults;

import com.google.auto.value.AutoValue;
import com.soundcloud.android.view.adapters.CollectionViewState;

@AutoValue
public abstract class TopResultsViewModel {

    abstract CollectionViewState<TopResultsBucketViewModel> buckets();

    public static TopResultsViewModel create(CollectionViewState<TopResultsBucketViewModel> buckets) {
        return new AutoValue_TopResultsViewModel(buckets);
    }
}
