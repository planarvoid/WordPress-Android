package com.soundcloud.android.provider;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.net.Uri;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * For organizing and inserting content values associated with Uris for the bulk insert
 * of dependencies. Used in {@link com.soundcloud.android.model.ScResource#getDependencyValuesMap()}
 */
public class BulkInsertMap extends HashMap<Uri, List<ContentValues>> {

    public void add(Uri uri, ContentValues values){
        if (!containsKey(uri)){
            put(uri,new ArrayList<ContentValues>());
        }
        get(uri).add(values);
    }

    public void merge(BulkInsertMap old) {
        for (Uri uri : old.keySet()){
            if (!containsKey(uri)) {
                put(uri, old.get(uri));
            } else {
                get(uri).addAll(old.get(uri));
            }
        }
    }

    public int insert(ContentResolver resolver) {
        int inserted = 0;
        for (Map.Entry<Uri, List<ContentValues>> entry : entrySet()) {
            final List<ContentValues> contentValuesList = entry.getValue();
            inserted += resolver.bulkInsert(entry.getKey(), contentValuesList.toArray(new ContentValues[contentValuesList.size()]));
        }
        return inserted;
    }
}
