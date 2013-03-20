package com.soundcloud.android.model;

import java.util.List;

public class SoundAssociationHolder extends CollectionHolder<SoundAssociation> {
    // needed for jackson
    public SoundAssociationHolder() {}

    public SoundAssociationHolder(List<SoundAssociation> collection) {
        super(collection);
    }
}
