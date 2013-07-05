package com.soundcloud.android.dao;

import static com.soundcloud.android.Expect.expect;
import static com.soundcloud.android.robolectric.TestHelper.createUsers;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import com.google.common.base.Function;
import com.google.common.collect.ContiguousSet;
import com.google.common.collect.DiscreteDomain;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Range;
import com.soundcloud.android.model.Association;
import com.soundcloud.android.model.ScResource;
import com.soundcloud.android.model.SuggestedUser;
import com.soundcloud.android.model.User;
import com.soundcloud.android.model.UserAssociation;
import com.soundcloud.android.provider.Content;
import com.soundcloud.android.provider.DBHelper;
import com.soundcloud.android.robolectric.DefaultTestRunner;
import com.soundcloud.android.robolectric.TestHelper;
import com.soundcloud.android.task.collection.RemoteCollectionLoaderTest;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import rx.concurrency.Schedulers;

import android.content.ContentResolver;
import android.content.ContentValues;

import java.util.List;

@RunWith(DefaultTestRunner.class)
public class UserAssociationStorageTest {
    final private static long USER_ID = 1L;
    final private static int BATCH_SIZE = 5;
    final private static int INITIAL_FOLLOWERS_COUNT = 3;
    public static final String TOKEN = "12345";

    private User user;
    private ContentResolver resolver;
    private UserAssociationStorage storage;

    @Before
    public void before() {
        TestHelper.setUserId(USER_ID);
        resolver = DefaultTestRunner.application.getContentResolver();
        storage = new UserAssociationStorage(Schedulers.immediate(), resolver);
        user = new User(1);
    }

    @Test
    public void shouldMarkFollowingWithoutToken() throws Exception {
        user.followers_count = INITIAL_FOLLOWERS_COUNT;
        expect(storage.follow(user).toBlockingObservable().last().getUser()).toEqual(user);

        UserAssociation userAssociation = TestHelper.loadUserAssociation(Content.ME_FOLLOWINGS, user.getId());
        expect(userAssociation.getLocalSyncState()).toEqual(UserAssociation.LocalState.PENDING_ADDITION);
        expect(userAssociation.getToken()).toBeNull();
    }

    @Test
    public void shouldMarkFollowingAndStoreToken() throws Exception {
        SuggestedUser suggestedUser = TestHelper.getModelFactory().createModel(SuggestedUser.class);
        expect(storage.followSuggestedUser(suggestedUser).toBlockingObservable().last().getUser()).toEqual(new User(suggestedUser));

        UserAssociation userAssociation = TestHelper.loadUserAssociation(Content.ME_FOLLOWINGS, suggestedUser.getId());
        expect(userAssociation.getLocalSyncState()).toEqual(UserAssociation.LocalState.PENDING_ADDITION);
        expect(userAssociation.getToken()).toEqual(suggestedUser.getToken());
    }

    @Test
    public void shouldMarkFollowingForAdditionAndUpdateFollowersCount() throws Exception {
        user.followers_count = INITIAL_FOLLOWERS_COUNT;
        expect(storage.follow(user).toBlockingObservable().last().getUser()).toEqual(user);

        expect(Content.ME_FOLLOWINGS).toHaveCount(1);
        expect(TestHelper.reload(user).followers_count).toBe(INITIAL_FOLLOWERS_COUNT + 1);

        UserAssociation userAssociation = TestHelper.loadUserAssociation(Content.ME_FOLLOWINGS, user.getId());
        expect(userAssociation.getLocalSyncState()).toEqual(UserAssociation.LocalState.PENDING_ADDITION);
    }

    @Test
    public void shouldFailToMarkExistingFollowingForAdditionAndNotUpdateFollowersCount() {
        user.followers_count = INITIAL_FOLLOWERS_COUNT;
        TestHelper.insertAsUserAssociation(user, UserAssociation.Type.FOLLOWING);
        expect(Content.ME_FOLLOWINGS).toHaveCount(1);

        storage.follow(user);
        expect(Content.ME_FOLLOWINGS).toHaveCount(1);
        expect(TestHelper.reload(user).followers_count).toBe(INITIAL_FOLLOWERS_COUNT);
    }

    @Test
    public void shouldMarkFollowingForRemovalAndUpdateFollowingCount() throws Exception {
        user.followers_count = INITIAL_FOLLOWERS_COUNT;
        TestHelper.insertAsUserAssociation(user, UserAssociation.Type.FOLLOWING);
        expect(Content.ME_FOLLOWINGS).toHaveCount(1);
        expect(TestHelper.reload(user).followers_count).toBe(INITIAL_FOLLOWERS_COUNT);
        storage.unfollow(user).toBlockingObservable().last();

        expect(Content.ME_FOLLOWINGS).toHaveCount(1);// should still exist but marked for removal
        expect(TestHelper.reload(user).followers_count).toBe(INITIAL_FOLLOWERS_COUNT - 1);
        UserAssociation userAssociation = TestHelper.loadUserAssociation(Content.ME_FOLLOWINGS, user.getId());
        expect(userAssociation.getLocalSyncState()).toEqual(UserAssociation.LocalState.PENDING_REMOVAL);
    }

    @Test
    public void shouldFailToMarkNonExistantFollowingForRemovalAndNotUpdateFollowerCount() {
        user.followers_count = INITIAL_FOLLOWERS_COUNT;
        TestHelper.insertWithDependencies(user);
        expect(TestHelper.reload(user).followers_count).toBe(INITIAL_FOLLOWERS_COUNT);

        storage.unfollow(user);
        expect(TestHelper.reload(user).followers_count).toBe(INITIAL_FOLLOWERS_COUNT);
    }

    @Test
    public void shouldBulkInsertAssociations() throws Exception {
        List<User> items = createUsers(2);
        expect(storage.insertAssociations(items, Content.ME_FOLLOWERS.uri, USER_ID)).toEqual(4); // 2 users, associations
        expect(Content.ME_FOLLOWERS).toHaveCount(2);
    }

    @Test
    public void shouldBulkInsertFollowings() throws Exception {
        final List<User> users = createUsers(3);
        final Iterable<UserAssociation> newAssociations = storage.followList(users).toBlockingObservable().toIterable();

        expect(Iterables.transform(newAssociations, new Function<UserAssociation, Object>() {
            @Override
            public Object apply(UserAssociation input) {
                return input.getUser();
            }
        })).toContainExactlyInAnyOrder(users.toArray(new User[3]));

        expect(Content.ME_FOLLOWINGS).toHaveCount(3);
        for (User user : users) {
            expect(TestHelper.getUserAssociationByTargetId(Content.ME_FOLLOWINGS.uri, user.getId()).getLocalSyncState())
                    .toBe(UserAssociation.LocalState.PENDING_ADDITION);
        }
    }

    @Test
    public void shouldBulkInsertFollowingsFromSuggestedUsers() throws Exception {
        final List<SuggestedUser> suggestedUsers = TestHelper.createSuggestedUsers(3);
        expect(storage.followSuggestedUserList(suggestedUsers).toBlockingObservable().last().getUser()).toEqual(new User(suggestedUsers.get(2)));
        expect(Content.ME_FOLLOWINGS).toHaveCount(3);

        for (SuggestedUser suggestedUser : suggestedUsers) {
            final UserAssociation userAssociationByTargetId = TestHelper.getUserAssociationByTargetId(Content.ME_FOLLOWINGS.uri, suggestedUser.getId());
            expect(userAssociationByTargetId.getLocalSyncState()).toBe(UserAssociation.LocalState.PENDING_ADDITION);
            expect(userAssociationByTargetId.getToken()).not.toBeNull();
        }
    }

    @Test
    public void shouldBulkMarkFollowingsForRemoval() throws Exception {
        final List<User> users = createUsers(3);
        storage.unfollowList(users).toBlockingObservable().last();
        expect(Content.ME_FOLLOWINGS).toHaveCount(3);
        for (User user : users) {
            expect(TestHelper.getUserAssociationByTargetId(Content.ME_FOLLOWINGS.uri, user.getId()).getLocalSyncState())
                    .toBe(UserAssociation.LocalState.PENDING_REMOVAL);
        }
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
        new UserAssociationStorage(Schedulers.immediate(), resolver).insertInBatches(Content.ME_FOLLOWERS, USER_ID, ids, 0, BATCH_SIZE);
        verify(resolver).bulkInsert(eq(Content.ME_FOLLOWERS.uri), any(ContentValues[].class));
        verifyNoMoreInteractions(resolver);
    }

    @Test
    public void shouldInsertInBatchesIfCollectionIsTooLarge() throws Exception {
        List<Long> ids = Lists.newArrayList(ContiguousSet.create(Range.closed(1L, (long) BATCH_SIZE * 2), DiscreteDomain.longs()));

        ContentResolver resolver = mock(ContentResolver.class);
        new UserAssociationStorage(Schedulers.immediate(), resolver).insertInBatches(Content.ME_FOLLOWERS, USER_ID, ids, 0, BATCH_SIZE);
        verify(resolver, times(2)).bulkInsert(eq(Content.ME_FOLLOWERS.uri), any(ContentValues[].class));
        verifyNoMoreInteractions(resolver);
    }

    @Test
    public void shouldSetCorrectInsertPositionOfPositionColumnWhenInsertingSingleBatch() {
        List<Long> ids = Lists.newArrayList(ContiguousSet.create(Range.closed(1L, (long) BATCH_SIZE), DiscreteDomain.longs()));

        int START_POSITION = 27;
        ContentResolver resolver = mock(ContentResolver.class);
        new UserAssociationStorage(Schedulers.immediate(), resolver).insertInBatches(Content.ME_FOLLOWERS, USER_ID, ids, START_POSITION, BATCH_SIZE);

        ArgumentCaptor<ContentValues[]> argumentCaptor = ArgumentCaptor.forClass(ContentValues[].class);
        verify(resolver).bulkInsert(eq(Content.ME_FOLLOWERS.uri), argumentCaptor.capture());
        ContentValues[] contentValuesArr = argumentCaptor.getValue();
        int counter = 0;
        for (ContentValues contentValues : contentValuesArr) {
            expect(contentValues.getAsInteger(DBHelper.UserAssociations.POSITION)).toEqual(START_POSITION + counter++);
        }
        expect(counter).toEqual(ids.size());
    }

    @Test
    public void shouldSetCorrectInsertPositionOfPositionColumnWhenInsertingMultipleBatches() {
        List<Long> ids = Lists.newArrayList(ContiguousSet.create(Range.closed(1L, (long) BATCH_SIZE*2), DiscreteDomain.longs()));

        int START_POSITION = 66;
        ContentResolver resolver = mock(ContentResolver.class);
        new UserAssociationStorage(Schedulers.immediate(), resolver).insertInBatches(Content.ME_FOLLOWERS, USER_ID, ids, START_POSITION, BATCH_SIZE);

        ArgumentCaptor<ContentValues[]> argumentCaptor = ArgumentCaptor.forClass(ContentValues[].class);
        verify(resolver, times(2)).bulkInsert(eq(Content.ME_FOLLOWERS.uri), argumentCaptor.capture());
        List<ContentValues[]> contentValues = argumentCaptor.getAllValues();
        int counter = 0;
        for (ContentValues[] contentValue : contentValues) {
            for(ContentValues values : contentValue){
                expect(values.getAsInteger(DBHelper.UserAssociations.POSITION)).toEqual(START_POSITION + counter++);
            }
        }
        expect(counter).toEqual(ids.size());
    }


    @Test
    public void shouldQueryFollowings() {
        TestHelper.bulkInsertToUserAssociations(createUsers(2), Content.ME_FOLLOWINGS.uri);
        expect(Content.ME_FOLLOWINGS).toHaveCount(2);
        expect(Iterables.size(storage.getFollowings().toBlockingObservable().toIterable())).toEqual(2);
    }

    @Test
    public void shouldQueryFollowingsAndExemptFollowingsMarkedForDeletion() {
        final List<User> users = createUsers(2);
        TestHelper.bulkInsertToUserAssociations(users.subList(0, 1), Content.ME_FOLLOWINGS.uri);
        TestHelper.bulkInsertToUserAssociationsAsRemovals(users.subList(1, 2), Content.ME_FOLLOWINGS.uri);
        expect(Content.ME_FOLLOWINGS).toHaveCount(2);
        expect(Iterables.size(storage.getFollowings().toBlockingObservable().toIterable())).toEqual(1);
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
        TestHelper.bulkInsertToUserAssociations(users.subList(0, 1), Content.ME_FOLLOWINGS.uri);
        TestHelper.bulkInsertToUserAssociationsAsAdditions(users.subList(1, 2), Content.ME_FOLLOWINGS.uri);
        expect(Content.ME_FOLLOWINGS).toHaveCount(2);
        expect(storage.getFollowingsNeedingSync().size()).toEqual(1);
    }

    @Test
    public void shouldQueryUnsyncedFollowingAdditionWithToken() {
        final List<User> users = createUsers(3);
        TestHelper.bulkInsertToUserAssociations(users.subList(0, 1), Content.ME_FOLLOWINGS.uri);
        TestHelper.bulkInsertToUserAssociationsAsAdditions(users.subList(1, 2), Content.ME_FOLLOWINGS.uri);
        TestHelper.bulkInsertToUserAssociationsAsAdditionsWithToken(users.subList(2, 3), Content.ME_FOLLOWINGS.uri, TOKEN);

        expect(Content.ME_FOLLOWINGS).toHaveCount(3);
        final List<UserAssociation> followingsNeedingSync = storage.getFollowingsNeedingSync();
        expect(followingsNeedingSync.size()).toEqual(2);
        expect(followingsNeedingSync.get(0).getToken()).toBeNull();
        expect(followingsNeedingSync.get(1).getToken()).toEqual(TOKEN);
    }

    @Test
    public void shouldQueryUnsyncedFollowingAdditionAndRemoval() {
        TestHelper.bulkInsertToUserAssociations(createUsers(2), Content.ME_FOLLOWINGS.uri);
        expect(Content.ME_FOLLOWINGS).toHaveCount(2);

        List<UserAssociation> userAssociations = Lists.newArrayList(storage.getFollowings().toBlockingObservable().getIterator());

        UserAssociation association = userAssociations.get(0);
        association.markForAddition();
        expect(new UserAssociationDAO(resolver).update(association)).toBeTrue();

        association = userAssociations.get(1);
        association.markForRemoval();
        expect(new UserAssociationDAO(resolver).update(association)).toBeTrue();

        expect(storage.getFollowingsNeedingSync().size()).toEqual(2);
    }

    @Test
    public void shouldNotClearAnySyncFlag() {
        TestHelper.bulkInsertToUserAssociations(createUsers(2), Content.ME_FOLLOWINGS.uri);
        expect(Content.ME_FOLLOWINGS).toHaveCount(2);

        UserAssociation association = storage.getFollowings().toBlockingObservable().last();
        expect(storage.setFollowingAsSynced(association)).toBeFalse();
    }

    @Test
    public void shouldClearSyncFlagForAddition() throws Exception {
        final List<User> users = createUsers(2);
        final Long userId = users.get(0).getId();

        TestHelper.bulkInsertToUserAssociationsAsAdditions(users, Content.ME_FOLLOWINGS.uri);
        expect(Content.ME_FOLLOWINGS).toHaveCount(2);

        UserAssociation association = TestHelper.loadUserAssociation(Content.ME_FOLLOWINGS, userId);
        expect(association.getLocalSyncState()).toEqual(UserAssociation.LocalState.PENDING_ADDITION);
        expect(storage.setFollowingAsSynced(association)).toBeTrue();

        association = TestHelper.loadUserAssociation(Content.ME_FOLLOWINGS, userId);
        expect(association.getLocalSyncState()).toEqual(UserAssociation.LocalState.NONE);
    }

    @Test
    public void shouldClearSyncFlagAndTokenForAddition() throws Exception {
        final List<User> users = createUsers(2);
        final Long userId = users.get(0).getId();

        TestHelper.bulkInsertToUserAssociationsAsAdditionsWithToken(users, Content.ME_FOLLOWINGS.uri, TOKEN);
        expect(Content.ME_FOLLOWINGS).toHaveCount(2);

        UserAssociation association = TestHelper.loadUserAssociation(Content.ME_FOLLOWINGS, userId);
        expect(association.getLocalSyncState()).toEqual(UserAssociation.LocalState.PENDING_ADDITION);
        expect(association.getToken()).toEqual(TOKEN);

        expect(storage.setFollowingAsSynced(association)).toBeTrue();
        association = TestHelper.loadUserAssociation(Content.ME_FOLLOWINGS, userId);

        expect(association.getLocalSyncState()).toEqual(UserAssociation.LocalState.NONE);
        expect(association.getToken()).toBeNull();
    }

    @Test
    public void shouldClearSyncFlagForRemoval() throws Exception {
        final List<User> users = createUsers(2);
        final Long userId = users.get(0).getId();

        TestHelper.bulkInsertToUserAssociationsAsRemovals(users, Content.ME_FOLLOWINGS.uri);
        expect(Content.ME_FOLLOWINGS).toHaveCount(2);

        UserAssociation association = TestHelper.loadUserAssociation(Content.ME_FOLLOWINGS, userId);
        expect(association.getLocalSyncState()).toEqual(UserAssociation.LocalState.PENDING_REMOVAL);

        expect(storage.setFollowingAsSynced(association)).toBeTrue();
        expect(storage.getFollowingsNeedingSync().size()).toEqual(1);
        expect(Content.ME_FOLLOWINGS).toHaveCount(1);
    }

    @Test
    public void shouldDeleteFollowingsList() throws Exception {
        TestHelper.bulkInsertToUserAssociationsAsRemovals(createUsers(2), Content.ME_FOLLOWINGS.uri);
        expect(Content.ME_FOLLOWINGS).toHaveCount(2);
        expect(storage.deleteFollowings(TestHelper.loadUserAssociations(Content.ME_FOLLOWINGS))).toBeTrue();
        expect(Content.ME_FOLLOWINGS).toHaveCount(0);
    }
}
