package com.soundcloud.android.storage.provider;

import com.soundcloud.android.storage.TableColumns;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.net.Uri;

import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/**
 * For organizing and inserting content values associated with Uris for the bulk create
 * of dependencies. Used in {@link com.soundcloud.android.api.legacy.model.PublicApiResource#putDependencyValues(BulkInsertMap)}
 */
public class BulkInsertMap extends HashMap<Uri, Set<BulkInsertMap.ResourceValues>> {

    public void add(Uri uri, ContentValues values) {
        if (!containsKey(uri)) {
            put(uri, new LinkedHashSet<ResourceValues>());
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

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }

            ResourceValues that = (ResourceValues) o;

            return !(contentValues == null ? that.contentValues != null :
                    (!contentValues.containsKey(TableColumns.ResourceTable._ID)
                            || !that.contentValues.containsKey(TableColumns.ResourceTable._ID)
                            || contentValues.get(TableColumns.ResourceTable._ID) != that.contentValues.get(TableColumns.ResourceTable._ID)
                    ));

        }

        @Override
        public int hashCode() {
            if (contentValues != null) {
                if (contentValues.containsKey(TableColumns.ResourceTable._ID)) {
                    return contentValues.getAsLong(TableColumns.ResourceTable._ID).hashCode();
                }
                return contentValues.hashCode();
            }
            return 0;
        }
    }
}
