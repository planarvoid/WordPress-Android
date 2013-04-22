package com.soundcloud.android.dao;

import static com.soundcloud.android.Expect.expect;

import com.soundcloud.android.model.LocalCollection;
import com.soundcloud.android.provider.Content;
import com.xtremelabs.robolectric.Robolectric;
import org.junit.Test;

import android.net.Uri;

public class LocalCollectionDAOTest extends AbstractDAOTest<LocalCollectionDAO> {
    public LocalCollectionDAOTest() {
        super(new LocalCollectionDAO(Robolectric.application.getContentResolver()));
    }

    @Test
    public void shouldInsertCollection() throws Exception {
        final Uri uri = Uri.parse("foo");
        LocalCollection c = new LocalCollection(uri, -1, -1, 0, -1, null);
        expect(getDAO().create(c)).toBeGreaterThan(0L);

        expect(c.uri).toEqual(uri);
        expect(c.last_sync_attempt).toBe(-1L);
        expect(c.last_sync_success).toBe(-1L);
        expect(c.size).toBe(-1);
    }

    @Test
    public void shouldCreateCollection() throws Exception {
        final Uri foo = Uri.parse("foo");
        LocalCollection lc = new LocalCollection(foo, 0, 0, 0, 0, null);

        expect(getDAO().create(lc)).toBeGreaterThan(0L);
        expect(getDAO().deleteUri(foo)).toBeTrue();
        expect(getDAO().deleteUri(foo)).toBeFalse();
        expect(Content.COLLECTIONS).toHaveCount(0);
    }

    @Test
    public void shouldInsertCollectionWithParams() throws Exception {
        final Uri uri = Uri.parse("foo");
        LocalCollection c = new LocalCollection(uri, 1, 1, 1, 2, "some-extra");
        getDAO().create(c);
        expect(c.uri).toEqual(uri);
        expect(c.last_sync_attempt).toBe(1L);
        expect(c.last_sync_success).toBe(1L);
        expect(c.size).toBe(2);
        expect(c.sync_state).toEqual(1);
        expect(c.extra).toEqual("some-extra");
        expect(c).toEqual(getDAO().fromContentUri(uri, true));
    }


    @Test
    public void shouldPersistLocalCollection() throws Exception {
        final Uri uri = Uri.parse("foo");
        LocalCollection c = new LocalCollection(uri, 1, 100, 1, 0, "some-extra");
        getDAO().create(c);
        LocalCollection c2 = getDAO().fromContentUri(uri, true);

        expect(c.id).toEqual(c2.id);
        expect(c.uri).toEqual(c2.uri);
        expect(c.last_sync_attempt).toEqual(c2.last_sync_attempt);
        expect(c.last_sync_success).toEqual(c2.last_sync_success);
        expect(c.size).toEqual(c2.size);

        expect(Content.COLLECTIONS).toHaveCount(1);
    }

    @Test
    public void shouldReturnNullIfCollectionNotFound() throws Exception {
        expect(getDAO().fromContentUri(Uri.parse("blaz"), false)).toBeNull();
        expect(getDAO().fromContentUri(Uri.parse("blaz"), true)).not.toBeNull();
    }


}
