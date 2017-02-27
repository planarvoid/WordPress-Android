package com.soundcloud.android.search.topresults;

import com.google.auto.value.AutoValue;
import com.soundcloud.android.analytics.SearchQuerySourceInfo;
import com.soundcloud.android.model.Urn;

@AutoValue
abstract class GoToProfileArgs {
    abstract SearchQuerySourceInfo searchQuerySourceInfo();
    abstract Urn user();
    static GoToProfileArgs create(SearchQuerySourceInfo searchQuerySourceInfo, Urn user) {
        return new AutoValue_GoToProfileArgs(searchQuerySourceInfo, user);
    }
}
