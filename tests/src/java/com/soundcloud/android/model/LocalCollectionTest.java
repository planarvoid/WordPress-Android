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
        LocalCollection c = LocalCollection.insertLocalCollection(resolver, uri);
        expect(c.uri).toEqual(uri);
        expect(c.last_sync).toBe(-1L);
        expect(c.size).toBe(-1);
    }

    @Test
    public void shouldInsertCollectionWithParams() throws Exception {
        final Uri uri = Uri.parse("foo");
        LocalCollection c = LocalCollection.insertLocalCollection(resolver, uri, 1, 2);
        expect(c.uri).toEqual(uri);
        expect(c.last_sync).toBe(1L);
        expect(c.size).toBe(2);
    }

    @Test
    public void shouldPersistLocalCollection() throws Exception {
        final Uri uri = Uri.parse("foo");
        LocalCollection c = LocalCollection.insertLocalCollection(resolver, uri, 100, 0);
        LocalCollection c2 = LocalCollection.fromContentUri(resolver, uri);

        expect(c.id).toEqual(c2.id);
        expect(c.uri).toEqual(c2.uri);
        expect(c.last_sync).toEqual(c2.last_sync);
        expect(c.size).toEqual(c2.size);
    }

    @Test
    public void shouldReturnNullIfCollectionNotFound() throws Exception {
        expect(LocalCollection.fromContentUri(resolver, Uri.parse("blaz"))).toBeNull();
    }

    @Test
    public void shouldgetLastSync() throws Exception {
        final Uri uri = Uri.parse("foo");
        LocalCollection c = LocalCollection.insertLocalCollection(resolver, uri, 100, 0);
        expect(c).not.toBeNull();
        expect(LocalCollection.getLastSync(resolver, uri)).toEqual(100L);
    }

    @Test
    public void shouldUpdateLastSyncTime() throws Exception {
        final Uri uri = Uri.parse("foo");
        LocalCollection c = LocalCollection.insertLocalCollection(resolver, uri);
        c.updateLastSyncTime(resolver, 200);
        expect(LocalCollection.fromContentUri(resolver, uri).last_sync).toEqual(200L);
    }
}
