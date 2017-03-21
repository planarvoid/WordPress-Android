package com.soundcloud.android.search.topresults;

import com.google.auto.value.AutoValue;
import com.soundcloud.android.analytics.SearchQuerySourceInfo;
import com.soundcloud.android.events.EventContextMetadata;
import com.soundcloud.android.events.SearchEvent;
import com.soundcloud.android.model.Urn;

@AutoValue
abstract class GoToItemArgs {
    abstract SearchQuerySourceInfo searchQuerySourceInfo();
    abstract Urn itemUrn();
    abstract EventContextMetadata eventContextMetadata();
    abstract SearchEvent.ClickSource clickSource();

    static GoToItemArgs create(SearchQuerySourceInfo searchQuerySourceInfo, Urn itemUrn, EventContextMetadata eventContextMetadata, SearchEvent.ClickSource clickSource) {
        return new AutoValue_GoToItemArgs(searchQuerySourceInfo, itemUrn, eventContextMetadata, clickSource);
    }
}
