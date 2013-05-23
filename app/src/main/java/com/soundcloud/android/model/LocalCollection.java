package com.soundcloud.android.model;

import com.soundcloud.android.dao.ContentValuesProvider;
import com.soundcloud.android.model.act.Activity;
import com.soundcloud.android.provider.BulkInsertMap;
import com.soundcloud.android.provider.Content;
import com.soundcloud.android.provider.DBHelper;
import com.soundcloud.android.service.sync.SyncConfig;
import org.jetbrains.annotations.NotNull;

import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;
import android.text.TextUtils;

/**
 * Represents the state of a local collection sync, including last sync and size.
 * See {@link DBHelper.Collections}.
 */
public class LocalCollection implements ModelLike, ContentValuesProvider {
    private long id;
    private final Uri uri;

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

    public interface OnChangeListener {
        void onLocalCollectionChanged(LocalCollection localCollection);
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
        setId(c.getInt(c.getColumnIndex(DBHelper.Collections._ID)));
        uri = Uri.parse(c.getString(c.getColumnIndex(DBHelper.Collections.URI)));
        setFromCursor(c);
    }

    public boolean hasSyncedBefore() {
        return last_sync_success > 0;
    }

    public Uri getUri() {
        return uri;
    }

    public void setFromCursor(Cursor c) {
        if (getId() <= 0) setId(c.getInt(c.getColumnIndex(DBHelper.Collections._ID)));
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
        if (getId() > 0) cv.put(DBHelper.Collections._ID, getId());
        if (sync_state != -1) cv.put(DBHelper.Collections.SYNC_STATE, sync_state);
        if (size != -1) cv.put(DBHelper.Collections.SIZE, size);
        if (last_sync_attempt != -1) cv.put(DBHelper.Collections.LAST_SYNC_ATTEMPT, last_sync_attempt);
        if (last_sync_success != -1) cv.put(DBHelper.Collections.LAST_SYNC, last_sync_success);
        if (!TextUtils.isEmpty(extra)) cv.put(DBHelper.Collections.EXTRA, extra);
        cv.put(DBHelper.Collections.URI, getUri().toString());
        return cv;
    }

    @Override
    public void putFullContentValues(@NotNull BulkInsertMap destination) {
    }

    @Override
    public void putDependencyValues(@NotNull BulkInsertMap destination) {
    }

    public boolean shouldAutoRefresh() {
        if (!isIdle() || getId() <= 0) return false;
        Content c = Content.match(getUri());

        // only auto refresh once every 30 mins at most, that we won't hammer their phone or the api if there are errors
        if (c == null || last_sync_attempt > System.currentTimeMillis() - SyncConfig.DEFAULT_ATTEMPT_DELAY) return false;

        // do not auto refresh users when the list opens, because users are always changing
        if (c.isUserBased()) return last_sync_success <= 0;

        final long staleTime = (Track.class.equals(c.modelType))    ? SyncConfig.TRACK_STALE_TIME :
                               (Playlist.class.equals(c.modelType)) ? SyncConfig.PLAYLIST_STALE_TIME :
                               (Activity.class.equals(c.modelType)) ? SyncConfig.ACTIVITY_STALE_TIME :
                               SyncConfig.DEFAULT_STALE_TIME;

        return System.currentTimeMillis() - last_sync_success > staleTime;
    }

    public boolean hasNotBeenRegistered(){
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
        return Content.COLLECTIONS.uri.buildUpon().appendPath(String.valueOf(getId())).build();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        LocalCollection that = (LocalCollection) o;

        if (getId() != that.getId()) return false;
        if (last_sync_attempt != that.last_sync_attempt) return false;
        if (last_sync_success != that.last_sync_success) return false;
        if (size != that.size) return false;
        if (sync_state != that.sync_state) return false;
        if (extra != null ? !extra.equals(that.extra) : that.extra != null) return false;
        //noinspection RedundantIfStatement
        if (getUri() != null ? !getUri().equals(that.getUri()) : that.getUri() != null) return false;
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
