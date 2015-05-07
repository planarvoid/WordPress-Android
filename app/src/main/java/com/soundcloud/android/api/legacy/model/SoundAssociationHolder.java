package com.soundcloud.android.api.legacy.model;

import java.util.List;

public class SoundAssociationHolder extends CollectionHolder<SoundAssociation> {
    // needed for jackson
    public SoundAssociationHolder() {}

    public SoundAssociationHolder(List<SoundAssociation> collection) {
        super(collection);
    }

    public SoundAssociationHolder(List<SoundAssociation> collection, String nextHref) {
        super(collection, nextHref);
    }
}
