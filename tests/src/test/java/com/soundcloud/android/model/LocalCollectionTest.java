package com.soundcloud.android.model;

import static com.soundcloud.android.Expect.expect;

import com.soundcloud.android.provider.Content;
import com.soundcloud.android.robolectric.DefaultTestRunner;
import com.soundcloud.android.service.sync.SyncConfig;
import com.soundcloud.android.service.sync.SyncContentTest;
import com.xtremelabs.robolectric.Robolectric;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import android.content.ContentResolver;
import android.net.Uri;

@RunWith(DefaultTestRunner.class)
public class LocalCollectionTest {
    ContentResolver resolver;

    @Before
    public void before() {
        resolver = Robolectric.application.getContentResolver();
    }

    @Test
    public void shouldInsertCollection() throws Exception {
        final Uri uri = Uri.parse("foo");
        LocalCollection c = LocalCollection.insertLocalCollection(uri, resolver);
        expect(c.uri).toEqual(uri);
        expect(c.last_sync_attempt).toBe(-1L);
        expect(c.last_sync_success).toBe(-1L);
        expect(c.size).toBe(-1);
    }

    @Test
    public void shouldInsertCollectionWithParams() throws Exception {
        final Uri uri = Uri.parse("foo");
        LocalCollection c = LocalCollection.insertLocalCollection(uri, 1, 1, 1, 2, "some-extra",resolver);
        expect(c.uri).toEqual(uri);
        expect(c.last_sync_attempt).toBe(1L);
        expect(c.last_sync_success).toBe(1L);
        expect(c.size).toBe(2);
        expect(c.sync_state).toEqual(1);
        expect(c.extra).toEqual("some-extra");
        expect(c).toEqual(LocalCollection.fromContentUri(uri, resolver, true));
    }

    @Test
    public void shouldSupportEqualsAndHashcode() throws Exception {
        LocalCollection c1 = new LocalCollection(1, Uri.parse("foo"), 1, 1, 0, 0, null);
        LocalCollection c2 = new LocalCollection(1, Uri.parse("foo"), 1, 1, 0, 0, null);
        LocalCollection c3 = new LocalCollection(100, Uri.parse("foo"), 1, 1, 0, 0, null);
        expect(c1).toEqual(c2);
        expect(c2).not.toEqual(c3);
    }

    @Test
    public void shouldPersistLocalCollection() throws Exception {
        final Uri uri = Uri.parse("foo");
        LocalCollection c = LocalCollection.insertLocalCollection(uri, 1, 1, 100, 0, "some-extra", resolver);
        LocalCollection c2 = LocalCollection.fromContentUri(uri, resolver, true);

        expect(c.id).toEqual(c2.id);
        expect(c.uri).toEqual(c2.uri);
        expect(c.last_sync_attempt).toEqual(c2.last_sync_attempt);
        expect(c.last_sync_success).toEqual(c2.last_sync_success);
        expect(c.size).toEqual(c2.size);

        expect(Content.COLLECTIONS).toHaveCount(1);
    }

    @Test
    public void shouldReturnNullIfCollectionNotFound() throws Exception {
        expect(LocalCollection.fromContentUri(Uri.parse("blaz"), resolver, false)).toBeNull();
        expect(LocalCollection.fromContentUri(Uri.parse("blaz"), resolver, true)).not.toBeNull();
    }

    @Test
    public void shouldgetLastSyncAttempt() throws Exception {
        final Uri uri = Uri.parse("foo");
        LocalCollection c = LocalCollection.insertLocalCollection(uri, 1, 100, 1, 0, null, resolver);
        expect(c).not.toBeNull();
        expect(LocalCollection.getLastSyncAttempt(uri, resolver)).toEqual(100L);
        expect(LocalCollection.getLastSyncAttempt(Uri.parse("notfound"), resolver)).toEqual(-1L);
    }

    @Test
    public void shouldgetLastSyncSuccess() throws Exception {
        final Uri uri = Uri.parse("foo");
        LocalCollection c = LocalCollection.insertLocalCollection(uri, 1, 1, 100, 0, null, resolver);
        expect(c).not.toBeNull();
        expect(LocalCollection.getLastSyncSuccess(uri, resolver)).toEqual(100L);
        expect(LocalCollection.getLastSyncSuccess(Uri.parse("notfound"), resolver)).toEqual(-1L);
    }

    @Test
    public void shouldUpdateLastSyncTime() throws Exception {
        final Uri uri = Uri.parse("foo");
        LocalCollection c = LocalCollection.insertLocalCollection(uri, resolver);
        c.updateLastSyncSuccessTime(200, resolver);
        expect(LocalCollection.fromContentUri(uri, resolver, true).last_sync_success).toEqual(200L);
    }

    @Test
    public void shouldForceToStale() throws Exception {
        final Uri uri = Uri.parse("foo");
        LocalCollection c = LocalCollection.insertLocalCollection(uri, resolver);
        c.updateLastSyncSuccessTime(200, resolver);
        expect(LocalCollection.fromContentUri(uri, resolver, true).last_sync_success).toEqual(200L);

        LocalCollection.forceToStale(uri,resolver);
        expect(LocalCollection.fromContentUri(uri, resolver, true).last_sync_success).toEqual(0L);
    }

    @Test
    public void shouldDeleteCollection() throws Exception {

        final Uri uri = Uri.parse("foo");
        expect(LocalCollection.insertLocalCollection(uri, resolver)).not.toBeNull();
        expect(LocalCollection.deleteUri(uri, resolver)).toBeTrue();
        expect(LocalCollection.deleteUri(uri, resolver)).toBeFalse();
        expect(Content.COLLECTIONS).toHaveCount(0);
    }

    @Test
    public void shouldAutoRefreshFavorites() throws Exception {
        LocalCollection lc = new LocalCollection(1,
                Content.ME_FAVORITES.uri,
                System.currentTimeMillis() - SyncConfig.DEFAULT_ATTEMPT_DELAY,
                0,
                100, 0, null);
        expect(lc.shouldAutoRefresh()).toBeTrue();
    }

    @Test
    public void shouldNotAutoRefreshFavorites() throws Exception {
        LocalCollection lc = new LocalCollection(1,
                Content.ME_FAVORITES.uri,
                System.currentTimeMillis(),
                0,
                100, 0, null);
        expect(lc.shouldAutoRefresh()).toBeFalse();
    }

    @Test
    public void shouldNotAutoRefreshFavorites2() throws Exception {
        LocalCollection lc = new LocalCollection(1,
                Content.ME_FAVORITES.uri,
                System.currentTimeMillis() - SyncConfig.DEFAULT_ATTEMPT_DELAY,
                System.currentTimeMillis() - SyncConfig.TRACK_STALE_TIME + 10000,
                100, 0, null);
        expect(lc.shouldAutoRefresh()).toBeFalse();
    }


    @Test
    public void shouldAutoRefreshFollowings() throws Exception {
        LocalCollection lc = new LocalCollection(1, Content.ME_FOLLOWINGS.uri, 1, 0, 100, 0, null);
        expect(lc.shouldAutoRefresh()).toBeTrue();
    }

    @Test
    public void shouldNotAutoRefreshFollowings() throws Exception {
        LocalCollection lc = new LocalCollection(1, Content.ME_FOLLOWINGS.uri, 0, 1, 100, 0, null);
        expect(lc.shouldAutoRefresh()).toBeFalse();
    }

    @Test
    public void shouldChangeAutoRefresh() throws Exception {
        Uri uri = Content.ME_FAVORITES.uri;
        LocalCollection lc = LocalCollection.insertLocalCollection(uri, 1, 1, 100, 0, null, resolver);
        expect(lc.shouldAutoRefresh()).toBeTrue();
        lc.updateSyncState(LocalCollection.SyncState.SYNCING,resolver);
        lc.updateSyncState(LocalCollection.SyncState.IDLE,resolver);
        expect(LocalCollection.fromContentUri(uri,resolver,false).shouldAutoRefresh()).toBeFalse();
    }



}
