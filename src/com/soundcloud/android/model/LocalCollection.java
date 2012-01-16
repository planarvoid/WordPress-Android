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
    public final String sync_state;
    public final int size;

    public LocalCollection(Cursor c) {
        id = c.getInt(c.getColumnIndex(DBHelper.Collections._ID));
        uri = Uri.parse(c.getString(c.getColumnIndex(DBHelper.Collections.URI)));
        last_sync = c.getLong(c.getColumnIndex(DBHelper.Collections.LAST_SYNC));
        sync_state = c.getString(c.getColumnIndex(DBHelper.Collections.SYNC_STATE));
        size = c.getInt(c.getColumnIndex(DBHelper.Collections.SIZE));
    }

    public LocalCollection(int id, Uri uri, long lastSync, String syncState, int size) {
        this.id = id;
        this.uri = uri;
        this.last_sync = lastSync;
        this.sync_state = syncState;
        this.size = size;
    }

    public static LocalCollection fromContent(Content content, ContentResolver resolver) {
        return fromContentUri(content.uri, resolver);
    }

    public static LocalCollection fromContentUri(Uri contentUri, ContentResolver resolver) {
        LocalCollection lc = null;
        Cursor c = resolver.query(Content.COLLECTIONS.uri, null, "uri = ?", new String[]{contentUri.toString()}, null);
        if (c != null && c.moveToFirst()) {
            lc = new LocalCollection(c);
        }
        if (c != null) c.close();
        return lc;
    }

    public static LocalCollection insertLocalCollection(Uri contentUri, ContentResolver resolver) {
        return insertLocalCollection(contentUri, null, -1, -1, resolver);
    }

    public static LocalCollection insertLocalCollection(Uri contentUri, String syncState, long lastRefresh, int size, ContentResolver resolver) {
        // insert if not there
        ContentValues cv = new ContentValues();
        cv.put(DBHelper.Collections.URI, contentUri.toString());
        if (lastRefresh != -1) cv.put(DBHelper.Collections.LAST_SYNC, lastRefresh);
        if (size != -1)        cv.put(DBHelper.Collections.SIZE, size);
        cv.put(DBHelper.Collections.SYNC_STATE, syncState);

        Uri inserted = resolver.insert(Content.COLLECTIONS.uri, cv);
        if (inserted != null) {
            return new LocalCollection(Integer.parseInt(inserted.getLastPathSegment()),
                    contentUri, lastRefresh, syncState, size);
        } else {
            return null;
        }
    }

    public static long getLastSync(Uri contentUri, ContentResolver resolver) {
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

    public boolean updateLastSyncTime(long time, ContentResolver resolver) {
        ContentValues cv = buildContentValues();
        cv.put(DBHelper.Collections.LAST_SYNC, time);
        Uri inserted = resolver.insert(Content.COLLECTIONS.uri, cv);
        return inserted != null;
    }

    private ContentValues buildContentValues() {
        ContentValues cv = new ContentValues();
        cv.put(DBHelper.Collections._ID, id);
        if (last_sync != -1) cv.put(DBHelper.Collections.LAST_SYNC, last_sync);
        if (last_sync != -1) cv.put(DBHelper.Collections.SIZE, size);
        cv.put(DBHelper.Collections.URI, uri.toString());
        return cv;
    }

    public static void deletePagesFrom(ContentResolver resolver, int collection_id, int page_index) {
        resolver.delete(Content.COLLECTION_PAGES.uri,
                DBHelper.CollectionPages.COLLECTION_ID + " = ? AND " + DBHelper.CollectionPages.PAGE_INDEX + " > ?",
                new String[]{String.valueOf(collection_id), String.valueOf(page_index)});

    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        LocalCollection that = (LocalCollection) o;

        if (id != that.id) return false;
        if (last_sync != that.last_sync) return false;
        if (size != that.size) return false;
        if (sync_state != null ? !sync_state.equals(that.sync_state) : that.sync_state != null) return false;
        if (uri != null ? !uri.equals(that.uri) : that.uri != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = id;
        result = 31 * result + (uri != null ? uri.hashCode() : 0);
        result = 31 * result + (int) (last_sync ^ (last_sync >>> 32));
        result = 31 * result + (sync_state != null ? sync_state.hashCode() : 0);
        result = 31 * result + size;
        return result;
    }
}
