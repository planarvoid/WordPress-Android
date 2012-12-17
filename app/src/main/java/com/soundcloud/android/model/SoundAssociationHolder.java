package com.soundcloud.android.model;

import com.soundcloud.android.provider.Content;
import com.soundcloud.android.provider.DBHelper;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
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

    /**
     * Delete any items from the content resolver that do not appear in this collection, for syncing
     * TODO PLAYLISTS. This will not pull playlists, therefore they will not get deleted (yet)
     */
    public int removeMissingLocallyStoredItems(ContentResolver resolver, Uri contentUri) {
        // get current local id and types for this uri
        Cursor c = resolver.query(contentUri,
                new String[]{DBHelper.CollectionItems.ITEM_ID, DBHelper.CollectionItems.RESOURCE_TYPE},
                null, null, null);

        int deleted = 0;
        if (c != null) {
            Map<Integer, ArrayList<Long>> deletions = new HashMap<Integer, ArrayList<Long>>();
            if (c.moveToFirst()) {
                do {
                    boolean found = false;
                    final long id = c.getLong(0);
                    final int resourceType = c.getInt(1);

                    for (SoundAssociation a : this) {
                        if (a.getSound().id == id && a.getResourceType() == resourceType) {
                            found = true;
                            break;
                        }
                    }
                    if (!found) {
                        // this item no longer exists
                        if (!deletions.containsKey(resourceType)) {
                            deletions.put(resourceType, new ArrayList<Long>());
                        }
                        deletions.get(resourceType).add(id);
                    }
                } while (c.moveToNext());
            }

            for (Integer type : deletions.keySet()) {
                for (Long id : deletions.get(type)) {
                    deleted += resolver.delete(contentUri,
                            DBHelper.CollectionItems.ITEM_ID + " = ? AND " + DBHelper.CollectionItems.RESOURCE_TYPE + " = ?",
                            new String[]{String.valueOf(id), String.valueOf(type)});
                }
            }
        }

        return deleted;
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
