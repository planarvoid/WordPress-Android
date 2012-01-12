package com.soundcloud.android.model;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;

import com.soundcloud.android.provider.Content;
import com.soundcloud.android.provider.DBHelper;

/**
 * Represents the state of a local collection sync, including last sync and size.
 * See {@link DBHelper.Collections}.
 */
public class LocalCollection {
    public final int id;
    public final Uri uri;
    public final long last_sync;
    public final int size;

    public LocalCollection(Cursor c) {
        id = c.getInt(c.getColumnIndex(DBHelper.Collections.ID));
        uri = Uri.parse(c.getString(c.getColumnIndex(DBHelper.Collections.URI)));
        last_sync = c.getLong(c.getColumnIndex(DBHelper.Collections.LAST_SYNC));
        size = c.getInt(c.getColumnIndex(DBHelper.Collections.SIZE));
    }

    public LocalCollection(int id, Uri uri, long lastSync, int size) {
        this.id = id;
        this.uri = uri;
        this.last_sync = lastSync;
        this.size = size;
    }

    public static LocalCollection fromContentUri(ContentResolver resolver, Uri contentUri) {
        LocalCollection lc = null;
        Cursor c = resolver.query(Content.COLLECTIONS.uri, null, "uri = ?", new String[]{contentUri.toString()}, null);
        if (c != null && c.moveToFirst()) {
            lc = new LocalCollection(c);
        }
        if (c != null) c.close();
        return lc;
    }

    public static LocalCollection insertLocalCollection(ContentResolver resolver, Uri contentUri) {
        return insertLocalCollection(resolver, contentUri, -1, -1);
    }

    public static LocalCollection insertLocalCollection(ContentResolver resolver, Uri contentUri, long lastRefresh, int size) {
        // insert if not there
        ContentValues cv = new ContentValues();
        cv.put(DBHelper.Collections.URI, contentUri.toString());
        if (lastRefresh != -1) cv.put(DBHelper.Collections.LAST_SYNC, lastRefresh);
        if (size != -1)        cv.put(DBHelper.Collections.SIZE, size);

        Uri inserted = resolver.insert(Content.COLLECTIONS.uri, cv);
        if (inserted != null) {
            return new LocalCollection(Integer.parseInt(inserted.getLastPathSegment()),
                    contentUri, lastRefresh, size);
        } else {
            return null;
        }
    }

    public static long getLastSync(ContentResolver resolver, Uri contentUri) {
        long lastSync = -1;
        if (contentUri != null) {
            Cursor c = resolver.query(Content.COLLECTIONS.uri,
                    new String[]{DBHelper.Collections.LAST_SYNC}, "uri = ?", new String[]{contentUri.toString()}, null);
            if (c != null && c.moveToFirst()) {
                lastSync = c.getLong(c.getColumnIndex(DBHelper.Collections.LAST_SYNC));
            }
            if (c != null) c.close();
        }
        return lastSync;
    }

    @Override
    public String toString() {
        return "LocalCollection{" +
                "id=" + id +
                ", uri=" + uri +
                ", last_sync=" + last_sync +
                ", size=" + size +
                '}';
    }

    public boolean updateLastSyncTime(ContentResolver resolver, long time) {
        ContentValues cv = toContentValues();
        cv.put(DBHelper.Collections.LAST_SYNC, time);
        Uri inserted = resolver.insert(Content.COLLECTIONS.uri, cv);
        return inserted != null;
    }

    private ContentValues toContentValues() {
        ContentValues cv = new ContentValues();
        cv.put(DBHelper.Collections.ID, id);
        cv.put(DBHelper.Collections.LAST_SYNC, last_sync);
        cv.put(DBHelper.Collections.URI, uri.toString());
        cv.put(DBHelper.Collections.SIZE, size);
        return cv;
    }

    public static void deletePagesFrom(ContentResolver resolver, int collection_id, int page_index) {
        resolver.delete(Content.COLLECTION_PAGES.uri,
                DBHelper.CollectionPages.COLLECTION_ID + " = ? AND " + DBHelper.CollectionPages.PAGE_INDEX + " > ?",
                new String[]{String.valueOf(collection_id), String.valueOf(page_index)});

    }
}
