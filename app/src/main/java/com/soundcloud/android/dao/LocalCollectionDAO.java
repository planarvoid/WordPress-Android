package com.soundcloud.android.dao;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;
import com.soundcloud.android.model.LocalCollection;
import com.soundcloud.android.provider.Content;
import com.soundcloud.android.provider.DBHelper;
import org.jetbrains.annotations.Nullable;

public class LocalCollectionDAO {
    public static @Nullable
    LocalCollection insertLocalCollection(Uri contentUri, int syncState, long lastSyncAttempt, long lastSyncSuccess, int size, String extra, ContentResolver resolver) {
        // insert if not there
        ContentValues cv = new ContentValues();
        cv.put(DBHelper.Collections.URI, contentUri.toString());
        if (lastSyncAttempt != -1) cv.put(DBHelper.Collections.LAST_SYNC_ATTEMPT, lastSyncAttempt);
        if (lastSyncSuccess != -1) cv.put(DBHelper.Collections.LAST_SYNC, lastSyncSuccess);
        if (size != -1)        cv.put(DBHelper.Collections.SIZE, size);
        cv.put(DBHelper.Collections.SYNC_STATE, syncState);
        cv.put(DBHelper.Collections.EXTRA, extra);

        Uri inserted = resolver.insert(Content.COLLECTIONS.uri, cv);
        if (inserted != null) {
            return new LocalCollection(Integer.parseInt(inserted.getLastPathSegment()),
                    contentUri, lastSyncAttempt,lastSyncSuccess, syncState, size, extra);
        } else {
            // TODO: should throw an exception here
            return null;
        }
    }

    public static @Nullable LocalCollection fromContent(Content content, ContentResolver resolver, boolean createIfNecessary) {
        return fromContentUri(content.uri, resolver, createIfNecessary);
    }

    public static @Nullable LocalCollection fromContentUri(Uri contentUri, ContentResolver resolver, boolean createIfNecessary) {
        LocalCollection lc = null;
        Cursor c = resolver.query(Content.COLLECTIONS.uri, null, "uri = ?", new String[]{contentUri.toString()}, null);
        if (c != null && c.moveToFirst()) {
            lc = new LocalCollection(c);
        }
        if (c != null) c.close();

        if (lc == null && createIfNecessary){
            lc = insertLocalCollection(contentUri,resolver);
        }

        return lc;
    }

    public static @Nullable LocalCollection insertLocalCollection(Uri contentUri, ContentResolver resolver) {
        return insertLocalCollection(contentUri, 0, -1, -1, -1, null, resolver);
    }

    public static boolean deleteUri(Uri contentUri, ContentResolver resolver) {
        return resolver.delete(Content.COLLECTIONS.uri,
                "uri = ?",
                new String[] { contentUri.toString() }) == 1;

    }

    public static long getLastSyncAttempt(Uri contentUri, ContentResolver resolver) {
        LocalCollection lc = fromContentUri(contentUri, resolver, false);
        if (lc == null) {
            return -1;
        } else {
            return lc.last_sync_attempt;
        }
    }

    public static long getLastSyncSuccess(Uri contentUri, ContentResolver resolver) {
            LocalCollection lc = fromContentUri(contentUri, resolver, false);
            if (lc == null) {
                return -1;
            } else {
                return lc.last_sync_success;
            }
        }

    public static String getExtraFromUri(Uri contentUri, ContentResolver resolver) {
        String extra = null;
        Cursor c = resolver.query(Content.COLLECTIONS.uri, new String[]{DBHelper.Collections.EXTRA}, "uri = ?", new String[]{contentUri.toString()}, null);
        if (c != null && c.moveToFirst()) {
            extra = c.getString(0);
        }
        if (c != null) c.close();
        return extra;
    }

    public static int incrementSyncMiss(Uri contentUri, ContentResolver resolver) {
        int id = -1;
        int misses = 0;
        Cursor c = resolver.query(Content.COLLECTIONS.uri, new String[]{DBHelper.Collections._ID, DBHelper.Collections.EXTRA}, "uri = ?", new String[]{contentUri.toString()}, null);
        if (c != null && c.moveToFirst()) {
            id = c.getInt(0);
            try {
                misses = Integer.parseInt(c.getString(1));
            } catch (NumberFormatException ignore){}
        }
        if (c != null) c.close();

        ContentValues cv = new ContentValues();
        cv.put(DBHelper.Collections.EXTRA, ++misses);
        return (resolver.update(Content.COLLECTIONS.forId(id), cv, null, null) == 1) ? misses : -1;
    }

    public static boolean forceToStale(Uri uri, ContentResolver resolver) {
        LocalCollection lc = fromContentUri(uri, resolver, true);
        ContentValues cv = lc.buildContentValues();
        cv.put(DBHelper.Collections.LAST_SYNC, 0);
        cv.put(DBHelper.Collections.LAST_SYNC_ATTEMPT, 0);
        return resolver.update(Content.COLLECTIONS.uri, cv, "uri = ?", new String[]{uri.toString()}) == 1;
    }
}
