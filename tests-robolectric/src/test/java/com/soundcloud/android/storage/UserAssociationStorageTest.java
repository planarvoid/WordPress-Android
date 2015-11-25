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
import com.soundcloud.java.collections.Lists;
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
    public static final String TOKEN = "12345";

    private PublicApiUser user;
    private ContentResolver resolver;
    private LegacyUserAssociationStorage storage;

    @Before
    public void before() {
        TestHelper.setUserId(USER_ID);
        resolver = DefaultTestRunner.application.getContentResolver();
        storage = new LegacyUserAssociationStorage(resolver);
        user = new PublicApiUser(1);
    }

    @Test
    public void shouldBulkInsertAssociations() throws Exception {
        List<PublicApiUser> items = ModelFixtures.create(PublicApiUser.class, 2);
        expect(storage.insertAssociations(items, Content.ME_FOLLOWINGS.uri, USER_ID)).toEqual(4); // 2 users, associations
        expect(Content.ME_FOLLOWINGS).toHaveCount(2);
    }

    @Test
    public void shouldGetLocalIds() throws Exception {
        TestHelper.bulkInsertDummyIdsToUserAssociations(Content.ME_FOLLOWINGS.uri, 107, USER_ID);
        List<Long> localIds = storage.getStoredIds(Content.ME_FOLLOWINGS.uri);
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

        expect(Content.ME_FOLLOWINGS).toHaveCount(50);

        storage.clear();
        expect(Content.ME_FOLLOWINGS).toHaveCount(0);
    }

    @Test
    public void shouldInsertInSingleBatchIfCollectionIsSmallEnough() throws Exception {
        List<Long> ids = Collections.singletonList(1L);

        ContentResolver resolver = mock(ContentResolver.class);
        new LegacyUserAssociationStorage(resolver).insertInBatches(USER_ID, ids, 0, BATCH_SIZE);
        verify(resolver).bulkInsert(eq(Content.ME_FOLLOWINGS.uri), any(ContentValues[].class));
        verifyNoMoreInteractions(resolver);
    }

    @Test
    public void shouldInsertInBatchesIfCollectionIsTooLarge() throws Exception {
        List<Long> ids = asList(1L, 2L);

        ContentResolver resolver = mock(ContentResolver.class);
        new LegacyUserAssociationStorage(resolver).insertInBatches(USER_ID, ids, 0, BATCH_SIZE);
        verify(resolver, times(2)).bulkInsert(eq(Content.ME_FOLLOWINGS.uri), any(ContentValues[].class));
        verifyNoMoreInteractions(resolver);
    }

    @Test
    public void shouldSetCorrectInsertPositionOfPositionColumnWhenInsertingSingleBatch() {
        List<Long> ids = Collections.singletonList(1L);

        int START_POSITION = 27;
        ContentResolver resolver = mock(ContentResolver.class);
        new LegacyUserAssociationStorage(resolver).insertInBatches(USER_ID, ids, START_POSITION, BATCH_SIZE);

        ArgumentCaptor<ContentValues[]> argumentCaptor = ArgumentCaptor.forClass(ContentValues[].class);
        verify(resolver).bulkInsert(eq(Content.ME_FOLLOWINGS.uri), argumentCaptor.capture());
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
        new LegacyUserAssociationStorage(resolver).insertInBatches(USER_ID, ids, START_POSITION, BATCH_SIZE);

        ArgumentCaptor<ContentValues[]> argumentCaptor = ArgumentCaptor.forClass(ContentValues[].class);
        verify(resolver, times(2)).bulkInsert(eq(Content.ME_FOLLOWINGS.uri), argumentCaptor.capture());
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
    public void shouldDeleteFollowings() {
        TestHelper.bulkInsertToUserAssociations(ModelFixtures.create(PublicApiUser.class, 2), Content.ME_FOLLOWINGS.uri);
        expect(Content.ME_FOLLOWINGS).toHaveCount(2);

        List<Long> storedIds = storage.getStoredIds(Content.ME_FOLLOWINGS.uri);
        storage.deleteAssociations(Content.ME_FOLLOWINGS.uri, storedIds);
        expect(Content.ME_FOLLOWINGS).toHaveCount(0);
    }

    @Test
    public void shouldDeleteFollowers() {
        TestHelper.bulkInsertToUserAssociations(ModelFixtures.create(PublicApiUser.class, 2), Content.ME_FOLLOWINGS.uri);
        expect(Content.ME_FOLLOWINGS).toHaveCount(2);

        List<Long> storedIds = storage.getStoredIds(Content.ME_FOLLOWINGS.uri);
        storage.deleteAssociations(Content.ME_FOLLOWINGS.uri, storedIds);
        expect(Content.ME_FOLLOWINGS).toHaveCount(0);
    }
}
