package com.soundcloud.android.dao;

import static com.soundcloud.android.Expect.expect;

import com.soundcloud.android.model.ScResource;
import com.soundcloud.android.model.SoundAssociationHolder;
import com.soundcloud.android.model.SoundAssociationTest;
import com.soundcloud.android.model.User;
import com.soundcloud.android.provider.Content;
import com.soundcloud.android.provider.DBHelper;
import com.soundcloud.android.robolectric.DefaultTestRunner;
import com.soundcloud.android.robolectric.TestHelper;
import com.soundcloud.android.service.sync.ApiSyncerTest;
import com.soundcloud.android.task.collection.RemoteCollectionLoaderTest;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import android.content.ContentResolver;
import android.content.ContentValues;

import java.util.ArrayList;
import java.util.List;

@RunWith(DefaultTestRunner.class)
public class UserAssociationStorageTest {

    final static long USER_ID = 1L;

    private ContentResolver resolver;
    private UserAssociationStorage storage;

    @Before
    public void before() {
        DefaultTestRunner.application.setCurrentUserId(USER_ID);
        resolver = DefaultTestRunner.application.getContentResolver();
        storage = new UserAssociationStorage();
    }

    @Test
    public void shouldBulkInsertAssociations() throws Exception {
        List<User> items = createUsers();
        expect(storage.insertAssociations(items, Content.ME_FOLLOWERS.uri, USER_ID)).toEqual(4); // 2 users, associations
        expect(Content.ME_FOLLOWERS).toHaveCount(2);
    }

    @Test
    public void shouldGetLocalIds() throws Exception {
        final int SIZE = 107;
        ContentValues[] cv = new ContentValues[SIZE];
        for (int i = 0; i < SIZE; i++) {
            cv[i] = new ContentValues();
            cv[i].put(DBHelper.UserAssociations.POSITION, i);
            cv[i].put(DBHelper.UserAssociations.TARGET_ID, i);
            cv[i].put(DBHelper.UserAssociations.OWNER_ID, USER_ID);
        }

        resolver.bulkInsert(Content.ME_FOLLOWERS.uri, cv);
        List<Long> localIds = storage.getStoredIds(Content.ME_FOLLOWERS.uri);
        expect(localIds.size()).toEqual(107);
    }

    @Test
    public void shouldRemoveSyncedContentForLoggedInUser() throws Exception {
        ScResource.ScResourceHolder<User> followedUsers = TestHelper.readJson(ScResource.ScResourceHolder.class, RemoteCollectionLoaderTest.class, "me_followings.json");
        TestHelper.bulkInsertToUserAssociations(followedUsers.collection, Content.ME_FOLLOWINGS.uri);

        ScResource.ScResourceHolder<User> followers = TestHelper.readJson(ScResource.ScResourceHolder.class, RemoteCollectionLoaderTest.class, "me_followers.json");
        TestHelper.bulkInsertToUserAssociations(followers.collection, Content.ME_FOLLOWERS.uri);

        expect(Content.ME_FOLLOWINGS).toHaveCount(50);
        expect(Content.ME_FOLLOWERS).toHaveCount(50);

        storage.clear();
        expect(Content.ME_FOLLOWINGS).toHaveCount(0);
        expect(Content.ME_FOLLOWERS).toHaveCount(0);
    }

    public static List<User> createUsers() {
        List<User> items = new ArrayList<User>();

        User u1 = new User();
        u1.permalink = "u1";
        u1.id = 100L;

        User u2 = new User();
        u2.permalink = "u2";
        u2.id = 300L;

        items.add(u1);
        items.add(u2);
        return items;
    }
}
