package com.soundcloud.android.sync;

import com.soundcloud.android.api.legacy.model.LocalCollection;
import com.soundcloud.android.rx.ScSchedulers;
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
import android.database.ContentObserver;
import android.database.Cursor;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// TODO: merge this class with SyncOperations
public class SyncStateManager {
    private final LocalCollectionDAO localCollectionDao;
    private final ContentResolver resolver;

    private final Map<Long, ContentObserver> contentObservers;

    @Deprecated // use @Inject instead
    public SyncStateManager(Context context) {
        this(context.getApplicationContext().getContentResolver(),
                new LocalCollectionDAO(context.getApplicationContext().getContentResolver()));
    }

    @Inject
    public SyncStateManager(ContentResolver resolver, LocalCollectionDAO dao) {
        this.resolver = resolver;
        localCollectionDao = dao;
        contentObservers = new HashMap<>();
    }

    @NotNull
    public LocalCollection fromContent(Content content) {
        return fromContent(content.uri);
    }

    @NotNull
    public LocalCollection fromContent(Uri contentUri) {
        return localCollectionDao.fromContentUri(contentUri, true);
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

        return localCollectionDao.update(lc.getId(), cv);
    }

    public boolean delete(Content content) {
        return localCollectionDao.deleteUri(content.uri);
    }

    public void clear() {
        localCollectionDao.deleteAll();
    }

    public Observable<Boolean> forceToStaleAsync(final Content content) {
        return Observable.create(new Observable.OnSubscribe<Boolean>() {
            @Override
            public void call(Subscriber<? super Boolean> observer) {
                observer.onNext(forceToStale(content));
                observer.onCompleted();
            }
        }).subscribeOn(ScSchedulers.HIGH_PRIO_SCHEDULER);
    }

    public Boolean forceToStale(final Content content) {
        LocalCollection lc = fromContent(content.uri);
        ContentValues cv = new ContentValues();
        cv.put(TableColumns.Collections.LAST_SYNC, 0);
        cv.put(TableColumns.Collections.LAST_SYNC_ATTEMPT, 0);

        return localCollectionDao.update(lc.getId(), cv);
    }

    public boolean onSyncComplete(ApiSyncResult result, LocalCollection collection) {
        if (result == null) {
            return false;
        }
        if (result.synced_at > 0) {
            collection.last_sync_success = result.synced_at;
        }
        collection.size = result.new_size;
        collection.extra = result.extra;
        collection.sync_state = LocalCollection.SyncState.IDLE;
        return localCollectionDao.update(collection);
    }


    public boolean updateSyncState(long id, int newSyncState) {
        ContentValues cv = new ContentValues();
        cv.put(TableColumns.Collections.SYNC_STATE, newSyncState);
        if (newSyncState == LocalCollection.SyncState.SYNCING || newSyncState == LocalCollection.SyncState.PENDING) {
            cv.put(TableColumns.Collections.LAST_SYNC_ATTEMPT, System.currentTimeMillis());
        }
        return localCollectionDao.update(id, cv);
    }

    public int incrementSyncMiss(Uri contentUri) {
        LocalCollection lc = fromContent(contentUri);
        ContentValues cv = new ContentValues();
        final int misses = lc.syncMisses() + 1;
        cv.put(TableColumns.Collections.EXTRA, misses);
        if (localCollectionDao.update(lc.getId(), cv)) {
            return misses;
        } else {
            return -1;
        }
    }

    public long getLastSyncAttempt(Uri contentUri) {
        LocalCollection lc = localCollectionDao.fromContentUri(contentUri, false);
        return lc == null ? -1 : lc.last_sync_attempt;
    }

    public long getLastSyncSuccess(Uri contentUri) {
        LocalCollection lc = localCollectionDao.fromContentUri(contentUri, false);
        return lc == null ? -1 : lc.last_sync_success;
    }

    /**
     * Returns a list of uris to be synced, based on recent changes. The idea is that collections which don't change
     * very often don't get synced as frequently as collections which do.
     *  @param syncContentEnumSet
     * @param force              force sync {@link android.content.ContentResolver#SYNC_EXTRAS_MANUAL}
     */
    public List<Uri> getCollectionsDueForSync(EnumSet<SyncContent> syncContentEnumSet, boolean force) {
        List<Uri> urisToSync = new ArrayList<>();
        for (SyncContent sc : syncContentEnumSet) {
            if (sc.isEnabled() && (force || isContentDueForSync(sc))) {
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
        contentObservers.put(lc.getId(), observer);

        // if the record is created asynchronously, we may not have a valid ID at this point yet (and by extension
        // cannot construct a content URI) so only actually register the observer if an ID is set.
        if (lc.getId() > 0) {
            final Uri contentUri = Content.COLLECTIONS.uri.buildUpon().appendPath(String.valueOf(lc.getId())).build();
            resolver.registerContentObserver(contentUri, true, observer);
        }
    }

    public void removeChangeListener(@NotNull LocalCollection lc) {
        ContentObserver observer = contentObservers.remove(lc.getId());
        if (observer != null) {
            resolver.unregisterContentObserver(observer);
        }
    }

    public boolean resetSyncMisses(Uri contentUri) {
        LocalCollection lc = fromContent(contentUri);
        ContentValues cv = new ContentValues();
        cv.put(TableColumns.Collections.EXTRA, 0);
        return localCollectionDao.update(lc.getId(), cv);
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
                localCollectionDao.create(localCollection);
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
        return (ChangeObserver) contentObservers.get(id);
    }

    /* package */ boolean hasObservers() {
        return contentObservers != null && !contentObservers.isEmpty();
    }

    /* package */ class ChangeObserver extends ContentObserver {
        private final LocalCollection syncState;
        private final LocalCollection.OnChangeListener listener;

        public ChangeObserver(LocalCollection syncState, LocalCollection.OnChangeListener listener) {
            super(new Handler(Looper.getMainLooper()));
            this.syncState = syncState;
            this.listener = listener;
        }

        @Override
        public boolean deliverSelfNotifications() {
            return true;
        }

        @Override
        public void onChange(boolean selfChange) {
            SyncStateQueryHandler handler = new SyncStateQueryHandler(syncState, listener);
            handler.startQuery(0, null, Content.COLLECTIONS.uri, null, "_id = ?", new String[]{String.valueOf(syncState.getId())}, null);
        }

        public LocalCollection.OnChangeListener getListener() {
            return listener;
        }
    }

    private class SyncStateQueryHandler extends AsyncQueryHandler {
        private final LocalCollection localCollection;
        private final LocalCollection.OnChangeListener listener;

        public SyncStateQueryHandler(@NotNull LocalCollection lc, LocalCollection.OnChangeListener listener) {
            super(resolver);
            localCollection = lc;
            this.listener = listener;
        }

        @Override
        protected void onQueryComplete(int token, Object cookie, Cursor cursor) {
            onCollectionAsyncQueryReturn(cursor, localCollection, listener);
        }
    }


}
