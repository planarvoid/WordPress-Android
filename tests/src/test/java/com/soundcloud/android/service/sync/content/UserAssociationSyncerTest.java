package com.soundcloud.android.service.sync.content;

import static com.soundcloud.android.Expect.expect;
import static com.soundcloud.android.robolectric.TestHelper.addCannedResponse;
import static com.soundcloud.android.robolectric.TestHelper.addIdResponse;
import static com.soundcloud.android.robolectric.TestHelper.addPendingHttpResponse;
import static com.soundcloud.android.robolectric.TestHelper.assertFirstIdToBe;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import com.soundcloud.android.Consts;
import com.soundcloud.android.dao.ResolverHelper;
import com.soundcloud.android.dao.UserAssociationStorage;
import com.soundcloud.android.model.Association;
import com.soundcloud.android.model.User;
import com.soundcloud.android.model.UserAssociation;
import com.soundcloud.android.provider.Content;
import com.soundcloud.android.robolectric.DefaultTestRunner;
import com.soundcloud.android.robolectric.TestHelper;
import com.soundcloud.android.service.sync.ApiSyncResult;
import com.soundcloud.android.service.sync.ApiSyncService;
import com.soundcloud.android.service.sync.ApiSyncServiceTest;
import com.xtremelabs.robolectric.Robolectric;
import org.apache.http.HttpStatus;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Intent;
import android.net.Uri;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@RunWith(DefaultTestRunner.class)
public class UserAssociationSyncerTest {
    private static final long USER_ID = 133201L;
    ContentResolver resolver;
    UserAssociationSyncer userAssociationSyncerMockStorage;

    @Mock UserAssociationStorage mockUserAssociationStorage;
    @Mock UserAssociation mockUserAssociation;

    @Before
    public void before() {
        TestHelper.setUserId(USER_ID);
        resolver = DefaultTestRunner.application.getContentResolver();

        userAssociationSyncerMockStorage = new UserAssociationSyncer(Robolectric.application,
                Robolectric.application.getContentResolver(), mockUserAssociationStorage);
    }

    @Test
    public void shouldSyncFollowers() throws Exception {
        addIdResponse("/me/followers/ids?linked_partitioning=1", 792584, 1255758, 308291);
        addCannedResponse(ApiSyncServiceTest.class, "/me/followers?linked_partitioning=1&limit=" + Consts.COLLECTION_PAGE_SIZE, "users.json");

        ApiSyncResult result = sync(Content.ME_FOLLOWERS.uri);
        expect(result.success).toBeTrue();
        expect(result.synced_at).toBeGreaterThan(0l);
        expect(Content.USERS).toHaveCount(3);

        List<User> followers = TestHelper.loadLocalContent(Content.ME_FOLLOWERS.uri, User.class);
        expect(followers.get(0).getId()).toEqual(308291l);
        for (User u : followers){
            expect(u.isStale()).toBeFalse();
        }
    }

    @Test
    public void shouldSyncFollowersInSingleBatchIfCollectionIsSmallEnough() throws Exception {
        addIdResponse("/me/followers/ids?linked_partitioning=1", 792584, 1255758, 308291);
        addCannedResponse(ApiSyncServiceTest.class, "/me/followers?linked_partitioning=1&limit=" + Consts.COLLECTION_PAGE_SIZE, "empty_collection.json");

        ContentResolver resolver = Mockito.mock(ContentResolver.class);

        UserAssociationSyncer syncer = new UserAssociationSyncer(Robolectric.application, resolver);

        syncer.setBulkInsertBatchSize(Integer.MAX_VALUE);
        syncer.syncContent(Content.ME_FOLLOWERS.uri, Intent.ACTION_SYNC);
        verify(resolver).bulkInsert(eq(Content.ME_FOLLOWERS.uri), any(ContentValues[].class));
        verify(resolver).query(eq(ResolverHelper.addIdOnlyParameter(Content.ME_FOLLOWERS.uri)),
                isNull(String[].class), isNull(String.class), isNull(String[].class), isNull(String.class));
        verifyNoMoreInteractions(resolver);
    }

    @Test
    public void shouldSyncFollowersInBatchesIfCollectionTooLarge() throws Exception {
        addIdResponse("/me/followers/ids?linked_partitioning=1", 792584, 1255758, 308291);
        addCannedResponse(ApiSyncServiceTest.class, "/me/followers?linked_partitioning=1&limit=" + Consts.COLLECTION_PAGE_SIZE, "empty_collection.json");

        ContentResolver resolver = Mockito.mock(ContentResolver.class);

        UserAssociationSyncer syncer = new UserAssociationSyncer(Robolectric.application, resolver);

        syncer.setBulkInsertBatchSize(2); // for 3 users, this should result in 2 batches being inserted
        syncer.syncContent(Content.ME_FOLLOWERS.uri, Intent.ACTION_SYNC);

        verify(resolver, times(2)).bulkInsert(eq(Content.ME_FOLLOWERS.uri), any(ContentValues[].class));
        verify(resolver).query(eq(ResolverHelper.addIdOnlyParameter(Content.ME_FOLLOWERS.uri)),
                isNull(String[].class), isNull(String.class), isNull(String[].class), isNull(String.class));
        verifyNoMoreInteractions(resolver);
    }

    @Test
    public void shouldSyncFriends() throws Exception {
        addIdResponse("/me/connections/friends/ids?linked_partitioning=1", 792584, 1255758, 308291);
        TestHelper.addResourceResponse(ApiSyncServiceTest.class, "/users?linked_partitioning=1&limit=200&ids=792584%2C1255758%2C308291", "users.json");

        sync(Content.ME_FRIENDS.uri);

        // make sure tracks+users got written
        expect(Content.USERS).toHaveCount(3);
        expect(Content.ME_FRIENDS).toHaveCount(3);
        assertFirstIdToBe(Content.ME_FRIENDS, 308291);
    }

    @Test
    public void shouldNotRemoveDirtyAdditionsWhenSyncingLocalToRemote() throws Exception {
        final long user_id = 1L;
        UserAssociation userAssociation = new UserAssociation(Association.Type.FOLLOWING,new User(user_id));
        userAssociation.markForAddition();
        TestHelper.insertWithDependencies(Content.USER_ASSOCIATIONS.uri, userAssociation);

        expect(TestHelper.getUserAssociationByTargetId(Content.ME_FOLLOWINGS.uri, user_id).getLocalSyncState())
                .toEqual(UserAssociation.LocalState.PENDING_ADDITION);

        addIdResponse("/me/followings/ids?linked_partitioning=1", 792584, 1255758, 308291);
        addCannedResponse(ApiSyncServiceTest.class, "/me/followings?linked_partitioning=1&limit=" + Consts.COLLECTION_PAGE_SIZE, "users.json");

        expect(sync(Content.ME_FOLLOWINGS.uri).success).toBeTrue();

        expect(TestHelper.getUserAssociationByTargetId(Content.ME_FOLLOWINGS.uri, user_id).getLocalSyncState())
                .toEqual(UserAssociation.LocalState.PENDING_ADDITION);
    }

    @Test
    public void shouldNotRemoveDirtyRemovalsWhenSyncingLocalToRemote() throws Exception {
        final long user_id = 1L;
        UserAssociation userAssociation = new UserAssociation(Association.Type.FOLLOWING,new User(user_id));
        userAssociation.markForRemoval();
        TestHelper.insertWithDependencies(Content.USER_ASSOCIATIONS.uri, userAssociation);

        expect(TestHelper.getUserAssociationByTargetId(Content.ME_FOLLOWINGS.uri, user_id).getLocalSyncState())
                .toEqual(UserAssociation.LocalState.PENDING_REMOVAL);

        addIdResponse("/me/followings/ids?linked_partitioning=1", 792584, 1255758, 308291);
        addCannedResponse(ApiSyncServiceTest.class, "/me/followings?linked_partitioning=1&limit=" + Consts.COLLECTION_PAGE_SIZE, "users.json");

        expect(sync(Content.ME_FOLLOWINGS.uri).success).toBeTrue();

        expect(TestHelper.getUserAssociationByTargetId(Content.ME_FOLLOWINGS.uri, user_id).getLocalSyncState())
                .toEqual(UserAssociation.LocalState.PENDING_REMOVAL);
    }


    @Test
    public void shouldReturnReorderedForUsersIfLocalStateEqualsRemote() throws Exception {
        addIdResponse("/me/followers/ids?linked_partitioning=1", 792584, 1255758, 308291);
        addCannedResponse(ApiSyncServiceTest.class, "/me/followers?linked_partitioning=1&limit=" + Consts.COLLECTION_PAGE_SIZE, "users.json");

        ApiSyncResult result = sync(Content.ME_FOLLOWERS.uri);
        expect(result.success).toBeTrue();
        expect(result.synced_at).toBeGreaterThan(0l);

        // make sure tracks+users got written
        expect(Content.USERS).toHaveCount(3);
        expect(Content.ME_FOLLOWERS).toHaveCount(3);
        assertFirstIdToBe(Content.ME_FOLLOWERS, 308291);

        addIdResponse("/me/followers/ids?linked_partitioning=1", 792584, 1255758, 308291);
        addCannedResponse(ApiSyncServiceTest.class, "/me/followers?linked_partitioning=1&limit=" + Consts.COLLECTION_PAGE_SIZE, "users.json");
        result = sync(Content.ME_FOLLOWERS.uri);
        expect(result.success).toBe(true);
        expect(result.change).toEqual(ApiSyncResult.REORDERED);
        expect(result.synced_at).toBeGreaterThan(0l);
        expect(result.extra).toBeNull();
    }

    @Test
    public void shouldClearSyncStateAndReturnSuccessFromAdditionPushWithResponseOk() throws Exception {
        Robolectric.setDefaultHttpResponse(HttpStatus.SC_OK, "ok");
        when(mockUserAssociation.getLocalSyncState()).thenReturn(UserAssociation.LocalState.PENDING_ADDITION);
        when(mockUserAssociation.getUser()).thenReturn(new User(1L));
        expect(userAssociationSyncerMockStorage.pushUserAssociation(mockUserAssociation)).toBeTrue();
        verify(mockUserAssociationStorage).setFollowingAsSynced(mockUserAssociation);
    }

    @Test
    public void shouldClearSyncStateAndReturnSuccessFromAdditionPushWithResponseCreated() throws Exception {
        Robolectric.setDefaultHttpResponse(HttpStatus.SC_CREATED, "created");
        when(mockUserAssociation.getLocalSyncState()).thenReturn(UserAssociation.LocalState.PENDING_ADDITION);
        when(mockUserAssociation.getUser()).thenReturn(new User(1L));
        expect(userAssociationSyncerMockStorage.pushUserAssociation(mockUserAssociation)).toBeTrue();
        verify(mockUserAssociationStorage).setFollowingAsSynced(mockUserAssociation);
    }

    @Test
    public void shouldClearSyncStateAndReturnSuccessFromSuccesfulRemovalPushWithResponsOk() throws Exception {
        Robolectric.setDefaultHttpResponse(HttpStatus.SC_OK, "ok");
        when(mockUserAssociation.getLocalSyncState()).thenReturn(UserAssociation.LocalState.PENDING_REMOVAL);
        when(mockUserAssociation.getUser()).thenReturn(new User(1L));
        expect(userAssociationSyncerMockStorage.pushUserAssociation(mockUserAssociation)).toBeTrue();
        verify(mockUserAssociationStorage).setFollowingAsSynced(mockUserAssociation);
    }

    @Test
    public void shouldClearSyncStateAndReturnSuccessFroRemovalPushWithResponseNotFound() throws Exception {
        Robolectric.setDefaultHttpResponse(HttpStatus.SC_NOT_FOUND, "not found");
        when(mockUserAssociation.getLocalSyncState()).thenReturn(UserAssociation.LocalState.PENDING_REMOVAL);
        when(mockUserAssociation.getUser()).thenReturn(new User(1L));
        expect(userAssociationSyncerMockStorage.pushUserAssociation(mockUserAssociation)).toBeTrue();
        verify(mockUserAssociationStorage).setFollowingAsSynced(mockUserAssociation);
    }

    @Test
    public void shouldNotClearSyncStateAndReturnSuccessFromCleanAssociation() throws Exception {
        Robolectric.setDefaultHttpResponse(HttpStatus.SC_OK, "ok");
        when(mockUserAssociation.getLocalSyncState()).thenReturn(UserAssociation.LocalState.NONE);
        when(mockUserAssociation.getUser()).thenReturn(new User(1L));
        expect(userAssociationSyncerMockStorage.pushUserAssociation(mockUserAssociation)).toBeTrue();
        verify(mockUserAssociationStorage, never()).setFollowingAsSynced(mockUserAssociation);
    }

    @Test
    public void shouldReturnSuccessOnEmptyAssociationPush() throws Exception {
        when(mockUserAssociationStorage.hasFollowingsNeedingSync()).thenReturn(false);
        expect(pushSyncMockedStorage(Content.ME_FOLLOWERS.uri).success).toBeTrue();
    }

    @Test
    public void shouldSetSuccesfullyPushedAssociationsAsSynced() throws Exception {
        Robolectric.setDefaultHttpResponse(HttpStatus.SC_OK, "ok");
        when(mockUserAssociationStorage.hasFollowingsNeedingSync()).thenReturn(true);

        final List<UserAssociation> usersAssociations = createDirtyFollowings(3);
        for (UserAssociation association : usersAssociations) association.markForAddition();

        when(mockUserAssociationStorage.getFollowingsNeedingSync()).thenReturn(usersAssociations);
        expect(pushSyncMockedStorage(Content.ME_FOLLOWINGS.uri).success).toBeTrue();

        for (UserAssociation userAssociation : usersAssociations){
            verify(mockUserAssociationStorage).setFollowingAsSynced(userAssociation);
        }
    }

    @Test
    public void shouldNotSetFailedAssociationPushesAsSynced() throws Exception {
        Robolectric.setDefaultHttpResponse(500, "error");
        when(mockUserAssociationStorage.hasFollowingsNeedingSync()).thenReturn(true);

        final List<UserAssociation> usersAssociations = createDirtyFollowings(3);
        for (UserAssociation association : usersAssociations) association.markForAddition();

        when(mockUserAssociationStorage.getFollowingsNeedingSync()).thenReturn(usersAssociations);
        expect(pushSyncMockedStorage(Content.ME_FOLLOWINGS.uri).success).toBeFalse();

        for (UserAssociation userAssociation : usersAssociations) {
            verify(mockUserAssociationStorage, never()).setFollowingAsSynced(userAssociation);
        }
    }

    @Test(expected = IOException.class)
    public void shouldThrowIOException() throws Exception {
        Robolectric.setDefaultHttpResponse(500, "error");
        sync(Content.ME_FOLLOWERS.uri);
    }

    private ApiSyncResult sync(Uri uri, String... fixtures) throws IOException {
        addPendingHttpResponse(ApiSyncServiceTest.class, fixtures);
        UserAssociationSyncer syncer = new UserAssociationSyncer(Robolectric.application, Robolectric.application.getContentResolver());
        return syncer.syncContent(uri, Intent.ACTION_SYNC);
    }

    private ApiSyncResult pushSyncMockedStorage(Uri uri, String... fixtures) throws IOException {
        addPendingHttpResponse(ApiSyncServiceTest.class, fixtures);
        return userAssociationSyncerMockStorage.syncContent(uri, ApiSyncService.ACTION_PUSH);
    }

    private static List<UserAssociation> createDirtyFollowings(int count) {
        List<UserAssociation> userAssociations = new ArrayList<UserAssociation>();
        for (User user : TestHelper.createUsers(count)) {
            final UserAssociation association = new UserAssociation(Association.Type.FOLLOWING, user);
            association.markForAddition();
            userAssociations.add(association);
        }
        return userAssociations;

    }
}
