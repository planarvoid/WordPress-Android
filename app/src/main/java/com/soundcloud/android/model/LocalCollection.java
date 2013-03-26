package com.soundcloud.android.model;

import com.soundcloud.android.dao.ContentValuesProvider;
import com.soundcloud.android.model.act.Activity;
import com.soundcloud.android.provider.BulkInsertMap;
import com.soundcloud.android.provider.Content;
import com.soundcloud.android.provider.DBHelper;
import com.soundcloud.android.service.sync.ApiSyncer;
import com.soundcloud.android.service.sync.SyncConfig;

import android.content.AsyncQueryHandler;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.database.ContentObserver;
import android.database.Cursor;
import android.net.Uri;
import android.os.Handler;
import android.text.TextUtils;
import org.jetbrains.annotations.NotNull;

/**
 * Represents the state of a local collection sync, including last sync and size.
 * See {@link DBHelper.Collections}.
 */
public class LocalCollection implements ModelLike, ContentValuesProvider {
    public long id;
    public final Uri uri;

    /** timestamp of last successful sync */
    public long last_sync_success = -1;
    /** timestamp of last sync attempt */
    public long last_sync_attempt = -1;
    /** see {@link SyncState}, for display/UI purposes ({@link com.soundcloud.android.fragment.ScListFragment}) */
    public int sync_state = -1;
    /** collection size */
    public int size = -1;
    /** collection specific data - future_href for activities, sync misses for rest */
    public String extra;

    private ContentResolver mContentResolver;
    private ContentObserver mChangeObserver;

    private OnChangeListener mChangeListener;


    public boolean hasSyncedBefore() {
        return last_sync_success > 0;
    }

    public interface OnChangeListener {
        void onLocalCollectionChanged();
    }

    public interface SyncState {
        int IDLE    = 0;
        int PENDING = 1;
        int SYNCING = 2;
    }

    public int syncMisses() {
        try {
            return Integer.parseInt(extra);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    public LocalCollection(Cursor c) {
        id = c.getInt(c.getColumnIndex(DBHelper.Collections._ID));
        uri = Uri.parse(c.getString(c.getColumnIndex(DBHelper.Collections.URI)));
        setFromCursor(c);
    }

    public void setFromCursor(Cursor c) {
        last_sync_attempt = c.getLong(c.getColumnIndex(DBHelper.Collections.LAST_SYNC_ATTEMPT));
        last_sync_success = c.getLong(c.getColumnIndex(DBHelper.Collections.LAST_SYNC));
        sync_state = c.getInt(c.getColumnIndex(DBHelper.Collections.SYNC_STATE));
        extra = c.getString(c.getColumnIndex(DBHelper.Collections.EXTRA));
        size = c.getInt(c.getColumnIndex(DBHelper.Collections.SIZE));
    }

    public LocalCollection(Uri uri, long lastSyncAttempt, long lastSyncSuccess, int syncState, int size, String extra) {
        this.uri = uri;
        this.last_sync_attempt = lastSyncAttempt;
        this.last_sync_success = lastSyncSuccess;
        this.sync_state = syncState;
        this.size = size;
        this.extra = extra;
    }

    /**
     * Creates a "blank" collection which is in {@link SyncState.IDLE} state
     */
    public LocalCollection(Uri uri) {
        this(uri, -1, -1, SyncState.IDLE, 0, null);
    }

    public boolean isIdle(){
        return sync_state == SyncState.IDLE;
    }


    @Override
    public String toString() {
        return "LocalCollection{" +
                "id=" + id +
                ", uri=" + uri +
                ", last_sync_attempt=" + last_sync_attempt +
                ", last_sync_success=" + last_sync_success +
                ", sync_state='" + sync_state + '\'' +
                ", extra=" + extra +
                ", size=" + size +
                '}';
    }


    public ContentValues buildContentValues() {
        ContentValues cv = new ContentValues();
        if (id > 0) cv.put(DBHelper.Collections._ID, id);
        if (sync_state != -1) cv.put(DBHelper.Collections.SYNC_STATE, sync_state);
        if (size != -1) cv.put(DBHelper.Collections.SIZE, size);
        if (last_sync_attempt != -1) cv.put(DBHelper.Collections.LAST_SYNC_ATTEMPT, last_sync_attempt);
        if (last_sync_success != -1) cv.put(DBHelper.Collections.LAST_SYNC, last_sync_success);
        if (!TextUtils.isEmpty(extra)) cv.put(DBHelper.Collections.EXTRA, extra);
        cv.put(DBHelper.Collections.URI, uri.toString());
        return cv;
    }

    @Override
    public void putFullContentValues(@NotNull BulkInsertMap destination) {
    }

    @Override
    public void putDependencyValues(@NotNull BulkInsertMap destination) {
    }

    public boolean shouldAutoRefresh() {
        if (!isIdle()) return false;
        Content c = Content.match(uri);

        // only auto refresh once every 30 mins at most, that we won't hammer their phone or the api if there are errors
        if (c == null || last_sync_attempt > System.currentTimeMillis() - SyncConfig.DEFAULT_ATTEMPT_DELAY) return false;

        // do not auto refresh users when the list opens, because users are always changing
        if (User.class.equals(c.modelType)) return last_sync_success <= 0;

        final long staleTime = (Track.class.equals(c.modelType))    ? SyncConfig.TRACK_STALE_TIME :
                               (Playlist.class.equals(c.modelType)) ? SyncConfig.PLAYLIST_STALE_TIME :
                               (Activity.class.equals(c.modelType)) ? SyncConfig.ACTIVITY_STALE_TIME :
                               SyncConfig.DEFAULT_STALE_TIME;

        return System.currentTimeMillis() - last_sync_success > staleTime;
    }

    @Deprecated
    public void startObservingSelf(ContentResolver contentResolver, OnChangeListener listener) {
        mContentResolver = contentResolver;
        mChangeObserver = new ChangeObserver();
        contentResolver.registerContentObserver(toUri(), true, mChangeObserver);
        mChangeListener = listener;
    }

    @Override
    public long getId() {
        return id;
    }

    @Override
    public void setId(long id) {
        this.id = id;
    }

    public Uri toUri() {
        return Content.COLLECTIONS.uri.buildUpon().appendPath(String.valueOf(id)).build();
    }

    public void stopObservingSelf() {
        if (mChangeObserver != null) mContentResolver.unregisterContentObserver(mChangeObserver);
        mChangeListener = null;
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
            LocalCollectionQueryHandler handler = new LocalCollectionQueryHandler(mContentResolver);
            handler.startQuery(0, null, Content.COLLECTIONS.uri, null, "_id = ?", new String[]{String.valueOf(id)}, null);
        }
    }

    private class LocalCollectionQueryHandler extends AsyncQueryHandler {
        public LocalCollectionQueryHandler(ContentResolver resolver) {
            super(resolver);
        }

        @Override
        protected void onQueryComplete(int token, Object cookie, Cursor cursor) {
            if (cursor != null && cursor.moveToFirst()) {
                setFromCursor(cursor);
            }
            if (cursor != null) cursor.close();
            if (mChangeListener != null) mChangeListener.onLocalCollectionChanged();
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        LocalCollection that = (LocalCollection) o;

        if (id != that.id) return false;
        if (last_sync_attempt != that.last_sync_attempt) return false;
        if (last_sync_success != that.last_sync_success) return false;
        if (size != that.size) return false;
        if (sync_state != that.sync_state) return false;
        if (extra != null ? !extra.equals(that.extra) : that.extra != null) return false;
        //noinspection RedundantIfStatement
        if (uri != null ? !uri.equals(that.uri) : that.uri != null) return false;
        return true;
    }

    @Override
    public int hashCode() {
        int result = (int) (id ^ (id >>> 32));
        result = 31 * result + (uri != null ? uri.hashCode() : 0);
        result = 31 * result + (int) (last_sync_attempt ^ (last_sync_attempt >>> 32));
        result = 31 * result + (int) (last_sync_success ^ (last_sync_success >>> 32));
        result = 31 * result + size;
        result = 31 * result + sync_state;
        result = 31 * result + (extra != null ? extra.hashCode() : 0);
        return result;
    }
}
