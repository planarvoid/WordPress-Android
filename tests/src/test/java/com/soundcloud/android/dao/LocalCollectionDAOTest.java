package com.soundcloud.android.dao;

import android.net.Uri;
import com.soundcloud.android.model.LocalCollection;
import com.soundcloud.android.provider.Content;
import org.junit.Test;

import static com.soundcloud.android.Expect.expect;

public class LocalCollectionDAOTest extends BaseDAOTest {
    @Test
    public void shouldInsertCollection() throws Exception {
        final Uri uri = Uri.parse("foo");
        LocalCollection c = LocalCollectionDAO.insertLocalCollection(uri, resolver);
        expect(c.uri).toEqual(uri);
        expect(c.last_sync_attempt).toBe(-1L);
        expect(c.last_sync_success).toBe(-1L);
        expect(c.size).toBe(-1);
    }

    @Test
    public void shouldInsertCollectionWithParams() throws Exception {
        final Uri uri = Uri.parse("foo");
        LocalCollection c = LocalCollectionDAO.insertLocalCollection(uri, 1, 1, 1, 2, "some-extra", resolver);
        expect(c.uri).toEqual(uri);
        expect(c.last_sync_attempt).toBe(1L);
        expect(c.last_sync_success).toBe(1L);
        expect(c.size).toBe(2);
        expect(c.sync_state).toEqual(1);
        expect(c.extra).toEqual("some-extra");
        expect(c).toEqual(LocalCollectionDAO.fromContentUri(uri, resolver, true));
    }


    @Test
    public void shouldPersistLocalCollection() throws Exception {
        final Uri uri = Uri.parse("foo");
        LocalCollection c = LocalCollectionDAO.insertLocalCollection(uri, 1, 1, 100, 0, "some-extra", resolver);
        LocalCollection c2 = LocalCollectionDAO.fromContentUri(uri, resolver, true);

        expect(c.id).toEqual(c2.id);
        expect(c.uri).toEqual(c2.uri);
        expect(c.last_sync_attempt).toEqual(c2.last_sync_attempt);
        expect(c.last_sync_success).toEqual(c2.last_sync_success);
        expect(c.size).toEqual(c2.size);

        expect(Content.COLLECTIONS).toHaveCount(1);
    }

    @Test
    public void shouldReturnNullIfCollectionNotFound() throws Exception {
        expect(LocalCollectionDAO.fromContentUri(Uri.parse("blaz"), resolver, false)).toBeNull();
        expect(LocalCollectionDAO.fromContentUri(Uri.parse("blaz"), resolver, true)).not.toBeNull();
    }

    @Test
    public void shouldgetLastSyncAttempt() throws Exception {
        final Uri uri = Uri.parse("foo");
        LocalCollection c = LocalCollectionDAO.insertLocalCollection(uri, 1, 100, 1, 0, null, resolver);
        expect(c).not.toBeNull();
        expect(LocalCollectionDAO.getLastSyncAttempt(uri, resolver)).toEqual(100L);
        expect(LocalCollectionDAO.getLastSyncAttempt(Uri.parse("notfound"), resolver)).toEqual(-1L);
    }

    @Test
    public void shouldgetLastSyncSuccess() throws Exception {
        final Uri uri = Uri.parse("foo");
        LocalCollection c = LocalCollectionDAO.insertLocalCollection(uri, 1, 1, 100, 0, null, resolver);
        expect(c).not.toBeNull();
        expect(LocalCollectionDAO.getLastSyncSuccess(uri, resolver)).toEqual(100L);
        expect(LocalCollectionDAO.getLastSyncSuccess(Uri.parse("notfound"), resolver)).toEqual(-1L);
    }

    @Test
    public void shouldUpdateLastSyncTime() throws Exception {
        final Uri uri = Uri.parse("foo");
        LocalCollection c = LocalCollectionDAO.insertLocalCollection(uri, resolver);
        c.updateLastSyncSuccessTime(200, resolver);
        expect(LocalCollectionDAO.fromContentUri(uri, resolver, true).last_sync_success).toEqual(200L);
    }

    @Test
    public void shouldForceToStale() throws Exception {
        final Uri uri = Uri.parse("foo");
        LocalCollection c = LocalCollectionDAO.insertLocalCollection(uri, resolver);
        c.updateLastSyncSuccessTime(200, resolver);
        expect(LocalCollectionDAO.fromContentUri(uri, resolver, true).last_sync_success).toEqual(200L);

        LocalCollectionDAO.forceToStale(uri, resolver);
        expect(LocalCollectionDAO.fromContentUri(uri, resolver, true).last_sync_success).toEqual(0L);
    }

    @Test
    public void shouldDeleteCollection() throws Exception {

        final Uri uri = Uri.parse("foo");
        expect(LocalCollectionDAO.insertLocalCollection(uri, resolver)).not.toBeNull();
        expect(LocalCollectionDAO.deleteUri(uri, resolver)).toBeTrue();
        expect(LocalCollectionDAO.deleteUri(uri, resolver)).toBeFalse();
        expect(Content.COLLECTIONS).toHaveCount(0);
    }

    @Test
    public void shouldChangeAutoRefresh() throws Exception {
        Uri uri = Content.ME_LIKES.uri;
        LocalCollection lc = LocalCollectionDAO.insertLocalCollection(uri, 0, 1, 0, 100, null, resolver);
        expect(lc.shouldAutoRefresh()).toBeTrue();
        lc.updateSyncState(LocalCollection.SyncState.SYNCING, resolver);
        expect(LocalCollectionDAO.fromContentUri(uri, resolver, false).shouldAutoRefresh()).toBeFalse();
    }
}
