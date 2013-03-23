package com.soundcloud.android.service.sync;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.preference.PreferenceManager;
import com.soundcloud.android.dao.LocalCollectionDAO;
import com.soundcloud.android.model.LocalCollection;
import com.soundcloud.android.provider.Content;
import com.soundcloud.android.provider.DBHelper;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class SyncStateManager {
    private final LocalCollectionDAO mLocalCollectionDao;

    public SyncStateManager(ContentResolver resolver) {
        mLocalCollectionDao = new LocalCollectionDAO(resolver);
    }

    public  LocalCollection insertLocalCollection(
            Uri contentUri,
            int syncState,
            long lastSyncAttempt,
            long lastSyncSuccess,
            int size,
            String extra) {

        // create if not there
        ContentValues cv = new ContentValues();
        cv.put(DBHelper.Collections.URI, contentUri.toString());
        if (lastSyncAttempt != -1) cv.put(DBHelper.Collections.LAST_SYNC_ATTEMPT, lastSyncAttempt);
        if (lastSyncSuccess != -1) cv.put(DBHelper.Collections.LAST_SYNC, lastSyncSuccess);
        if (size != -1)        cv.put(DBHelper.Collections.SIZE, size);
        cv.put(DBHelper.Collections.SYNC_STATE, syncState);
        cv.put(DBHelper.Collections.EXTRA, extra);

        long id = mLocalCollectionDao.create(cv);
        return new LocalCollection((int) id, contentUri, lastSyncAttempt,lastSyncSuccess, syncState, size, extra);
    }


    public @NotNull LocalCollection fromContent(Content content) {
        return fromContent(content.uri);
    }

    public @NotNull LocalCollection fromContent(Uri contentUri) {
        return mLocalCollectionDao.fromContentUri(contentUri, true);
    }

    public boolean updateLastSyncSuccessTime(Content content, long time) {
        return updateLastSyncSuccessTime(content.uri, time);
    }

    public boolean updateLastSyncSuccessTime(Uri uri, long time) {
        LocalCollection lc = fromContent(uri);

        ContentValues   cv = new ContentValues();
        cv.put(DBHelper.Collections.LAST_SYNC, time);

        return mLocalCollectionDao.update(lc.id, cv);
    }

    public boolean forceToStale(Content content) {
        return forceToStale(content.uri);
    }

    public boolean delete(Content content) {
        return mLocalCollectionDao.deleteUri(content.uri);
    }

    public boolean forceToStale(Uri uri) {
        LocalCollection lc = fromContent(uri);
        ContentValues cv = new ContentValues();
        cv.put(DBHelper.Collections.LAST_SYNC, 0);
        cv.put(DBHelper.Collections.LAST_SYNC_ATTEMPT, 0);

        return mLocalCollectionDao.update(lc.id, cv);
    }

    public boolean onSyncComplete(ApiSyncer.Result result, LocalCollection collection) {
        if (result == null) return false;
        if (result.synced_at > 0) collection.last_sync_success = result.synced_at;
        collection.size = result.new_size;
        collection.extra = result.extra;
        collection.sync_state = LocalCollection.SyncState.IDLE;
        return mLocalCollectionDao.update(collection);
    }


    public boolean updateSyncState(long id, int newSyncState) {
        ContentValues cv = new ContentValues();
        cv.put(DBHelper.Collections.SYNC_STATE, newSyncState);
        if (newSyncState == LocalCollection.SyncState.SYNCING || newSyncState == LocalCollection.SyncState.PENDING) {
            cv.put(DBHelper.Collections.LAST_SYNC_ATTEMPT, System.currentTimeMillis());
        }
        return mLocalCollectionDao.update(id, cv);
    }

    public int incrementSyncMiss(Uri contentUri) {
        LocalCollection lc = fromContent(contentUri);
        ContentValues cv = new ContentValues();
        final int misses = lc.syncMisses() + 1;
        cv.put(DBHelper.Collections.EXTRA, misses);
        if (mLocalCollectionDao.update(lc.id, cv)) {
            return misses;
        } else {
            return -1;
        }
    }

    public String getExtraFromUri(Uri contentUri) {
        LocalCollection lc = mLocalCollectionDao.fromContentUri(contentUri, false);
        return lc == null ? null : lc.extra;
    }

    public long getLastSyncAttempt(Uri contentUri) {
        LocalCollection lc = mLocalCollectionDao.fromContentUri(contentUri, false);
        return lc == null ? -1 : lc.last_sync_attempt;
    }

    public long getLastSyncSuccess(Uri contentUri) {
        LocalCollection lc = mLocalCollectionDao.fromContentUri(contentUri, false);
        return lc == null ? -1 : lc.last_sync_success;
    }

    /**
     * Returns a list of uris to be synced, based on recent changes. The idea is that collections which don't change
     * very often don't get synced as frequently as collections which do.
     *
     * @param manual manual sync {@link android.content.ContentResolver#SYNC_EXTRAS_MANUAL}
     */
    public List<Uri> getCollectionsDueForSync(Context c, boolean manual) {
        List<Uri> urisToSync = new ArrayList<Uri>();
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(c);
        for (SyncContent sc : SyncContent.values()) {
            if (sc.isEnabled(prefs)) {
                final LocalCollection lc = fromContent(sc.content);
                if (manual || sc.shouldSync(lc.syncMisses(), lc.last_sync_success)) {
                    urisToSync.add(sc.content.uri);
                }
            }
        }
        return urisToSync;
    }
}
