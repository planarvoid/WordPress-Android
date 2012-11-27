package com.soundcloud.android.model;

import com.soundcloud.android.provider.Content;

import android.content.ContentResolver;
import android.content.ContentValues;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class SoundAssociationHolder extends CollectionHolder<SoundAssociation> {

    // needed for jackson
    public SoundAssociationHolder() {}

    public SoundAssociationHolder(List<SoundAssociation> collection) {
        super(collection);
    }

    public Set<User> getUsers() {
        Set<User> users = new HashSet<User>();
        for (SoundAssociation a : this)  {
            if (a.getUser() != null) users.add(a.getUser());
        }
        return users;
    }

    public int insert(ContentResolver resolver) {
        List<ContentValues> sounds = new ArrayList<ContentValues>();
        List<ContentValues> users = new ArrayList<ContentValues>();
        List<ContentValues> items = new ArrayList<ContentValues>();

        for (SoundAssociation a : this) {
            if (a.getTrack() != null) {
                sounds.add(a.getTrack().buildContentValues());
            }
            items.add(a.buildContentValues());
        }
        for (User u : getUsers()) {
            users.add(u.buildContentValues());
        }

        int inserted = 0;
        if (!sounds.isEmpty()) {
            inserted += resolver.bulkInsert(Content.TRACKS.uri, sounds.toArray(new ContentValues[sounds.size()]));
        }
        if (!users.isEmpty())  {
            inserted += resolver.bulkInsert(Content.USERS.uri, users.toArray(new ContentValues[users.size()]));
        }
        resolver.bulkInsert(Content.COLLECTION_ITEMS.uri, items.toArray(new ContentValues[items.size()]));
        return inserted;
    }
}
