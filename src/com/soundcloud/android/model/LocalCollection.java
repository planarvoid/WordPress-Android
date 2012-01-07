package com.soundcloud.android.model;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;

import com.soundcloud.android.provider.Content;
import com.soundcloud.android.provider.DBHelper;

public class LocalCollection {
    public int id;
    public Uri uri;
    public long last_sync;
    public int size;

     public LocalCollection(Cursor c){
         id = c.getInt(c.getColumnIndex(DBHelper.Collections.ID));
         uri = Uri.parse(c.getString(c.getColumnIndex(DBHelper.Collections.URI)));
         last_sync = c.getLong(c.getColumnIndex(DBHelper.Collections.LAST_SYNC));
         size = c.getInt(c.getColumnIndex(DBHelper.Collections.SIZE));
     }
    public LocalCollection(String id, Uri uri){
         this.id = Integer.parseInt(id);
         this.uri = uri;
     }

    public static LocalCollection fromContentUri(ContentResolver contentResolver, Uri contentUri){
        LocalCollection lc = null;
        Cursor c = contentResolver.query(Content.COLLECTIONS.uri, null, "uri = ?", new String[]{contentUri.toString()}, null);
        if (c != null && c.moveToFirst()) {
            lc = new LocalCollection(c);
        }
        if (c != null) c.close();
        return lc;
    }

    public static LocalCollection insertLocalCollection(ContentResolver contentResolver, Uri contentUri) {
        return insertLocalCollection(contentResolver,contentUri,-1,-1);
    }

    public static LocalCollection insertLocalCollection(ContentResolver contentResolver, Uri contentUri, long lastRefresh, int size) {
        // insert if not there
        ContentValues cv = new ContentValues();
        cv.put(DBHelper.Collections.URI, contentUri.toString());
        if (lastRefresh != -1) cv.put(DBHelper.Collections.LAST_SYNC, lastRefresh);
        if (size != -1) cv.put(DBHelper.Collections.SIZE, size);

        Uri inserted = contentResolver.insert(Content.COLLECTIONS.uri, cv);
        if (inserted != null) {
            return new LocalCollection(inserted.getLastPathSegment(),contentUri);
        } else {
            return null;
        }
    }

    public static long getLastSync(ContentResolver contentResolver, Uri contentUri) {
        long lastSync = -1;
        if (contentUri != null) {
            Cursor c = contentResolver.query(Content.COLLECTIONS.uri,
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

    public boolean updateLasySyncTime(ContentResolver contentResolver, long time) {
        ContentValues cv = new ContentValues();
        cv.put(DBHelper.Collections.LAST_SYNC, time);
        Uri inserted = contentResolver.insert(Content.COLLECTIONS.uri, cv);
        if (inserted != null) {
            return true;
        } else {
            return false;
        }
    }

    public static void deletePagesFrom(ContentResolver resolver, int collection_id, int page_index) {
        resolver.delete(Content.COLLECTION_PAGES.uri,
                DBHelper.CollectionPages.COLLECTION_ID + " = ? AND " + DBHelper.CollectionPages.PAGE_INDEX + " > ?",
                new String[]{String.valueOf(collection_id), String.valueOf(page_index)});

    }
}
