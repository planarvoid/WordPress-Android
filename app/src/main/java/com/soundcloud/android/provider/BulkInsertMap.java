package com.soundcloud.android.provider;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.net.Uri;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * For organizing and inserting content values associated with Uris for the bulk insert
 * of dependencies. Used in {@link com.soundcloud.android.model.ScResource#putDependencyValues(BulkInsertMap)}
 */
public class BulkInsertMap extends HashMap<Uri, Set<BulkInsertMap.ResourceValues>> {

    public void add(Uri uri, ContentValues values){
        if (!containsKey(uri)){
            put(uri, new HashSet<ResourceValues>());
        }
        get(uri).add(new ResourceValues(values));
    }

    public int insert(ContentResolver resolver) {
        int inserted = 0;
        for (Map.Entry<Uri, Set<ResourceValues>> entry : entrySet()) {
            ContentValues[] toInsert = new ContentValues[entry.getValue().size()];
            int i = 0;
            for (ResourceValues resourceValues : entry.getValue()) {
                toInsert[i] = resourceValues.contentValues;
                i++;
            }
            inserted += resolver.bulkInsert(entry.getKey(), toInsert);
        }
        return inserted;
    }

    public class ResourceValues {
        ContentValues contentValues;
        private ResourceValues(ContentValues contentValues) {
            this.contentValues = contentValues;
        }

        public ContentValues getContentValues() {
            return contentValues;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            ResourceValues that = (ResourceValues) o;

            if (contentValues == null ? that.contentValues != null :
                    (!contentValues.containsKey(DBHelper.ResourceTable._ID)
                            || !that.contentValues.containsKey(DBHelper.ResourceTable._ID)
                            || contentValues.get(DBHelper.ResourceTable._ID) != that.contentValues.get(DBHelper.ResourceTable._ID)
                    )) {
                return false;
            }

            return true;
        }

        @Override
        public int hashCode() {
            if (contentValues != null){
                if (contentValues.containsKey(DBHelper.ResourceTable._ID)){
                    return contentValues.getAsLong(DBHelper.ResourceTable._ID).hashCode();
                }
                return contentValues.hashCode();
            }
            return 0;
        }
    }
}
