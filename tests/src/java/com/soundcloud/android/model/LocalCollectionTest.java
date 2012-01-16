package com.soundcloud.android.model;

import static com.soundcloud.android.Expect.expect;

import com.soundcloud.android.robolectric.DefaultTestRunner;
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
        expect(c.last_sync).toBe(-1L);
        expect(c.size).toBe(-1);
    }

    @Test
    public void shouldInsertCollectionWithParams() throws Exception {
        final Uri uri = Uri.parse("foo");
        LocalCollection c = LocalCollection.insertLocalCollection(uri, "some-state", 1, 2, resolver);
        expect(c.uri).toEqual(uri);
        expect(c.last_sync).toBe(1L);
        expect(c.size).toBe(2);
        expect(c.sync_state).toEqual("some-state");

        expect(c).toEqual(LocalCollection.fromContentUri(uri, resolver));
    }

    @Test
    public void shouldSupportEqualsAndHashcode() throws Exception {
        LocalCollection c1 = new LocalCollection(1, Uri.parse("foo"), 1, null, 0);
        LocalCollection c2 = new LocalCollection(1, Uri.parse("foo"), 1, null, 0);
        LocalCollection c3 = new LocalCollection(100, Uri.parse("foo"), 1, null, 0);
        expect(c1).toEqual(c2);
        expect(c2).not.toEqual(c3);
    }

    @Test
    public void shouldPersistLocalCollection() throws Exception {
        final Uri uri = Uri.parse("foo");
        LocalCollection c = LocalCollection.insertLocalCollection(uri, "some-state", 100, 0, resolver);
        LocalCollection c2 = LocalCollection.fromContentUri(uri, resolver);

        expect(c.id).toEqual(c2.id);
        expect(c.uri).toEqual(c2.uri);
        expect(c.last_sync).toEqual(c2.last_sync);
        expect(c.size).toEqual(c2.size);
    }

    @Test
    public void shouldReturnNullIfCollectionNotFound() throws Exception {
        expect(LocalCollection.fromContentUri(Uri.parse("blaz"), resolver)).toBeNull();
    }

    @Test
    public void shouldgetLastSync() throws Exception {
        final Uri uri = Uri.parse("foo");
        LocalCollection c = LocalCollection.insertLocalCollection(uri, null, 100, 0, resolver);
        expect(c).not.toBeNull();
        expect(LocalCollection.getLastSync(uri, resolver)).toEqual(100L);
        expect(LocalCollection.getLastSync(Uri.parse("notfound"), resolver)).toEqual(-1L);
    }

    @Test
    public void shouldUpdateLastSyncTime() throws Exception {
        final Uri uri = Uri.parse("foo");
        LocalCollection c = LocalCollection.insertLocalCollection(uri, resolver);
        c.updateLastSyncTime(200, resolver);
        expect(LocalCollection.fromContentUri(uri, resolver).last_sync).toEqual(200L);
    }
}
