package com.soundcloud.android.collection;

import com.soundcloud.android.model.Urn;
import com.soundcloud.android.presentation.ListItem;
import com.soundcloud.java.collections.PropertySet;
import com.soundcloud.java.optional.Optional;

public abstract class CollectionItem implements ListItem {

    static final int TYPE_PREVIEW = 0;
    static final int TYPE_PLAYLIST_HEADER = 1;
    static final int TYPE_PLAYLIST_ITEM = 2;
    static final int TYPE_PLAYLIST_REMOVE_FILTER = 3;
    static final int TYPE_PLAYLIST_EMPTY = 4;
    static final int TYPE_ONBOARDING = 5;
    protected static final int TYPE_RECENTLY_PLAYED_BUCKET = 6;
    protected static final int TYPE_PLAY_HISTORY_BUCKET = 7;

    public abstract int getType();

    public boolean isSingleSpan() {
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
