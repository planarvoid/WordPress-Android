package com.soundcloud.android.model;

import com.soundcloud.android.provider.BulkInsertMap;
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
            while (c.moveToNext()) {
                boolean found = false;
                final long id = c.getLong(0);
                final int resourceType = c.getInt(1);

                for (SoundAssociation a : this) {
                    if (a.getPlayable().id == id && a.getResourceType() == resourceType) {
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
            }
            c.close();

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

        List<ContentValues> items = new ArrayList<ContentValues>();
        BulkInsertMap map = new BulkInsertMap();

        for (SoundAssociation soundAssociation : this) {
            soundAssociation.putDependencyValues(map);
            items.add(soundAssociation.buildContentValues());
        }
        map.insert(resolver); // dependencies
        return resolver.bulkInsert(Content.COLLECTION_ITEMS.uri, items.toArray(new ContentValues[items.size()]));
    }
}
