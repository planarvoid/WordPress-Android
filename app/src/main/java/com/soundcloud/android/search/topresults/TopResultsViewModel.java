package com.soundcloud.android.search.topresults;

import com.google.auto.value.AutoValue;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.view.collection.CollectionRendererState;
import com.soundcloud.java.optional.Optional;

@AutoValue
public abstract class TopResultsViewModel {

    abstract Optional<Urn> queryUrn();

    abstract CollectionRendererState<TopResultsBucketViewModel> buckets();

    public static TopResultsViewModel create(Optional<Urn> queryUrn, CollectionRendererState<TopResultsBucketViewModel> buckets) {
        return new AutoValue_TopResultsViewModel(queryUrn, buckets);
    }
}
