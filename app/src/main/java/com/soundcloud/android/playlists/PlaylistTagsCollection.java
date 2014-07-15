package com.soundcloud.android.playlists;

import com.soundcloud.android.api.model.ModelCollection;

import java.util.List;

public class PlaylistTagsCollection extends ModelCollection<String> {

    public PlaylistTagsCollection() {
        super();
    }

    public PlaylistTagsCollection(List<String> collection) {
        super(collection);
    }

}
