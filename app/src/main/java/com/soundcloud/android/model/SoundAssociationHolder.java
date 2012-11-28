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

    public Set<Track> getTracks() {
        Set<Track> tracks = new HashSet<Track>();
        for (SoundAssociation a : this)  {
            if (a.getTrack() != null) tracks.add(a.getTrack());
        }
        return tracks;
    }

    public int insert(ContentResolver resolver) {
        List<ContentValues> tracks = new ArrayList<ContentValues>();
        List<ContentValues> users = new ArrayList<ContentValues>();
        List<ContentValues> items = new ArrayList<ContentValues>();

        for (SoundAssociation a : this) {
            items.add(a.buildContentValues());
        }
        for (User u : getUsers()) {
            users.add(u.buildContentValues());
        }
        for (Track t : getTracks()) {
            tracks.add(t.buildContentValues());
        }

        int inserted = 0;
        if (!tracks.isEmpty()) {
            inserted += resolver.bulkInsert(Content.TRACKS.uri, tracks.toArray(new ContentValues[tracks.size()]));
        }
        if (!users.isEmpty())  {
            inserted += resolver.bulkInsert(Content.USERS.uri, users.toArray(new ContentValues[users.size()]));
        }
        resolver.bulkInsert(Content.COLLECTION_ITEMS.uri, items.toArray(new ContentValues[items.size()]));
        return inserted;
    }
}
