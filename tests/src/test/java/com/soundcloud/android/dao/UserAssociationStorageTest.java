package com.soundcloud.android.dao;

import static com.soundcloud.android.Expect.expect;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import com.google.common.collect.ContiguousSet;
import com.google.common.collect.DiscreteDomain;
import com.google.common.collect.Lists;
import com.google.common.collect.Range;
import com.soundcloud.android.model.ScResource;
import com.soundcloud.android.model.User;
import com.soundcloud.android.provider.Content;
import com.soundcloud.android.robolectric.DefaultTestRunner;
import com.soundcloud.android.robolectric.TestHelper;
import com.soundcloud.android.task.collection.RemoteCollectionLoaderTest;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;

import android.content.ContentResolver;
import android.content.ContentValues;

import java.util.ArrayList;
import java.util.List;

@RunWith(DefaultTestRunner.class)
public class UserAssociationStorageTest {

    final static long USER_ID = 1L;
    public static final int BATCH_SIZE = 5;

    private ContentResolver resolver;
    private UserAssociationStorage storage;

    @Before
    public void before() {
        TestHelper.setUserId(USER_ID);
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
        TestHelper.bulkInsertDummyIdsToUserAssociations(Content.ME_FOLLOWERS.uri, 107, USER_ID);
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

    @Test
    public void shouldInsertInSingleBatchIfCollectionIsSmallEnough() throws Exception {
        List<Long> ids = Lists.newArrayList(ContiguousSet.create(Range.closed(1L, (long) BATCH_SIZE), DiscreteDomain.longs()));

        ContentResolver resolver = Mockito.mock(ContentResolver.class);
        new UserAssociationStorage(resolver).insertInBatches(Content.ME_FOLLOWERS,USER_ID,ids,0, BATCH_SIZE);
        verify(resolver).bulkInsert(eq(Content.ME_FOLLOWERS.uri), any(ContentValues[].class));
        verifyNoMoreInteractions(resolver);
    }

    @Test
    public void shouldInsertInBatchesIfCollectionIsTooLarge() throws Exception {
        List<Long> ids = Lists.newArrayList(ContiguousSet.create(Range.closed(1L, (long) BATCH_SIZE * 2), DiscreteDomain.longs()));

        ContentResolver resolver = Mockito.mock(ContentResolver.class);
        new UserAssociationStorage(resolver).insertInBatches(Content.ME_FOLLOWERS, USER_ID, ids, 0, BATCH_SIZE);
        verify(resolver, times(2)).bulkInsert(eq(Content.ME_FOLLOWERS.uri), any(ContentValues[].class));
        verifyNoMoreInteractions(resolver);
    }

    private static List<User> createUsers() {
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
