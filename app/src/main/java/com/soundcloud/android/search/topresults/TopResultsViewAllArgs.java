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

    public static TopResultsViewAllArgs create(Bucket.Kind kind, Optional<Urn> queryUrn) {
        final boolean isPremium = kind == Bucket.Kind.GO_TRACKS;
        return new AutoValue_TopResultsViewAllArgs(kind, isPremium, queryUrn);
    }
}
