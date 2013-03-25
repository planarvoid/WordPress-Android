package com.soundcloud.android.dao;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;
import com.soundcloud.android.model.CollectionHolder;
import com.soundcloud.android.model.ScResource;
import com.soundcloud.android.model.SoundAssociation;
import com.soundcloud.android.provider.BulkInsertMap;
import com.soundcloud.android.provider.Content;
import com.soundcloud.android.provider.DBHelper;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SoundAssociationDAO extends BaseDAO<SoundAssociation> {
    public SoundAssociationDAO(ContentResolver contentResolver) {
        super(contentResolver);
    }

    @Override
    public List<SoundAssociation> queryAll() {
        List<SoundAssociation> result = new ArrayList<SoundAssociation>();
        result.addAll(queryAllByUri(Content.ME_LIKES.uri)); // liked tracks & playlists
        result.addAll(queryAllByUri(Content.ME_SOUNDS.uri)); // own tracks, own playlists, or reposts
        return result;
    }

    /**
     * Sync this collection to the local database by removing any stale items and
     * inserting the sound associations (which will replace the existing items)
     * @param soundAssociations
     * @param contentUri
     * @return whether any items were added or removed
     */
    public boolean syncToLocal(List<SoundAssociation> soundAssociations, Uri contentUri) {
        // get current local id and types for this uri
        Cursor c = mResolver.query(contentUri,
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
                        mResolver.delete(contentUri,
                                DBHelper.CollectionItems.ITEM_ID + " = ? AND " + DBHelper.CollectionItems.RESOURCE_TYPE + " = ?",
                                new String[]{String.valueOf(id), String.valueOf(type)});
                    }
                }
            }
        }

        insert(getContent().uri, soundAssociations);
        return changed;
    }


    public int insert(List<? extends ScResource> resources) {
        return insert(getContent().uri, resources);
    }

    public int insert(Uri uri, List<? extends ScResource> resources) {
        List<ContentValues> items = new ArrayList<ContentValues>();
        BulkInsertMap map = new BulkInsertMap();

        for (ScResource resource : resources) {
            resource.putDependencyValues(map);
            items.add(resource.buildContentValues());
        }
        map.insert(mResolver); // dependencies
        return mResolver.bulkInsert(uri, items.toArray(new ContentValues[items.size()]));
    }

    public int writeCollection(CollectionHolder<? extends ScResource> models) {
        return insert(getContent().uri, models.collection);
    }

    @Override
    public Content getContent() {
        return Content.COLLECTION_ITEMS;
    }

    @NotNull
    @Override
    public Class<SoundAssociation> getModelClass() {
        return SoundAssociation.class;
    }
}
