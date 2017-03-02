package com.soundcloud.android.search.topresults;

import com.google.auto.value.AutoValue;
import com.soundcloud.android.model.Urn;
import com.soundcloud.java.optional.Optional;

@AutoValue
abstract class TopResultsViewAllArgs {
    abstract TopResultsBucketViewModel.Kind kind();

    abstract boolean isPremium();

    abstract Urn queryUrn();

    abstract Optional<String> query();

    public static TopResultsViewAllArgs create(TopResultsBucketViewModel.Kind kind, Urn queryUrn) {
        final boolean isPremium = kind == TopResultsBucketViewModel.Kind.GO_TRACKS;
        return new AutoValue_TopResultsViewAllArgs(kind, isPremium, queryUrn, Optional.absent());
    }

    public TopResultsViewAllArgs copyWithSearchQuery(String searchQuery) {
        return new AutoValue_TopResultsViewAllArgs(kind(), isPremium(), queryUrn(), Optional.of(searchQuery));
    }
}
