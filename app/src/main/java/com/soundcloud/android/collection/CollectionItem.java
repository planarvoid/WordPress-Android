package com.soundcloud.android.collection;

import com.soundcloud.android.model.Urn;
import com.soundcloud.android.presentation.ListItem;
import com.soundcloud.java.collections.PropertySet;
import com.soundcloud.java.optional.Optional;

abstract class CollectionItem implements ListItem {

    static final int TYPE_PREVIEW = 0;
    static final int TYPE_HEADER = 1;
    static final int TYPE_PLAYLIST_ITEM = 2;
    static final int TYPE_PLAYLIST_REMOVE_FILTER = 3;
    static final int TYPE_PLAYLIST_EMPTY = 4;
    static final int TYPE_ONBOARDING = 5;
    static final int TYPE_TRACK_ITEM = 6;
    static final int TYPE_VIEW_ALL = 7;

    abstract int getType();

    boolean isSingleSpan() {
        return false;
    }

    @Override
    public ListItem update(PropertySet sourceSet) {
        return this;
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
