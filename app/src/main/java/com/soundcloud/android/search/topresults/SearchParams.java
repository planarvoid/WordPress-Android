package com.soundcloud.android.search.topresults;

import com.google.auto.value.AutoValue;
import com.soundcloud.android.model.Urn;
import com.soundcloud.java.optional.Optional;

@AutoValue
abstract class SearchParams {
    abstract String apiQuery();
    abstract String userQuery();
    abstract Optional<Urn> queryUrn();
    abstract Optional<Integer> queryPosition();

    static SearchParams create(String apiQuery, String userQuery, Optional<Urn> queryUrn, Optional<Integer> queryPosition) {
        return new AutoValue_SearchParams(apiQuery, userQuery, queryUrn, queryPosition);
    }
}
