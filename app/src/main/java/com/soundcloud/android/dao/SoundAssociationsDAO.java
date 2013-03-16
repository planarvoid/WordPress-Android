package com.soundcloud.android.dao;

import com.soundcloud.android.model.CollectionHolder;
import com.soundcloud.android.model.SoundAssociation;
import com.soundcloud.android.model.SoundAssociationHolder;
import com.soundcloud.android.provider.BulkInsertMap;
import com.soundcloud.android.provider.Content;
import com.soundcloud.android.provider.DBHelper;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SoundAssociationsDAO {
    /**
     * Sync this collection to the local database by removing any stale items and
     * inserting the sound associations (which will replace the existing items)
     * @param soundAssociations
     * @param resolver
     * @param contentUri
     * @return whether any items were added or removed
     */
    public static boolean syncToLocal(SoundAssociationHolder soundAssociations, ContentResolver resolver, Uri contentUri) {
        // get current local id and types for this uri
        Cursor c = resolver.query(contentUri,
                new String[]{DBHelper.CollectionItems.ITEM_ID, DBHelper.CollectionItems.RESOURCE_TYPE,
                        DBHelper.CollectionItems.COLLECTION_TYPE}, null, null, null);

        boolean changed = true; // assume changed by default
        if (c != null) {
            final int localCount = c.getCount();
            Map<Integer, ArrayList<Long>> deletions = new HashMap<Integer, ArrayList<Long>>();
            while (c.moveToNext()) {
                boolean found = false;
                final long id = c.getLong(0);
                final int resourceType = c.getInt(1);
                final int associationType = c.getInt(2);

                for (SoundAssociation a : soundAssociations) {
                    if (a.getPlayable().id == id && a.getResourceType() == resourceType
                            && a.associationType == associationType) {
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

            if (deletions.isEmpty()) {
                // user hasn't removed anything, and if size is consistent we can assume the collection hasn't changed
                changed = localCount != soundAssociations.size();
            } else {
                for (Integer type : deletions.keySet()) {
                    for (Long id : deletions.get(type)) {
                        resolver.delete(contentUri,
                                DBHelper.CollectionItems.ITEM_ID + " = ? AND " + DBHelper.CollectionItems.RESOURCE_TYPE + " = ?",
                                new String[]{String.valueOf(id), String.valueOf(type)});
                    }
                }
            }
        }

        insert(soundAssociations, resolver);
        return changed;
    }

    public static int insert(SoundAssociationHolder soundAssociations, ContentResolver resolver) {

        List<ContentValues> items = new ArrayList<ContentValues>();
        BulkInsertMap map = new BulkInsertMap();

        for (SoundAssociation soundAssociation : soundAssociations) {
            soundAssociation.putDependencyValues(map);
            items.add(soundAssociation.buildContentValues());
        }
        map.insert(resolver); // dependencies
        return resolver.bulkInsert(Content.COLLECTION_ITEMS.uri, items.toArray(new ContentValues[items.size()]));
    }
}
