package com.soundcloud.android.playlists;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.soundcloud.android.api.model.ApiPlaylist;
import com.soundcloud.android.api.model.Link;
import com.soundcloud.android.api.model.ModelCollection;

import android.support.annotation.VisibleForTesting;

import java.util.List;
import java.util.Map;

public final class ApiPlaylistCollection extends ModelCollection<ApiPlaylist> {
    public ApiPlaylistCollection(@JsonProperty("collection") List<ApiPlaylist> collection,
                                 @JsonProperty("_links") Map<String, Link> links,
                                 @JsonProperty("query_urn") String queryUrn) {
        super(collection, links, queryUrn);
    }

    @VisibleForTesting
    public ApiPlaylistCollection(List<ApiPlaylist> apiPlaylists) {
        super(apiPlaylists);
    }
}
