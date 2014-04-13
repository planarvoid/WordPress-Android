package com.soundcloud.android.sync;

import com.soundcloud.android.model.LocalCollection;
import com.soundcloud.android.rx.ScSchedulers;
import com.soundcloud.android.rx.ScheduledOperations;
import com.soundcloud.android.storage.LocalCollectionDAO;
import com.soundcloud.android.storage.TableColumns;
import com.soundcloud.android.storage.provider.Content;
import com.soundcloud.android.utils.IOUtils;
import com.soundcloud.android.utils.UriUtils;
import org.jetbrains.annotations.NotNull;
import rx.Observable;
import rx.Subscriber;

import android.content.AsyncQueryHandler;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.ContentObserver;
import android.database.Cursor;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.preference.PreferenceManager;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// TODO: merge this class with SyncOperations
public class SyncStateManager extends ScheduledOperations {
    private final LocalCollectionDAO mLocalCollectionDao;
    private final ContentResolver mResolver;

    private final Map<Long, ContentObserver> mContentObservers;

    @Deprecated // use @Inject instead
    public SyncStateManager(Context context) {
        this(context.getApplicationContext().getContentResolver(),
                new LocalCollectionDAO(context.getApplicationContext().getContentResolver()));
    }

    @Inject
    public SyncStateManager(ContentResolver resolver, LocalCollectionDAO dao) {
        super(ScSchedulers.STORAGE_SCHEDULER);
        mResolver = resolver;
        mLocalCollectionDao = dao;
        mContentObservers = new HashMap<Long, ContentObserver>();
    }

    @NotNull
    public LocalCollection fromContent(Content content) {
        return fromContent(content.uri);
    }

    @NotNull
    public LocalCollection fromContent(Uri contentUri) {
        return mLocalCollectionDao.fromContentUri(contentUri, true);
    }

    /**
     * Returns a blank sync state record which will be either loaded or created asynchronously and redelivered through
     * the given listener.
     *
     * @param contentUri content URI for the sync state to observe
     * @param listener   callback that's called when load or insert finished
     * @return the sync state instance
     */
    @NotNull
    public LocalCollection fromContentAsync(@NotNull Uri contentUri, @NotNull LocalCollection.OnChangeListener listener) {
        final Uri cleanUri = UriUtils.clearQueryParams(contentUri);
        LocalCollection syncState = new LocalCollection(cleanUri);
        SyncStateQueryHandler handler = new SyncStateQueryHandler(syncState, listener);
        handler.startQuery(0, null, Content.COLLECTIONS.uri, null, "uri = ?", new String[]{cleanUri.toString()}, null);
        return syncState;
    }

    public boolean updateLastSyncSuccessTime(Content content, long time) {
        return updateLastSyncSuccessTime(content.uri, time);
    }

    public boolean updateLastSyncSuccessTime(Uri uri, long time) {
        LocalCollection lc = fromContent(uri);

        ContentValues cv = new ContentValues();
        cv.put(TableColumns.Collections.LAST_SYNC, time);

        return mLocalCollectionDao.update(lc.getId(), cv);
    }

    public boolean delete(Content content) {
        return mLocalCollectionDao.deleteUri(content.uri);
    }

    public void clear() {
        mLocalCollectionDao.deleteAll();
    }

    public Observable<Boolean> forceToStaleAsync(final Content content) {
        return schedule(Observable.create(new Observable.OnSubscribe<Boolean>() {
            @Override
            public void call(Subscriber<? super Boolean> observer) {
                observer.onNext(forceToStale(content));
                observer.onCompleted();
            }
        }));
    }

    public Boolean forceToStale(final Content content) {
        LocalCollection lc = fromContent(content.uri);
        ContentValues cv = new ContentValues();
        cv.put(TableColumns.Collections.LAST_SYNC, 0);
        cv.put(TableColumns.Collections.LAST_SYNC_ATTEMPT, 0);

        return mLocalCollectionDao.update(lc.getId(), cv);
    }

    public boolean onSyncComplete(ApiSyncResult result, LocalCollection collection) {
        if (result == null) return false;
        if (result.synced_at > 0) collection.last_sync_success = result.synced_at;
        collection.size = result.new_size;
        collection.extra = result.extra;
        collection.sync_state = LocalCollection.SyncState.IDLE;
        return mLocalCollectionDao.update(collection);
    }


    public boolean updateSyncState(long id, int newSyncState) {
        ContentValues cv = new ContentValues();
        cv.put(TableColumns.Collections.SYNC_STATE, newSyncState);
        if (newSyncState == LocalCollection.SyncState.SYNCING || newSyncState == LocalCollection.SyncState.PENDING) {
            cv.put(TableColumns.Collections.LAST_SYNC_ATTEMPT, System.currentTimeMillis());
        }
        return mLocalCollectionDao.update(id, cv);
    }

    public int incrementSyncMiss(Uri contentUri) {
        LocalCollection lc = fromContent(contentUri);
        ContentValues cv = new ContentValues();
        final int misses = lc.syncMisses() + 1;
        cv.put(TableColumns.Collections.EXTRA, misses);
        if (mLocalCollectionDao.update(lc.getId(), cv)) {
            return misses;
        } else {
            return -1;
        }
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
     * @param syncContentEnumSet
     * @param force force sync {@link android.content.ContentResolver#SYNC_EXTRAS_MANUAL}
     */
    public List<Uri> getCollectionsDueForSync(Context c, EnumSet<SyncContent> syncContentEnumSet, boolean force) {
        List<Uri> urisToSync = new ArrayList<Uri>();
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(c);
        for (SyncContent sc : syncContentEnumSet) {
            if (sc.isEnabled(prefs) && (force || isContentDueForSync(sc))){
                urisToSync.add(sc.content.uri);
            }
        }
        return urisToSync;
    }

    public boolean isContentDueForSync(SyncContent syncContent) {
        final LocalCollection lc = fromContent(syncContent.content);
        if (syncContent.shouldSync(lc.syncMisses(), lc.last_sync_success)) {
            return true;
        }
        return false;
    }

    public void addChangeListener(@NotNull LocalCollection lc, @NotNull LocalCollection.OnChangeListener listener) {
        ChangeObserver observer = new ChangeObserver(lc, listener);
        mContentObservers.put(lc.getId(), observer);

        // if the record is created asynchronously, we may not have a valid ID at this point yet (and by extension
        // cannot construct a content URI) so only actually register the observer if an ID is set.
        if (lc.getId() > 0) {
            final Uri contentUri = Content.COLLECTIONS.uri.buildUpon().appendPath(String.valueOf(lc.getId())).build();
            mResolver.registerContentObserver(contentUri, true, observer);
        }
    }

    public void removeChangeListener(@NotNull LocalCollection lc) {
        ContentObserver observer = mContentObservers.remove(lc.getId());
        if (observer != null) mResolver.unregisterContentObserver(observer);
    }

    /* package */ void onCollectionAsyncQueryReturn(Cursor cursor, LocalCollection localCollection, LocalCollection.OnChangeListener listener) {
        try {
            final boolean wasRegistered = localCollection.hasNotBeenRegistered();
            if (cursor != null && cursor.moveToFirst()) {
                // the sync state record already existed, just inform the listener that it has changed
                localCollection.setFromCursor(cursor);
            } else {
                // create a new local collection in intialized state
                localCollection = new LocalCollection(localCollection.getUri());
                // the record didn't exist yet; go ahead and create it before reporting any changes
                mLocalCollectionDao.create(localCollection);
            }
            if (wasRegistered && listener != null) {
                addChangeListener(localCollection, listener);
            }
        } finally {
            IOUtils.close(cursor);
        }

        if (listener != null) {
            listener.onLocalCollectionChanged(localCollection);
        }
    }

    /* package */ ChangeObserver getObserverById(long id) {
        return (ChangeObserver) mContentObservers.get(id);
    }

    /* package */ boolean hasObservers() {
        return mContentObservers != null && !mContentObservers.isEmpty();
    }

    /* package */ class ChangeObserver extends ContentObserver {
        private final LocalCollection mSyncState;
        private final LocalCollection.OnChangeListener mListener;

        public ChangeObserver(LocalCollection syncState, LocalCollection.OnChangeListener listener) {
            super(new Handler(Looper.getMainLooper()));
            mSyncState = syncState;
            mListener = listener;
        }

        @Override
        public boolean deliverSelfNotifications() {
            return true;
        }

        @Override
        public void onChange(boolean selfChange) {
            SyncStateQueryHandler handler = new SyncStateQueryHandler(mSyncState, mListener);
            handler.startQuery(0, null, Content.COLLECTIONS.uri, null, "_id = ?", new String[]{String.valueOf(mSyncState.getId())}, null);
        }

        public LocalCollection.OnChangeListener getListener() {
            return mListener;
        }
    }

    private class SyncStateQueryHandler extends AsyncQueryHandler {
        private LocalCollection mLocalCollection;
        private final LocalCollection.OnChangeListener mListener;

        public SyncStateQueryHandler(@NotNull LocalCollection lc, LocalCollection.OnChangeListener listener) {
            super(mResolver);
            mLocalCollection = lc;
            mListener = listener;
        }

        @Override
        protected void onQueryComplete(int token, Object cookie, Cursor cursor) {
            onCollectionAsyncQueryReturn(cursor, mLocalCollection, mListener);
        }
    }


}
