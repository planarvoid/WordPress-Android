package com.soundcloud.android.dao;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.net.Uri;
import com.soundcloud.android.model.ScResource;
import com.soundcloud.android.provider.BulkInsertMap;
import com.soundcloud.android.provider.Content;
import com.soundcloud.android.provider.DBHelper;
import org.jetbrains.annotations.NotNull;

import java.util.List;

import static com.soundcloud.android.dao.ResolverHelper.addPagingParams;
import static com.soundcloud.android.dao.ResolverHelper.idCursorToList;

public class CollectionStorage {

    private final ContentResolver mResolver;

    public CollectionStorage(ContentResolver resolver) {
        mResolver = resolver;
    }

    public int insertCollection(@NotNull List<? extends ScResource> resources,
                                @NotNull Uri collectionUri,
                                long ownerId) {
        if (ownerId < 0) throw new IllegalArgumentException("need valid ownerId for collection");

        BulkInsertMap map = new BulkInsertMap();
        for (int i = 0; i < resources.size(); i++) {
            ScResource r = resources.get(i);
            if (r == null) continue;

            r.putFullContentValues(map);
            ContentValues contentValues = new ContentValues();

            contentValues.put(DBHelper.CollectionItems.POSITION, i);
            contentValues.put(DBHelper.CollectionItems.ITEM_ID, r.id);
            contentValues.put(DBHelper.CollectionItems.USER_ID, ownerId);
            map.add(collectionUri, contentValues);
        }
        return map.insert(mResolver);
    }

    public List<Long> getLocalIds(Content content, long userId, int startIndex, int limit) {
        return idCursorToList(mResolver.query(
                addPagingParams(Content.COLLECTION_ITEMS.uri, startIndex, limit).build(),
                new String[]{ DBHelper.CollectionItems.ITEM_ID },
                DBHelper.CollectionItems.COLLECTION_TYPE + " = ? AND " + DBHelper.CollectionItems.USER_ID + " = ?",
                new String[]{ String.valueOf(content.collectionType), String.valueOf(userId) },
                DBHelper.CollectionItems.SORT_ORDER
            )
        );
    }
}
