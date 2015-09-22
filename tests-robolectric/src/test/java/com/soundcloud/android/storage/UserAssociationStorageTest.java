package com.soundcloud.android.storage;

import static com.soundcloud.android.Expect.expect;
import static com.soundcloud.java.collections.Lists.newArrayList;
import static java.util.Arrays.asList;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import com.soundcloud.android.api.legacy.model.Association;
import com.soundcloud.android.api.legacy.model.PublicApiResource;
import com.soundcloud.android.api.legacy.model.PublicApiUser;
import com.soundcloud.android.api.legacy.model.UserAssociation;
import com.soundcloud.android.collections.tasks.RemoteCollectionLoaderTest;
import com.soundcloud.android.robolectric.DefaultTestRunner;
import com.soundcloud.android.storage.provider.Content;
import com.soundcloud.android.testsupport.TestHelper;
import com.soundcloud.android.testsupport.fixtures.ModelFixtures;
import com.soundcloud.java.collections.Iterables;
import com.soundcloud.java.functions.Function;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import rx.schedulers.Schedulers;

import android.content.ContentResolver;
import android.content.ContentValues;

import java.util.Collections;
import java.util.List;

@RunWith(DefaultTestRunner.class)
public class UserAssociationStorageTest {
    final private static long USER_ID = 1L;
    final private static int BATCH_SIZE = 1;
    final private static int INITIAL_FOLLOWERS_COUNT = 3;
    public static final String TOKEN = "12345";

    private PublicApiUser user;
    private ContentResolver resolver;
    private LegacyUserAssociationStorage storage;

    @Before
    public void before() {
        TestHelper.setUserId(USER_ID);
        resolver = DefaultTestRunner.application.getContentResolver();
        storage = new LegacyUserAssociationStorage(Schedulers.immediate(), resolver);
        user = new PublicApiUser(1);
    }

    @Test
    public void shouldMarkFollowingWithoutToken() throws Exception {
        user.followers_count = INITIAL_FOLLOWERS_COUNT;
        expect(storage.follow(user).toBlocking().last().getUser()).toEqual(user);

        UserAssociation userAssociation = TestHelper.loadUserAssociation(Content.ME_FOLLOWINGS, user.getId());
        expect(userAssociation.getLocalSyncState()).toEqual(UserAssociation.LocalState.PENDING_ADDITION);
        expect(userAssociation.getToken()).toBeNull();
    }

    @Test
    public void shouldMarkFollowingForAdditionAndUpdateFollowersCount() throws Exception {
        user.followers_count = INITIAL_FOLLOWERS_COUNT;
        expect(storage.follow(user).toBlocking().last().getUser()).toEqual(user);

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
        storage.unfollow(user).toBlocking().last();

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
        List<PublicApiUser> items = ModelFixtures.create(PublicApiUser.class, 2);
        expect(storage.insertAssociations(items, Content.ME_FOLLOWERS.uri, USER_ID)).toEqual(4); // 2 users, associations
        expect(Content.ME_FOLLOWERS).toHaveCount(2);
    }

    @Test
    public void shouldBulkInsertFollowings() throws Exception {
        final List<PublicApiUser> users = ModelFixtures.create(PublicApiUser.class, 3);
        final Iterable<UserAssociation> newAssociations = storage.followList(users).toBlocking().toIterable();

        expect(Iterables.transform(newAssociations, new Function<UserAssociation, Object>() {
            @Override
            public Object apply(UserAssociation input) {
                return input.getUser();
            }
        })).toContainExactlyInAnyOrder(users.toArray(new PublicApiUser[3]));

        expect(Content.ME_FOLLOWINGS).toHaveCount(3);
        for (PublicApiUser user : users) {
            expect(TestHelper.getUserAssociationByTargetId(Content.ME_FOLLOWINGS.uri, user.getId()).getLocalSyncState())
                    .toBe(UserAssociation.LocalState.PENDING_ADDITION);
        }
    }

    @Test
    public void shouldBulkMarkFollowingsForRemoval() throws Exception {
        List<PublicApiUser> users = ModelFixtures.create(PublicApiUser.class, 3);
        storage.unfollowList(users).subscribe();
        expect(Content.ME_FOLLOWINGS).toHaveCount(3);
        for (PublicApiUser user : users) {
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
        UserAssociation associationForRemoval = new UserAssociation(Association.Type.FOLLOWING, new PublicApiUser(1000L));
        associationForRemoval.markForRemoval();
        TestHelper.insertWithDependencies(Content.USER_ASSOCIATIONS.uri, associationForRemoval);

        List<Long> localIds = storage.getStoredIds(Content.ME_FOLLOWINGS.uri);
        expect(localIds.size()).toEqual(107);
    }

    @Test
    public void shouldRemoveSyncedContentForLoggedInUser() throws Exception {
        PublicApiResource.ResourceHolder<PublicApiUser> followedUsers = TestHelper.readJson(PublicApiResource.ResourceHolder.class, RemoteCollectionLoaderTest.class, "me_followings.json");
        TestHelper.bulkInsertToUserAssociations(followedUsers.collection, Content.ME_FOLLOWINGS.uri);

        PublicApiResource.ResourceHolder<PublicApiUser> followers = TestHelper.readJson(PublicApiResource.ResourceHolder.class, RemoteCollectionLoaderTest.class, "me_followers.json");
        TestHelper.bulkInsertToUserAssociations(followers.collection, Content.ME_FOLLOWERS.uri);

        expect(Content.ME_FOLLOWINGS).toHaveCount(50);
        expect(Content.ME_FOLLOWERS).toHaveCount(50);

        storage.clear();
        expect(Content.ME_FOLLOWINGS).toHaveCount(0);
        expect(Content.ME_FOLLOWERS).toHaveCount(0);
    }

    @Test
    public void shouldInsertInSingleBatchIfCollectionIsSmallEnough() throws Exception {
        List<Long> ids = Collections.singletonList(1L);

        ContentResolver resolver = mock(ContentResolver.class);
        new LegacyUserAssociationStorage(Schedulers.immediate(), resolver).insertInBatches(Content.ME_FOLLOWERS, USER_ID, ids, 0, BATCH_SIZE);
        verify(resolver).bulkInsert(eq(Content.ME_FOLLOWERS.uri), any(ContentValues[].class));
        verifyNoMoreInteractions(resolver);
    }

    @Test
    public void shouldInsertInBatchesIfCollectionIsTooLarge() throws Exception {
        List<Long> ids = asList(1L, 2L);

        ContentResolver resolver = mock(ContentResolver.class);
        new LegacyUserAssociationStorage(Schedulers.immediate(), resolver).insertInBatches(Content.ME_FOLLOWERS, USER_ID, ids, 0, BATCH_SIZE);
        verify(resolver, times(2)).bulkInsert(eq(Content.ME_FOLLOWERS.uri), any(ContentValues[].class));
        verifyNoMoreInteractions(resolver);
    }

    @Test
    public void shouldSetCorrectInsertPositionOfPositionColumnWhenInsertingSingleBatch() {
        List<Long> ids = Collections.singletonList(1L);

        int START_POSITION = 27;
        ContentResolver resolver = mock(ContentResolver.class);
        new LegacyUserAssociationStorage(Schedulers.immediate(), resolver).insertInBatches(Content.ME_FOLLOWERS, USER_ID, ids, START_POSITION, BATCH_SIZE);

        ArgumentCaptor<ContentValues[]> argumentCaptor = ArgumentCaptor.forClass(ContentValues[].class);
        verify(resolver).bulkInsert(eq(Content.ME_FOLLOWERS.uri), argumentCaptor.capture());
        ContentValues[] contentValuesArr = argumentCaptor.getValue();
        int counter = 0;
        for (ContentValues contentValues : contentValuesArr) {
            expect(contentValues.getAsInteger(TableColumns.UserAssociations.POSITION)).toEqual(START_POSITION + counter++);
        }
        expect(counter).toEqual(ids.size());
    }

    @Test
    public void shouldSetCorrectInsertPositionOfPositionColumnWhenInsertingMultipleBatches() {
        List<Long> ids = asList(1L, 2L);

        int START_POSITION = 66;
        ContentResolver resolver = mock(ContentResolver.class);
        new LegacyUserAssociationStorage(Schedulers.immediate(), resolver).insertInBatches(Content.ME_FOLLOWERS, USER_ID, ids, START_POSITION, BATCH_SIZE);

        ArgumentCaptor<ContentValues[]> argumentCaptor = ArgumentCaptor.forClass(ContentValues[].class);
        verify(resolver, times(2)).bulkInsert(eq(Content.ME_FOLLOWERS.uri), argumentCaptor.capture());
        List<ContentValues[]> contentValues = argumentCaptor.getAllValues();
        int counter = 0;
        for (ContentValues[] contentValue : contentValues) {
            for(ContentValues values : contentValue){
                expect(values.getAsInteger(TableColumns.UserAssociations.POSITION)).toEqual(START_POSITION + counter++);
            }
        }
        expect(counter).toEqual(ids.size());
    }


    @Test
    public void shouldQueryFollowings() {
        TestHelper.bulkInsertToUserAssociations(ModelFixtures.create(PublicApiUser.class, 2), Content.ME_FOLLOWINGS.uri);
        expect(Content.ME_FOLLOWINGS).toHaveCount(2);
        expect(Iterables.size(storage.getFollowings().toBlocking().toIterable())).toEqual(2);
    }

    @Test
    public void shouldQueryFollowingsAndExemptFollowingsMarkedForDeletion() {
        List<PublicApiUser> users = ModelFixtures.create(PublicApiUser.class, 2);
        TestHelper.bulkInsertToUserAssociations(users.subList(0, 1), Content.ME_FOLLOWINGS.uri);
        TestHelper.bulkInsertToUserAssociationsAsRemovals(users.subList(1, 2), Content.ME_FOLLOWINGS.uri);
        expect(Content.ME_FOLLOWINGS).toHaveCount(2);
        expect(Iterables.size(storage.getFollowings().toBlocking().toIterable())).toEqual(1);
    }

    @Test
    public void shouldNotHaveUnsyncedFollowings() {
        TestHelper.bulkInsertToUserAssociations(ModelFixtures.create(PublicApiUser.class, 2), Content.ME_FOLLOWINGS.uri);
        expect(Content.ME_FOLLOWINGS).toHaveCount(2);
        expect(storage.hasFollowingsNeedingSync()).toBeFalse();
    }

    @Test
    public void shouldHaveAnUnsyncedRemoval() {
        List<PublicApiUser> users = ModelFixtures.create(PublicApiUser.class, 2);
        TestHelper.bulkInsertToUserAssociations(users.subList(0, 1), Content.ME_FOLLOWINGS.uri);
        TestHelper.bulkInsertToUserAssociationsAsRemovals(users.subList(1, 2), Content.ME_FOLLOWINGS.uri);
        expect(Content.ME_FOLLOWINGS).toHaveCount(2);
        expect(storage.hasFollowingsNeedingSync()).toBeTrue();
    }

    @Test
    public void shouldHaveAnUnsyncedAddition() {
        List<PublicApiUser> users = ModelFixtures.create(PublicApiUser.class, 2);
        TestHelper.bulkInsertToUserAssociations(users.subList(0, 1), Content.ME_FOLLOWINGS.uri);
        TestHelper.bulkInsertToUserAssociationsAsAdditions(users.subList(1, 2), Content.ME_FOLLOWINGS.uri);
        expect(Content.ME_FOLLOWINGS).toHaveCount(2);
        expect(storage.hasFollowingsNeedingSync()).toBeTrue();
    }

    @Test
    public void shouldQueryUnsyncedFollowingRemoval() {
        List<PublicApiUser> users = ModelFixtures.create(PublicApiUser.class, 2);
        TestHelper.bulkInsertToUserAssociations(users.subList(0, 1), Content.ME_FOLLOWINGS.uri);
        TestHelper.bulkInsertToUserAssociationsAsRemovals(users.subList(1, 2), Content.ME_FOLLOWINGS.uri);
        expect(storage.getFollowingsNeedingSync().size()).toEqual(1);
    }

    @Test
    public void shouldQueryUnsyncedFollowingAddition() {
        List<PublicApiUser> users = ModelFixtures.create(PublicApiUser.class, 2);
        TestHelper.bulkInsertToUserAssociations(users.subList(0, 1), Content.ME_FOLLOWINGS.uri);
        TestHelper.bulkInsertToUserAssociationsAsAdditions(users.subList(1, 2), Content.ME_FOLLOWINGS.uri);
        expect(Content.ME_FOLLOWINGS).toHaveCount(2);
        expect(storage.getFollowingsNeedingSync().size()).toEqual(1);
    }

    @Test
    public void shouldQueryUnsyncedFollowingAdditionWithToken() {
        List<PublicApiUser> users = ModelFixtures.create(PublicApiUser.class, 3);
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
        TestHelper.bulkInsertToUserAssociations(ModelFixtures.create(PublicApiUser.class, 2), Content.ME_FOLLOWINGS.uri);
        expect(Content.ME_FOLLOWINGS).toHaveCount(2);

        List<UserAssociation> userAssociations = newArrayList(storage.getFollowings().toBlocking().getIterator());

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
        TestHelper.bulkInsertToUserAssociations(ModelFixtures.create(PublicApiUser.class, 2), Content.ME_FOLLOWINGS.uri);
        expect(Content.ME_FOLLOWINGS).toHaveCount(2);

        UserAssociation association = storage.getFollowings().toBlocking().last();
        expect(storage.setFollowingAsSynced(association)).toBeFalse();
    }

    @Test
    public void shouldClearSyncFlagForAddition() throws Exception {
        final List<PublicApiUser> users = ModelFixtures.create(PublicApiUser.class, 2);
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
        final List<PublicApiUser> users = ModelFixtures.create(PublicApiUser.class, 2);
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
        final List<PublicApiUser> users = ModelFixtures.create(PublicApiUser.class, 2);
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
        TestHelper.bulkInsertToUserAssociationsAsRemovals(ModelFixtures.create(PublicApiUser.class, 2), Content.ME_FOLLOWINGS.uri);
        expect(Content.ME_FOLLOWINGS).toHaveCount(2);
        expect(storage.deleteFollowings(TestHelper.loadUserAssociations(Content.ME_FOLLOWINGS))).toBeTrue();
        expect(Content.ME_FOLLOWINGS).toHaveCount(0);
    }

    @Test
    public void shouldSetListOfFollowingsAsSynced() throws Exception {
        final List<PublicApiUser> users = ModelFixtures.create(PublicApiUser.class, 2);

        TestHelper.bulkInsertToUserAssociationsAsAdditions(users, Content.ME_FOLLOWINGS.uri);
        expect(Content.ME_FOLLOWINGS).toHaveCount(2);
        UserAssociation association1 = TestHelper.loadUserAssociation(Content.ME_FOLLOWINGS, users.get(0).getId());
        expect(association1.getLocalSyncState()).toEqual(UserAssociation.LocalState.PENDING_ADDITION);
        UserAssociation association2 = TestHelper.loadUserAssociation(Content.ME_FOLLOWINGS, users.get(1).getId());
        expect(association2.getLocalSyncState()).toEqual(UserAssociation.LocalState.PENDING_ADDITION);

        storage.setFollowingsAsSynced(newArrayList(association1, association2)).toBlocking().last();

        expect(Content.ME_FOLLOWINGS).toHaveCount(2);
        association1 = TestHelper.loadUserAssociation(Content.ME_FOLLOWINGS, users.get(0).getId());
        expect(association1.getLocalSyncState()).toEqual(UserAssociation.LocalState.NONE);
        association2 = TestHelper.loadUserAssociation(Content.ME_FOLLOWINGS, users.get(1).getId());
        expect(association2.getLocalSyncState()).toEqual(UserAssociation.LocalState.NONE);
    }

    @Test
    public void shouldDeleteFollowings() {
        TestHelper.bulkInsertToUserAssociations(ModelFixtures.create(PublicApiUser.class, 2), Content.ME_FOLLOWINGS.uri);
        expect(Content.ME_FOLLOWINGS).toHaveCount(2);

        List<Long> storedIds = storage.getStoredIds(Content.ME_FOLLOWINGS.uri);
        storage.deleteAssociations(Content.ME_FOLLOWINGS.uri, storedIds);
        expect(Content.ME_FOLLOWINGS).toHaveCount(0);
    }

    @Test
    public void shouldDeleteFollowers() {
        TestHelper.bulkInsertToUserAssociations(ModelFixtures.create(PublicApiUser.class, 2), Content.ME_FOLLOWERS.uri);
        expect(Content.ME_FOLLOWERS).toHaveCount(2);

        List<Long> storedIds = storage.getStoredIds(Content.ME_FOLLOWERS.uri);
        storage.deleteAssociations(Content.ME_FOLLOWERS.uri, storedIds);
        expect(Content.ME_FOLLOWERS).toHaveCount(0);
    }
}
