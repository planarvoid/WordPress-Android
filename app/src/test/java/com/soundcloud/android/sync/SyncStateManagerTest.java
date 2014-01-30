package com.soundcloud.android.sync;


import static com.soundcloud.android.Expect.expect;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.model.LocalCollection;
import com.soundcloud.android.robolectric.DefaultTestRunner;
import com.soundcloud.android.storage.LocalCollectionDAO;
import com.soundcloud.android.storage.provider.Content;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import android.app.Activity;
import android.content.ContentResolver;
import android.net.Uri;

@RunWith(DefaultTestRunner.class)
public class SyncStateManagerTest {

    SyncStateManager syncStateManager;
    ContentResolver resolver;
    @Mock LocalCollection.OnChangeListener onLocalCollectionChangeListener;

    @Before public void before() {
        MockitoAnnotations.initMocks(this);
        resolver = DefaultTestRunner.application.getContentResolver();
        syncStateManager = new SyncStateManager(resolver, new LocalCollectionDAO(resolver));
    }

    @Test
    public void shouldCreateSyncStateRecordLazily() {
        expect(Content.COLLECTIONS).toHaveCount(0);

        Uri contentUri = Content.PLAYLISTS.forId(123);
        LocalCollection syncState = syncStateManager.fromContent(contentUri);
        expect(Content.COLLECTIONS).toHaveCount(1);
        expect(syncState).not.toBeNull();
        expect(syncState.getUri()).toEqual(contentUri);
    }

    @Test
    public void shouldCreateSyncStateWithoutQueryparam() {
        final Uri uri = Content.PLAYLISTS.forId(123);
        Uri contentUri = uri.buildUpon().appendQueryParameter("foo","bar").build();
        expect(syncStateManager.fromContent(contentUri).getUri()).toEqual(uri);
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
        final Uri uri = Content.ME_SOUND_STREAM.uri;
        insertLocalCollection(uri);
        syncStateManager.updateLastSyncSuccessTime(uri, 200);
        expect(syncStateManager.fromContent(uri).last_sync_success).toEqual(200L);

        syncStateManager.forceToStale(Content.ME_SOUND_STREAM);
        expect(syncStateManager.fromContent(uri).last_sync_success).toEqual(0L);
    }

    @Test
    public void shouldChangeAutoRefresh() throws Exception {
        Uri uri = Content.ME_LIKES.uri;
        LocalCollection lc = new LocalCollection(uri, 100, 1, LocalCollection.SyncState.IDLE, 0, null);
        new LocalCollectionDAO(resolver).create(lc);
        expect(lc.shouldAutoRefresh()).toBeTrue();
        syncStateManager.updateSyncState(lc.getId(), LocalCollection.SyncState.SYNCING);
        expect(syncStateManager.fromContent(uri).shouldAutoRefresh()).toBeFalse();
    }

    @Test
    public void shouldAddListenerWithFirstAsyncQuery() {
        final LocalCollection lc = new LocalCollection(Content.ME_LIKES.uri,
                System.currentTimeMillis(), System.currentTimeMillis(), LocalCollection.SyncState.SYNCING, 50, "extra");

        syncStateManager.onCollectionAsyncQueryReturn(null, lc, onLocalCollectionChangeListener);
        final SyncStateManager.ChangeObserver observerById = syncStateManager.getObserverById(1);
        expect(observerById.getListener()).toBe(onLocalCollectionChangeListener);

    }

    @Test
    public void shouldNotAddListenerWhenCollectionAlreadyHasId() {
        final LocalCollection lc = new LocalCollection(Content.ME_LIKES.uri,
                System.currentTimeMillis(), System.currentTimeMillis(), LocalCollection.SyncState.SYNCING, 50, "extra");
        lc.setId(123L);
        syncStateManager.onCollectionAsyncQueryReturn(null, lc, onLocalCollectionChangeListener);
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

        final LocalCollection initLc = new LocalCollection(Content.ME_LIKES.uri);
        initLc.setId(1);

        syncStateManager.onCollectionAsyncQueryReturn(null, lc, onLocalCollectionChangeListener);
        verify(onLocalCollectionChangeListener).onLocalCollectionChanged(initLc);
    }

    @Test
    public void shouldResolveContextToApplicationContextToPreventMemoryLeaks() {
        Activity activity = mock(Activity.class);
        when(activity.getApplicationContext()).thenReturn(activity);
        new SyncStateManager(activity);
        verify(activity, atLeastOnce()).getApplicationContext();
    }

    private LocalCollection insertLocalCollection(Uri contentUri) {
        LocalCollection collection = new LocalCollection(contentUri);
        new LocalCollectionDAO(resolver).create(collection);
        return collection;
    }

}
