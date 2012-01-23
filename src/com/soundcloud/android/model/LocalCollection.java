package com.soundcloud.android.model;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.database.ContentObserver;
import android.database.Cursor;
import android.net.Uri;

import android.os.Handler;
import android.text.TextUtils;
import android.util.Log;
import com.soundcloud.android.provider.Content;
import com.soundcloud.android.provider.DBHelper;
import com.soundcloud.android.service.sync.ApiSyncer;
import com.soundcloud.android.utils.CloudUtils;

/**
 * Represents the state of a local collection sync, including last sync and size.
 * See {@link DBHelper.Collections}.
 */
public class LocalCollection {
    public final int id;
    public final Uri uri;
    public long last_sync = -1;
    public int sync_state = -1;
    public int size = -1;
    public String extra;

    private ContentResolver mContentResolver;
    private ContentObserver mChangeObserver;

    public interface SyncState {
        int UNDEFINED = -1;
        int PENDING = 0;
        int SYNCING = 1;
        int IDLE = 2;
    }

    public LocalCollection(Cursor c) {
        id = c.getInt(c.getColumnIndex(DBHelper.Collections._ID));
        uri = Uri.parse(c.getString(c.getColumnIndex(DBHelper.Collections.URI)));
        setFromCursor(c);
    }

    public void setFromCursor(Cursor c) {
        last_sync = c.getLong(c.getColumnIndex(DBHelper.Collections.LAST_SYNC));
        sync_state = c.getInt(c.getColumnIndex(DBHelper.Collections.SYNC_STATE));
        extra = c.getString(c.getColumnIndex(DBHelper.Collections.EXTRA));
        size = c.getInt(c.getColumnIndex(DBHelper.Collections.SIZE));
    }

    public LocalCollection(int id, Uri uri, long lastSync, int syncState, int size, String extra) {
        this.id = id;
        this.uri = uri;
        this.last_sync = lastSync;
        this.sync_state = syncState;
        this.size = size;
        this.extra = extra;
    }

    public boolean onSyncComplete(ApiSyncer.Result result, ContentResolver resolver) {
        last_sync = result.synced_at;
        size = result.new_size;
        extra = result.extra;
        sync_state = SyncState.IDLE;

        return (resolver.update(Content.COLLECTIONS_ITEM.forId(id), buildContentValues(), null,null) == 1);
    }

    public static LocalCollection fromContent(Content content, ContentResolver resolver) {
        return fromContent(content, resolver, false);
    }

    public static LocalCollection fromContent(Content content, ContentResolver resolver, boolean createIfNecessary) {
        return fromContentUri(content.uri, resolver, createIfNecessary);
    }

    public static LocalCollection fromContentUri(Uri contentUri, ContentResolver resolver, boolean createIfNecessary) {
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

    public static LocalCollection insertLocalCollection(Uri contentUri, ContentResolver resolver) {
        return insertLocalCollection(contentUri, 0, -1, -1, null, resolver);
    }

    public static LocalCollection insertLocalCollection(Uri contentUri, int syncState, long lastRefresh, int size, String extra, ContentResolver resolver) {
        // insert if not there
        ContentValues cv = new ContentValues();
        cv.put(DBHelper.Collections.URI, contentUri.toString());
        if (lastRefresh != -1) cv.put(DBHelper.Collections.LAST_SYNC, lastRefresh);
        if (size != -1)        cv.put(DBHelper.Collections.SIZE, size);
        cv.put(DBHelper.Collections.SYNC_STATE, syncState);
        cv.put(DBHelper.Collections.EXTRA, extra);

        Uri inserted = resolver.insert(Content.COLLECTIONS.uri, cv);
        if (inserted != null) {
            return new LocalCollection(Integer.parseInt(inserted.getLastPathSegment()),
                    contentUri, lastRefresh, syncState, size, extra);
        } else {
            return null;
        }
    }

    public static boolean deleteUri(Uri contentUri, ContentResolver resolver) {
        return resolver.delete(Content.COLLECTIONS.uri,
                "uri = ?",
                new String[] { contentUri.toString() }) == 1;

    }

    public static long getLastSync(Uri contentUri, ContentResolver resolver) {
        LocalCollection lc = fromContentUri(contentUri, resolver, false);
        if (lc == null) {
            return -1;
        } else {
            return lc.last_sync;
        }
    }

    @Override
    public String toString() {
        return "LocalCollection{" +
                "id=" + id +
                ", uri=" + uri +
                ", last_sync=" + last_sync +
                ", sync_state='" + sync_state + '\'' +
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
        if (sync_state != -1) cv.put(DBHelper.Collections.SYNC_STATE, sync_state);
        if (size != -1) cv.put(DBHelper.Collections.SIZE, size);
        if (last_sync != -1) cv.put(DBHelper.Collections.LAST_SYNC, last_sync);
        if (!TextUtils.isEmpty(extra)) cv.put(DBHelper.Collections.EXTRA, extra);
        cv.put(DBHelper.Collections.URI, uri.toString());
        return cv;
    }

    public int getSyncStateByUri(Uri uri, ContentResolver resolver) {
        Cursor c = resolver.query(Content.COLLECTIONS.uri,new String[]{DBHelper.Collections.SYNC_STATE},
                DBHelper.Collections.URI + " = ?", new String[]{String.valueOf(uri)}, null);
        if (c != null){
            if (c.moveToFirst()){
                return c.getInt(0);
            }
            c.close();
        }
        return -1;
    }


    public boolean updateSyncState(int newSyncState, ContentResolver resolver) {
        ContentValues cv = new ContentValues();
        cv.put(DBHelper.Collections.SYNC_STATE, newSyncState);
        return (resolver.update(Content.COLLECTIONS_ITEM.forId(id), cv, null,null) == 1);
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


    public static void deletePagesFrom(ContentResolver resolver, int collection_id, int page_index) {
        resolver.delete(Content.COLLECTION_PAGES.uri,
                DBHelper.CollectionPages.COLLECTION_ID + " = ? AND " + DBHelper.CollectionPages.PAGE_INDEX + " > ?",
                new String[]{String.valueOf(collection_id), String.valueOf(page_index)});

    }

    public void startObservingSelf(ContentResolver contentResolver) {
        mContentResolver = contentResolver;
        mChangeObserver = new ChangeObserver();
        contentResolver.registerContentObserver(Content.COLLECTIONS.uri.buildUpon().appendPath(String.valueOf(id)).build(), true, mChangeObserver);
    }
    public void stopObservingSelf() {
        if (mChangeObserver != null) mContentResolver.unregisterContentObserver(mChangeObserver);
    }

    private class ChangeObserver extends ContentObserver {
        public ChangeObserver() {
            super(new Handler());
        }

        @Override
        public boolean deliverSelfNotifications() {
            return true;
        }

        @Override
        public void onChange(boolean selfChange) {
            LocalCollection lc = null;
            Cursor c = mContentResolver.query(Content.COLLECTIONS.uri, null, "_id = ?", new String[]{String.valueOf(id)}, null);
            if (c != null && c.moveToFirst()) {
                setFromCursor(c);
            }
            if (c != null) c.close();
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        LocalCollection that = (LocalCollection) o;

        if (id != that.id) return false;
        if (last_sync != that.last_sync) return false;
        if (size != that.size) return false;
        if (sync_state != that.sync_state) return false;
        if (extra != null ? !extra.equals(that.extra) : that.extra != null) return false;
        if (uri != null ? !uri.equals(that.uri) : that.uri != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = id;
        result = 31 * result + (uri != null ? uri.hashCode() : 0);
        result = 31 * result + (int) (last_sync ^ (last_sync >>> 32));
        result = 31 * result + size;
        result = 31 * result + sync_state;
        result = 31 * result + (extra != null ? extra.hashCode() : 0);
        return result;
    }
}
