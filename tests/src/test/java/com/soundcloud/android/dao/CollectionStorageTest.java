package com.soundcloud.android.dao;

import android.content.ContentResolver;
import android.content.ContentValues;
import com.soundcloud.android.model.ScResource;
import com.soundcloud.android.model.Track;
import com.soundcloud.android.model.User;
import com.soundcloud.android.provider.Content;
import com.soundcloud.android.provider.DBHelper;
import com.soundcloud.android.robolectric.DefaultTestRunner;
import com.xtremelabs.robolectric.Robolectric;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.List;

import static com.soundcloud.android.Expect.expect;
import static com.soundcloud.android.robolectric.TestHelper.addPendingHttpResponse;
import static junit.framework.Assert.fail;

@RunWith(DefaultTestRunner.class)
public class CollectionStorageTest {
    final static long USER_ID = 1L;

    private ContentResolver resolver;
    private CollectionStorage storage;

    @Before
    public void before() {
        DefaultTestRunner.application.setCurrentUserId(USER_ID);
        resolver = Robolectric.application.getContentResolver();
        storage = new CollectionStorage(resolver);
    }

    @Test
    public void shouldGetLocalIds() throws Exception {
        final int SIZE = 107;
        final long USER_ID = 1L;
        ContentValues[] cv = new ContentValues[SIZE];
        for (int i = 0; i < SIZE; i++) {
            cv[i] = new ContentValues();
            cv[i].put(DBHelper.CollectionItems.POSITION, i);
            cv[i].put(DBHelper.CollectionItems.ITEM_ID, i);
            cv[i].put(DBHelper.CollectionItems.USER_ID, USER_ID);
        }

        resolver.bulkInsert(Content.ME_LIKES.uri, cv);

        expect(storage.getLocalIds(Content.ME_LIKES, USER_ID, -1, -1).size()).toEqual(107);
        List<Long> localIds = storage.getLocalIds(Content.ME_LIKES, USER_ID, 50, -1);

        expect(localIds.size()).toEqual(57);
        expect(localIds.get(0)).toEqual(50L);

        localIds = storage.getLocalIds(Content.ME_LIKES, USER_ID, 100, 50);
        expect(localIds.size()).toEqual(7);
        expect(localIds.get(0)).toEqual(100L);
    }

    @Test
    public void shouldWriteMissingCollectionItems() throws Exception {
        addPendingHttpResponse(getClass(), "5_users.json");

        List<ScResource> users = new ArrayList<ScResource>();
        for (int i = 0; i < 2; i++){
            users.add(createUserWithId(i));
        }

        ArrayList<Long> ids = new ArrayList<Long>();
        for (long i = 0; i < 10; i++){
            ids.add(i);
        }

//        expect(manager.writeCollection(users, ScResource.CacheUpdateMode.MINI)).toEqual(2);
//        expect(manager.fetchMissingCollectionItems((AndroidCloudAPI) Robolectric.application, ids, Content.USERS, false, 5)).toEqual(5);
        fail("fix me");
    }


    @Test
    public void shouldBulkInsert() throws Exception {
//        List<ScResource> items = createModels();
//        expect(manager.writeCollection(items, ScResource.CacheUpdateMode.MINI)).toEqual(3);
        fail("fix me");
    }

    @Test
    public void shouldBulkInsertWithCollections() throws Exception {
//        List<Track> items = createTracks();
//        expect(manager.writeCollection(items, Content.ME_LIKES.uri, USER_ID, ScResource.CacheUpdateMode.MINI)).toEqual(6);
//
//        Cursor c = resolver.query(Content.ME_LIKES.uri, null, null, null, null);
//        expect(c.getCount()).toEqual(2);
        fail("fix me");
    }

    private List<ScResource> createModels() {
        List<ScResource> items = new ArrayList<ScResource>();

        User u1 = new User();
        u1.permalink = "u1";
        u1.id = 100L;

        Track t = new Track();
        t.id = 200L;
        t.user = u1;

        User u2 = new User();
        u2.permalink = "u2";
        u2.id = 300L;

        User u2_ = new User();
        u2_.permalink = "u2";
        u2_.id = 300L;

        items.add(u1);
        items.add(t);
        items.add(u2_);
        return items;
    }


    public static List<Track> createTracks() {
        List<Track> items = new ArrayList<Track>();

        User u1 = new User();
        u1.permalink = "u1";
        u1.id = 100L;

        Track t = new Track();
        t.id = 200L;
        t.user = u1;

        User u2 = new User();
        u2.permalink = "u2";
        u2.id = 300L;

        Track t2 = new Track();
        t2.id = 400;
        t2.user = u2;

        items.add(t);
        items.add(t2);
        return items;
    }

    private User createUserWithId(long id){
        User u = new User();
        u.id = id;
        return u;
    }
}
