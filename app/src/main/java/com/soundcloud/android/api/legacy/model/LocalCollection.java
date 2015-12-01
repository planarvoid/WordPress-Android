package com.soundcloud.android.api.legacy.model;

import com.soundcloud.android.api.legacy.model.behavior.Identifiable;
import com.soundcloud.android.api.legacy.model.behavior.Persisted;
import com.soundcloud.android.storage.TableColumns;
import com.soundcloud.android.storage.provider.BulkInsertMap;
import com.soundcloud.android.storage.provider.Content;
import org.jetbrains.annotations.NotNull;

import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;
import android.text.TextUtils;

/**
 * Represents the state of a local collection sync, including last sync and size.
 * See {@link com.soundcloud.android.storage.TableColumns.Collections}.
 */
public class LocalCollection implements Identifiable, Persisted {
    private long id;
    private final Uri uri;

    /**
     * timestamp of last successful sync
     */
    public long last_sync_success = -1;
    /**
     * timestamp of last sync attempt
     */
    public long last_sync_attempt = -1;
    /**
     * see {@link SyncState}, for display/UI purposes ({@link com.soundcloud.android.collections.ScListFragment})
     */
    public int sync_state = -1;
    /**
     * collection size
     */
    public int size = -1;
    /**
     * collection specific data - future_href for activities, sync misses for rest
     */
    public String extra;

    public interface OnChangeListener {
        void onLocalCollectionChanged(LocalCollection localCollection);
    }

    public interface SyncState {
        int IDLE = 0;
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
        id = (long) c.getInt(c.getColumnIndex(TableColumns.Collections._ID));
        uri = Uri.parse(c.getString(c.getColumnIndex(TableColumns.Collections.URI)));
        last_sync_attempt = c.getLong(c.getColumnIndex(TableColumns.Collections.LAST_SYNC_ATTEMPT));
        last_sync_success = c.getLong(c.getColumnIndex(TableColumns.Collections.LAST_SYNC));
        sync_state = c.getInt(c.getColumnIndex(TableColumns.Collections.SYNC_STATE));
        extra = c.getString(c.getColumnIndex(TableColumns.Collections.EXTRA));
        size = c.getInt(c.getColumnIndex(TableColumns.Collections.SIZE));
    }

    public boolean hasSyncedBefore() {
        return last_sync_success > 0;
    }

    public Uri getUri() {
        return uri;
    }

    public void setFromCursor(Cursor c) {
        if (getId() <= 0) {
            setId(c.getInt(c.getColumnIndex(TableColumns.Collections._ID)));
        }
        last_sync_attempt = c.getLong(c.getColumnIndex(TableColumns.Collections.LAST_SYNC_ATTEMPT));
        last_sync_success = c.getLong(c.getColumnIndex(TableColumns.Collections.LAST_SYNC));
        sync_state = c.getInt(c.getColumnIndex(TableColumns.Collections.SYNC_STATE));
        extra = c.getString(c.getColumnIndex(TableColumns.Collections.EXTRA));
        size = c.getInt(c.getColumnIndex(TableColumns.Collections.SIZE));
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

    public boolean isIdle() {
        return sync_state == SyncState.IDLE;
    }

    @Override
    public String toString() {
        return "LocalCollection{" +
                "id=" + getId() +
                ", uri=" + getUri() +
                ", last_sync_attempt=" + last_sync_attempt +
                ", last_sync_success=" + last_sync_success +
                ", sync_state='" + sync_state + '\'' +
                ", extra=" + extra +
                ", size=" + size +
                '}';
    }


    public ContentValues buildContentValues() {
        ContentValues cv = new ContentValues();
        if (getId() > 0) {
            cv.put(TableColumns.Collections._ID, getId());
        }
        if (sync_state != -1) {
            cv.put(TableColumns.Collections.SYNC_STATE, sync_state);
        }
        if (size != -1) {
            cv.put(TableColumns.Collections.SIZE, size);
        }
        if (last_sync_attempt != -1) {
            cv.put(TableColumns.Collections.LAST_SYNC_ATTEMPT, last_sync_attempt);
        }
        if (last_sync_success != -1) {
            cv.put(TableColumns.Collections.LAST_SYNC, last_sync_success);
        }
        if (!TextUtils.isEmpty(extra)) {
            cv.put(TableColumns.Collections.EXTRA, extra);
        }
        cv.put(TableColumns.Collections.URI, getUri().toString());
        return cv;
    }

    @Override
    public void putFullContentValues(@NotNull BulkInsertMap destination) {
    }

    @Override
    public void putDependencyValues(@NotNull BulkInsertMap destination) {
    }

    public boolean hasNotBeenRegistered() {
        return id == 0;
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
        return getBulkInsertUri().buildUpon().appendPath(String.valueOf(id)).build();
    }

    @Override
    public Uri getBulkInsertUri() {
        return Content.COLLECTIONS.uri;
    }

    @Override @SuppressWarnings("PMD.ModifiedCyclomaticComplexity")
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        LocalCollection that = (LocalCollection) o;

        if (getId() != that.getId()) {
            return false;
        }
        if (last_sync_attempt != that.last_sync_attempt) {
            return false;
        }
        if (last_sync_success != that.last_sync_success) {
            return false;
        }
        if (size != that.size) {
            return false;
        }
        if (sync_state != that.sync_state) {
            return false;
        }
        if (extra != null ? !extra.equals(that.extra) : that.extra != null) {
            return false;
        }
        //noinspection RedundantIfStatement
        if (getUri() != null ? !getUri().equals(that.getUri()) : that.getUri() != null) {
            return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
        int result = (int) (getId() ^ (getId() >>> 32));
        result = 31 * result + (getUri() != null ? getUri().hashCode() : 0);
        result = 31 * result + (int) (last_sync_attempt ^ (last_sync_attempt >>> 32));
        result = 31 * result + (int) (last_sync_success ^ (last_sync_success >>> 32));
        result = 31 * result + size;
        result = 31 * result + sync_state;
        result = 31 * result + (extra != null ? extra.hashCode() : 0);
        return result;
    }
}
