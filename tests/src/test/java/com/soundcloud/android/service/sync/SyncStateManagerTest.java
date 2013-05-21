package com.soundcloud.android.service.sync;


import static com.soundcloud.android.Expect.expect;

import com.soundcloud.android.dao.LocalCollectionDAO;
import com.soundcloud.android.model.LocalCollection;
import com.soundcloud.android.provider.Content;
import com.soundcloud.android.robolectric.DefaultTestRunner;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import android.content.ContentResolver;
import android.net.Uri;

@RunWith(DefaultTestRunner.class)
public class SyncStateManagerTest {

    SyncStateManager syncStateManager;
    ContentResolver resolver;

    @Before public void before() {
        resolver = DefaultTestRunner.application.getContentResolver();
        syncStateManager = new SyncStateManager();
    }

    @Test
    public void shouldCreateSyncStateRecordLazily() {
        expect(Content.COLLECTIONS).toHaveCount(0);

        Uri contentUri = Content.PLAYLISTS.forId(123);
        LocalCollection syncState = syncStateManager.fromContent(contentUri);
        expect(Content.COLLECTIONS).toHaveCount(1);
        expect(syncState).not.toBeNull();
        expect(syncState.uri).toEqual(contentUri);
    }

    @Test
    public void shouldGetLastSyncAttempt() throws Exception {
        final Uri uri = Uri.parse("foo");

        LocalCollection c = new LocalCollection(uri, 100, 1, LocalCollection.SyncState.PENDING, 0, null);
        new LocalCollectionDAO(resolver).create(c);
        expect(c).not.toBeNull();
        expect(syncStateManager.getLastSyncAttempt(uri)).toEqual(100L);
        expect(syncStateManager.getLastSyncAttempt(Uri.parse("notfound"))).toEqual(-1L);
    }

    @Test
    public void shouldGetLastSyncSuccess() throws Exception {
        final Uri uri = Uri.parse("foo");
        LocalCollection c = new LocalCollection(uri, 1, 100, LocalCollection.SyncState.PENDING, 0, null);
        new LocalCollectionDAO(resolver).create(c);
        expect(c).not.toBeNull();
        expect(syncStateManager.getLastSyncSuccess(uri)).toEqual(100L);
        expect(syncStateManager.getLastSyncSuccess(Uri.parse("notfound"))).toEqual(-1L);
    }

    @Test
    public void shouldUpdateLastSyncTime() throws Exception {
        final Uri uri = Uri.parse("foo");
        insertLocalCollection(uri);
        syncStateManager.updateLastSyncSuccessTime(uri, 200);
        expect(syncStateManager.fromContent(uri).last_sync_success).toEqual(200L);
    }

    @Test
    public void shouldForceToStale() throws Exception {
        final Uri uri = Uri.parse("foo");
        insertLocalCollection(uri);
        syncStateManager.updateLastSyncSuccessTime(uri, 200);
        expect(syncStateManager.fromContent(uri).last_sync_success).toEqual(200L);

        syncStateManager.forceToStale(uri);
        expect(syncStateManager.fromContent(uri).last_sync_success).toEqual(0L);
    }

    @Test
    public void shouldChangeAutoRefresh() throws Exception {
        Uri uri = Content.ME_LIKES.uri;
        LocalCollection lc = new LocalCollection(uri, 100, 1, LocalCollection.SyncState.IDLE, 0, null);
        new LocalCollectionDAO(resolver).create(lc);
        expect(lc.shouldAutoRefresh()).toBeTrue();
        syncStateManager.updateSyncState(lc.id, LocalCollection.SyncState.SYNCING);
        expect(syncStateManager.fromContent(uri).shouldAutoRefresh()).toBeFalse();
    }

    @Test
    public void shouldAddListenerWithFirstAsyncQuery() {
        final LocalCollection lc = new LocalCollection(Content.ME_LIKES.uri,
                System.currentTimeMillis(), System.currentTimeMillis(), LocalCollection.SyncState.SYNCING, 50, "extra");

        final long[] id = {0};
        final LocalCollection.OnChangeListener listener = new LocalCollection.OnChangeListener() {
            @Override
            public void onLocalCollectionChanged(LocalCollection localCollection) {
                id[0] = localCollection.id;
            }
        };
        syncStateManager.onCollectionAsyncQueryReturn(null, lc, listener);
        expect(syncStateManager.getObserverById(id[0]).getListener()).toBe(listener);
    }

    @Test
    public void shouldNotAddListenerWithFirstAsyncQuery() {
        final LocalCollection lc = new LocalCollection(Content.ME_LIKES.uri,
                System.currentTimeMillis(), System.currentTimeMillis(), LocalCollection.SyncState.SYNCING, 50, "extra");
        lc.id = 123L;

        final long[] id = {0};
        final LocalCollection.OnChangeListener listener = new LocalCollection.OnChangeListener() {
            @Override
            public void onLocalCollectionChanged(LocalCollection localCollection) {
                id[0] = localCollection.id;
            }
        };
        syncStateManager.onCollectionAsyncQueryReturn(null, lc, listener);
        expect(syncStateManager.hasObservers()).toBeFalse();
    }

    @Test
    public void shouldNotAddObserverWithNoListener() {
        final LocalCollection lc = new LocalCollection(Content.ME_LIKES.uri,
                System.currentTimeMillis(), System.currentTimeMillis(), LocalCollection.SyncState.SYNCING, 50, "extra");
        syncStateManager.onCollectionAsyncQueryReturn(null, lc, null);
        expect(syncStateManager.hasObservers()).toBeFalse();
    }

    @Test
    public void shouldInitializeNewLocalCollectionIfNotInDatabase() {
        final LocalCollection lc = new LocalCollection(Content.ME_LIKES.uri,
                System.currentTimeMillis(), System.currentTimeMillis(), LocalCollection.SyncState.SYNCING, 50, "extra");

        final boolean[] listenerCalled = {false};
        syncStateManager.onCollectionAsyncQueryReturn(null, lc, new LocalCollection.OnChangeListener() {
            @Override
            public void onLocalCollectionChanged(LocalCollection localCollection) {
                expect(localCollection.last_sync_success).toEqual(-1L);
                expect(localCollection.sync_state).toEqual(LocalCollection.SyncState.IDLE);
                listenerCalled[0] = true;

            }
        });
        expect(listenerCalled[0]).toBeTrue();
    }

    private LocalCollection insertLocalCollection(Uri contentUri) {
        LocalCollection collection = new LocalCollection(contentUri);
        new LocalCollectionDAO(resolver).create(collection);
        return collection;
    }

}
