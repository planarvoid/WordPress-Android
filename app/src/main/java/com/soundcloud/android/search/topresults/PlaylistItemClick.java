package com.soundcloud.android.search.topresults;

import com.google.auto.value.AutoValue;
import com.soundcloud.android.analytics.SearchQuerySourceInfo;
import com.soundcloud.android.playlists.PlaylistItem;

@AutoValue
abstract class PlaylistItemClick {
    abstract SearchQuerySourceInfo searchQuerySourceInfo();
    abstract PlaylistItem playlistItem();

    static PlaylistItemClick create(SearchQuerySourceInfo searchQuerySourceInfo, PlaylistItem playlistItem) {
        return new AutoValue_PlaylistItemClick(searchQuerySourceInfo, playlistItem);
    }
}
