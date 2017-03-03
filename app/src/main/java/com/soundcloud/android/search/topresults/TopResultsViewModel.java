package com.soundcloud.android.search.topresults;

import com.google.auto.value.AutoValue;
import com.soundcloud.android.view.collection.CollectionRendererState;

@AutoValue
public abstract class TopResultsViewModel {

    abstract CollectionRendererState<TopResultsBucketViewModel> buckets();

    public static TopResultsViewModel create(CollectionRendererState<TopResultsBucketViewModel> buckets) {
        return new AutoValue_TopResultsViewModel(buckets);
    }
}
