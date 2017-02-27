package com.soundcloud.android.search.topresults;

import com.google.auto.value.AutoValue;
import com.soundcloud.android.analytics.SearchQuerySourceInfo;
import com.soundcloud.android.events.EventContextMetadata;
import com.soundcloud.android.model.Urn;

@AutoValue
abstract class GoToPlaylistArgs {
    abstract SearchQuerySourceInfo searchQuerySourceInfo();
    abstract Urn playlistUrn();
    abstract EventContextMetadata eventContextMetadata();

    static GoToPlaylistArgs create(SearchQuerySourceInfo searchQuerySourceInfo, Urn playlistUrn, EventContextMetadata eventContextMetadata) {
        return new AutoValue_GoToPlaylistArgs(searchQuerySourceInfo, playlistUrn, eventContextMetadata);
    }
}
