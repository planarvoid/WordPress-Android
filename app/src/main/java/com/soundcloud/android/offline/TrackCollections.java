package com.soundcloud.android.offline;

import com.google.auto.value.AutoValue;
import com.soundcloud.android.model.Urn;

import java.util.Collection;
import java.util.Collections;

// The only reason why we need this class is because
// we don't have urn for liked tracks collections.
@AutoValue
abstract class TrackCollections {
    public static TrackCollections EMPTY = create(Collections.emptyList(), false);

    public static TrackCollections create(Collection<Urn> playlists, boolean likesCollection) {
        return new AutoValue_TrackCollections(playlists, likesCollection);
    }

    abstract Collection<Urn> playlists();

    abstract boolean likesCollection();
}
