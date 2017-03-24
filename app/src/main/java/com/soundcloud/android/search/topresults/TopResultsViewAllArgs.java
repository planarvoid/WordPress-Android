package com.soundcloud.android.search.topresults;

import com.google.auto.value.AutoValue;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.search.topresults.TopResults.Bucket;
import com.soundcloud.java.optional.Optional;

@AutoValue
abstract class TopResultsViewAllArgs {
    abstract Bucket.Kind kind();

    abstract boolean isPremium();

    abstract Optional<Urn> queryUrn();

    abstract Optional<String> query();

    public static TopResultsViewAllArgs create(Bucket.Kind kind) {
        final boolean isPremium = kind == Bucket.Kind.GO_TRACKS;
        return new AutoValue_TopResultsViewAllArgs(kind, isPremium, Optional.absent(), Optional.absent());
    }

    TopResultsViewAllArgs copyWithSearchQuery(String searchQuery, Optional<Urn> queryUrn) {
        return new AutoValue_TopResultsViewAllArgs(kind(), isPremium(), queryUrn, Optional.of(searchQuery));
    }
}
