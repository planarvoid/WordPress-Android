package com.soundcloud.android.search.topresults;

import com.google.auto.value.AutoValue;
import com.soundcloud.android.analytics.SearchQuerySourceInfo;
import com.soundcloud.android.tracks.TrackItem;

import java.util.List;

@AutoValue
abstract class TrackItemClick {
    abstract SearchQuerySourceInfo searchQuerySourceInfo();
    abstract List<TrackItem> playQueue();
    abstract int position();

    static TrackItemClick create(SearchQuerySourceInfo searchQuerySourceInfo, List<TrackItem> playQueue, int position) {
        return new AutoValue_TrackItemClick(searchQuerySourceInfo, playQueue, position);
    }
}
