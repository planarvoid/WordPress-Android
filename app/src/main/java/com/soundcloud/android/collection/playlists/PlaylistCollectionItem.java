package com.soundcloud.android.collection.playlists;

import com.soundcloud.android.model.Urn;
import com.soundcloud.android.presentation.ListItem;
import com.soundcloud.java.optional.Optional;

abstract class PlaylistCollectionItem implements ListItem {

    static final int TYPE_HEADER = 1;
    static final int TYPE_PLAYLIST = 2;
    static final int TYPE_REMOVE_FILTER = 3;
    static final int TYPE_EMPTY = 4;

    public abstract int getType();

    public boolean isSingleSpan() {
        return false;
    }

    @Override
    public Urn getUrn() {
        return Urn.NOT_SET;
    }

    @Override
    public Optional<String> getImageUrlTemplate() {
        return Optional.absent();
    }
}

