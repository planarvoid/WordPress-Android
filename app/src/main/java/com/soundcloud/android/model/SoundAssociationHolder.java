package com.soundcloud.android.model;

import com.soundcloud.android.provider.Content;

import android.content.ContentResolver;
import android.content.ContentValues;

import java.util.ArrayList;
import java.util.List;

public class SoundAssociationHolder extends CollectionHolder<SoundAssociation> {

    // needed for jackson
    public SoundAssociationHolder() {}

    public SoundAssociationHolder(List<SoundAssociation> collection) {
        super(collection);
    }



    public int insert(ContentResolver resolver) {
        List<ContentValues> sounds = new ArrayList<ContentValues>();
        List<ContentValues> items = new ArrayList<ContentValues>();

        for (SoundAssociation a : this) {
            if (a.track != null) sounds.add(a.track.buildContentValues());
            items.add(a.buildContentValues());
        }

        if (!sounds.isEmpty()) {
            resolver.bulkInsert(Content.TRACKS.uri, sounds.toArray(new ContentValues[sounds.size()]));
            resolver.bulkInsert(Content.COLLECTION_ITEMS.uri, items.toArray(new ContentValues[items.size()]));
        }
        return sounds.size();
    }
}
