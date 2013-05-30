package com.soundcloud.android.dao;

import static com.soundcloud.android.Expect.expect;
import static com.soundcloud.android.robolectric.TestHelper.createUsers;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import com.google.common.collect.ContiguousSet;
import com.google.common.collect.DiscreteDomain;
import com.google.common.collect.Lists;
import com.google.common.collect.Range;
import com.soundcloud.android.model.Association;
import com.soundcloud.android.model.ScResource;
import com.soundcloud.android.model.User;
import com.soundcloud.android.model.UserAssociation;
import com.soundcloud.android.provider.Content;
import com.soundcloud.android.robolectric.DefaultTestRunner;
import com.soundcloud.android.robolectric.TestHelper;
import com.soundcloud.android.task.collection.RemoteCollectionLoaderTest;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import android.content.ContentResolver;
import android.content.ContentValues;

import java.util.Date;
import java.util.List;

@RunWith(DefaultTestRunner.class)
public class UserAssociationStorageTest {
    final private static long USER_ID = 1L;
    final private static int BATCH_SIZE = 5;
    final private static int INITIAL_FOLLOWERS_COUNT = 3;

    private User user;
    private ContentResolver resolver;
    private UserAssociationStorage storage;

    @Before
    public void before() {
        DefaultTestRunner.application.setCurrentUserId(USER_ID);
        resolver = DefaultTestRunner.application.getContentResolver();
        storage = new UserAssociationStorage();
        user = new User(1);
    }

    @Test
    public void shouldMarkFollowingForAdditionAndUpdateFollowersCount() {
        user.followers_count = INITIAL_FOLLOWERS_COUNT;
        storage.addFollowing(user);

        expect(Content.ME_FOLLOWINGS).toHaveCount(1);
        expect(TestHelper.reload(user).followers_count).toBe(INITIAL_FOLLOWERS_COUNT + 1);

        final UserAssociation userAssociation = UserAssociationDAO.forContent(Content.ME_FOLLOWINGS, resolver).query(user.id);
        expect(userAssociation.getLocalSyncState()).toEqual(UserAssociation.LocalState.PENDING_ADDITION);
    }

    @Test
    public void shouldFailToMarkExistingFollowingForAdditionAndNotUpdateFollowersCount() {
        user.followers_count = INITIAL_FOLLOWERS_COUNT;
        TestHelper.insertAsUserAssociation(user, UserAssociation.Type.FOLLOWING);
        expect(Content.ME_FOLLOWINGS).toHaveCount(1);

        storage.addFollowing(user);
        expect(Content.ME_FOLLOWINGS).toHaveCount(1);
        expect(TestHelper.reload(user).followers_count).toBe(INITIAL_FOLLOWERS_COUNT);
    }

    @Test
    public void shouldMarkFollowingForRemovalAndUpdateFollowingCount() {
        user.followers_count = INITIAL_FOLLOWERS_COUNT;
        TestHelper.insertAsUserAssociation(user, UserAssociation.Type.FOLLOWING);
        expect(Content.ME_FOLLOWINGS).toHaveCount(1);
        expect(TestHelper.reload(user).followers_count).toBe(INITIAL_FOLLOWERS_COUNT);
        storage.removeFollowing(user);

        expect(Content.ME_FOLLOWINGS).toHaveCount(1);// should still exist but marked for removal
        expect(TestHelper.reload(user).followers_count).toBe(INITIAL_FOLLOWERS_COUNT - 1);
        final UserAssociation userAssociation = UserAssociationDAO.forContent(Content.ME_FOLLOWINGS, resolver).query(user.id);
        expect(userAssociation.getLocalSyncState()).toEqual(UserAssociation.LocalState.PENDING_REMOVAL);
    }

    @Test
    public void shouldFailToMarkNonExistantFollowingForRemovalAndNotUpdateFollowerCount() {
        user.followers_count = INITIAL_FOLLOWERS_COUNT;
        TestHelper.insertWithDependencies(user);
        expect(TestHelper.reload(user).followers_count).toBe(INITIAL_FOLLOWERS_COUNT);

        storage.removeFollowing(user);
        expect(TestHelper.reload(user).followers_count).toBe(INITIAL_FOLLOWERS_COUNT);
    }

    @Test
    public void shouldBulkInsertAssociations() throws Exception {
        List<User> items = createUsers(2);
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
    public void shouldGetLocalFollowingIdsWithExemptedRemoval() throws Exception {
        TestHelper.bulkInsertDummyIdsToUserAssociations(Content.ME_FOLLOWINGS.uri, 107, USER_ID);
        UserAssociation associationForRemoval = new UserAssociation(Association.Type.FOLLOWING, new User(1000L));
        associationForRemoval.markForRemoval();
        TestHelper.insertWithDependencies(Content.USER_ASSOCIATIONS.uri, associationForRemoval);

        List<Long> localIds = storage.getStoredIds(Content.ME_FOLLOWINGS.uri);
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

        ContentResolver resolver = mock(ContentResolver.class);
        new UserAssociationStorage(resolver).insertInBatches(Content.ME_FOLLOWERS,USER_ID,ids,0, BATCH_SIZE);
        verify(resolver).bulkInsert(eq(Content.ME_FOLLOWERS.uri), any(ContentValues[].class));
        verifyNoMoreInteractions(resolver);
    }

    @Test
    public void shouldInsertInBatchesIfCollectionIsTooLarge() throws Exception {
        List<Long> ids = Lists.newArrayList(ContiguousSet.create(Range.closed(1L, (long) BATCH_SIZE * 2), DiscreteDomain.longs()));

        ContentResolver resolver = mock(ContentResolver.class);
        new UserAssociationStorage(resolver).insertInBatches(Content.ME_FOLLOWERS, USER_ID, ids, 0, BATCH_SIZE);
        verify(resolver, times(2)).bulkInsert(eq(Content.ME_FOLLOWERS.uri), any(ContentValues[].class));
        verifyNoMoreInteractions(resolver);
    }

    @Test
    public void shouldQueryFollowings(){
        TestHelper.bulkInsertToUserAssociations(createUsers(2), Content.ME_FOLLOWINGS.uri);
        expect(Content.ME_FOLLOWINGS).toHaveCount(2);
        expect(storage.getFollowings().size()).toEqual(2);
    }

    @Test
    public void shouldQueryFollowingsAndExemptFollowingsMarkedForDeletion() {
        final List<User> users = createUsers(2);
        TestHelper.bulkInsertToUserAssociations(users.subList(0, 1), Content.ME_FOLLOWINGS.uri);
        TestHelper.bulkInsertToUserAssociationsAsRemovals(users.subList(1, 2), Content.ME_FOLLOWINGS.uri);
        expect(Content.ME_FOLLOWINGS).toHaveCount(2);
        expect(storage.getFollowings().size()).toEqual(1);
    }

    @Test
    public void shouldNotHaveUnsyncedFollowings() {
        TestHelper.bulkInsertToUserAssociations(createUsers(2), Content.ME_FOLLOWINGS.uri);
        expect(Content.ME_FOLLOWINGS).toHaveCount(2);
        expect(storage.hasFollowingsNeedingSync()).toBeFalse();
    }

    @Test
    public void shouldHaveAnUnsyncedRemoval() {
        final List<User> users = createUsers(2);
        TestHelper.bulkInsertToUserAssociations(users.subList(0, 1), Content.ME_FOLLOWINGS.uri);
        TestHelper.bulkInsertToUserAssociationsAsRemovals(users.subList(1, 2), Content.ME_FOLLOWINGS.uri);
        expect(Content.ME_FOLLOWINGS).toHaveCount(2);
        expect(storage.hasFollowingsNeedingSync()).toBeTrue();
    }

    @Test
    public void shouldHaveAnUnsyncedAddition() {
        final List<User> users = createUsers(2);
        TestHelper.bulkInsertToUserAssociations(users.subList(0, 1), Content.ME_FOLLOWINGS.uri);
        TestHelper.bulkInsertToUserAssociationsAsAdditions(users.subList(1, 2), Content.ME_FOLLOWINGS.uri);
        expect(Content.ME_FOLLOWINGS).toHaveCount(2);
        expect(storage.hasFollowingsNeedingSync()).toBeTrue();
    }

    @Test
    public void shouldQueryUnsyncedFollowingRemoval() {
        final List<User> users = createUsers(2);
        TestHelper.bulkInsertToUserAssociations(users.subList(0, 1), Content.ME_FOLLOWINGS.uri);
        TestHelper.bulkInsertToUserAssociationsAsRemovals(users.subList(1, 2), Content.ME_FOLLOWINGS.uri);
        expect(storage.getFollowingsNeedingSync().size()).toEqual(1);
    }

    @Test
    public void shouldQueryUnsyncedFollowingAddition() {
        final List<User> users = createUsers(2);
        TestHelper.bulkInsertToUserAssociations(users.subList(0,1), Content.ME_FOLLOWINGS.uri);
        TestHelper.bulkInsertToUserAssociationsAsAdditions(users.subList(1, 2), Content.ME_FOLLOWINGS.uri);
        expect(Content.ME_FOLLOWINGS).toHaveCount(2);
        expect(storage.getFollowingsNeedingSync().size()).toEqual(1);
    }

    @Test
    public void shouldQueryUnsyncedFollowingAdditionAndRemoval() {
        TestHelper.bulkInsertToUserAssociations(createUsers(2), Content.ME_FOLLOWINGS.uri);
        expect(Content.ME_FOLLOWINGS).toHaveCount(2);

        UserAssociation association = storage.getFollowings().get(0);
        association.markForAddition();
        expect(new UserAssociationDAO(resolver).update(association)).toBeTrue();

        association = storage.getFollowings().get(1);
        association.markForRemoval();
        expect(new UserAssociationDAO(resolver).update(association)).toBeTrue();

        expect(storage.getFollowingsNeedingSync().size()).toEqual(2);
    }

    @Test
    public void shouldNotClearAnySyncFlag() {
        TestHelper.bulkInsertToUserAssociations(createUsers(2), Content.ME_FOLLOWINGS.uri);
        expect(Content.ME_FOLLOWINGS).toHaveCount(2);

        UserAssociation association = storage.getFollowings().get(0);
        expect(storage.setFollowingAsSynced(association)).toBeFalse();
    }

    @Test
    public void shouldClearSyncFlagForAddition() {
        TestHelper.bulkInsertToUserAssociations(createUsers(2), Content.ME_FOLLOWINGS.uri);
        expect(Content.ME_FOLLOWINGS).toHaveCount(2);

        UserAssociation association = storage.getFollowings().get(0);
        association.markForAddition();
        expect(new UserAssociationDAO(resolver).update(association)).toBeTrue();
        expect(storage.getFollowingsNeedingSync().size()).toEqual(1);

        expect(storage.setFollowingAsSynced(association)).toBeTrue();
        expect(storage.getFollowingsNeedingSync().size()).toEqual(0);
    }

    @Test
    public void shouldClearSyncFlagForRemoval() {
        TestHelper.bulkInsertToUserAssociations(createUsers(2), Content.ME_FOLLOWINGS.uri);
        expect(Content.ME_FOLLOWINGS).toHaveCount(2);

        UserAssociation association = storage.getFollowings().get(0);
        association.markForRemoval();
        expect(new UserAssociationDAO(resolver).update(association)).toBeTrue();
        expect(storage.getFollowingsNeedingSync().size()).toEqual(1);

        expect(storage.setFollowingAsSynced(association)).toBeTrue();
        expect(storage.getFollowingsNeedingSync().size()).toEqual(0);
        expect(Content.ME_FOLLOWINGS).toHaveCount(1);
        expect(UserAssociationDAO.forContent(Content.ME_FOLLOWINGS, resolver).query(association.getUser().id)).toBeNull();
    }
}
