package com.soundcloud.android.dao;

import static com.soundcloud.android.Expect.expect;
import static com.soundcloud.android.robolectric.TestHelper.addPendingHttpResponse;

import com.soundcloud.android.AndroidCloudAPI;
import com.soundcloud.android.model.Track;
import com.soundcloud.android.model.User;
import com.soundcloud.android.provider.Content;
import com.soundcloud.android.provider.DBHelper;
import com.soundcloud.android.robolectric.DefaultTestRunner;
import com.xtremelabs.robolectric.Robolectric;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import android.content.ContentResolver;
import android.content.ContentValues;

import java.util.ArrayList;
import java.util.List;

@RunWith(DefaultTestRunner.class)
public class CollectionStorageTest {
    final static long USER_ID = 1L;

    private ContentResolver resolver;
    private CollectionStorage storage;

    @Before
    public void before() {
        DefaultTestRunner.application.setCurrentUserId(USER_ID);
        resolver = DefaultTestRunner.application.getContentResolver();
        storage = new CollectionStorage(DefaultTestRunner.application);
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

        List<Long> localIds = storage.getLocalIds(Content.ME_LIKES, USER_ID);

        expect(localIds.size()).toEqual(107);
    }

    @Test
    public void shouldWriteMissingCollectionItems() throws Exception {
        addPendingHttpResponse(getClass(), "5_users.json");

        List<User> users = new ArrayList<User>();
        for (int i = 0; i < 2; i++){
            users.add(createUserWithId(i));
        }
        new UserDAO(resolver).createCollection(users);

        ArrayList<Long> ids = new ArrayList<Long>();
        for (long i = 0; i < 10; i++){
            ids.add(i);
        }

        AndroidCloudAPI api = (AndroidCloudAPI) Robolectric.application;
        int itemsStored = storage.fetchAndStoreMissingCollectionItems(api, ids, Content.USERS, false);
        expect(itemsStored).toEqual(5);
    }

    @Test
    public void shouldBulkInsertWithCollections() throws Exception {
        List<Track> items = createTracks();
        expect(storage.insertCollection(items, Content.ME_LIKES.uri, USER_ID)).toEqual(6);
        expect(storage.getLikesCount()).toEqual(2);
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
