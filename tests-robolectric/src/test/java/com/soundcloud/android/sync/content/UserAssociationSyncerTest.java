package com.soundcloud.android.sync.content;

import static com.google.common.collect.Lists.newArrayList;
import static com.soundcloud.android.Expect.expect;
import static com.soundcloud.android.testsupport.TestHelper.addCannedResponse;
import static com.soundcloud.android.testsupport.TestHelper.addIdResponse;
import static com.soundcloud.android.testsupport.TestHelper.addPendingHttpResponse;
import static com.soundcloud.android.testsupport.TestHelper.assertFirstIdToBe;
import static org.mockito.Matchers.anyListOf;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import com.google.common.collect.Lists;
import com.soundcloud.android.Consts;
import com.soundcloud.android.Navigator;
import com.soundcloud.android.accounts.AccountOperations;
import com.soundcloud.android.api.ApiRequest;
import com.soundcloud.android.api.ApiRequestException;
import com.soundcloud.android.api.ApiResponse;
import com.soundcloud.android.api.json.JsonTransformer;
import com.soundcloud.android.api.legacy.model.Association;
import com.soundcloud.android.api.legacy.model.PublicApiUser;
import com.soundcloud.android.api.legacy.model.UserAssociation;
import com.soundcloud.android.associations.FollowingOperations;
import com.soundcloud.android.model.ScModel;
import com.soundcloud.android.onboarding.suggestions.SuggestedUsersOperations;
import com.soundcloud.android.robolectric.DefaultTestRunner;
import com.soundcloud.android.storage.UserAssociationStorage;
import com.soundcloud.android.storage.provider.Content;
import com.soundcloud.android.sync.ApiSyncResult;
import com.soundcloud.android.sync.ApiSyncService;
import com.soundcloud.android.sync.ApiSyncServiceTest;
import com.soundcloud.android.testsupport.TestHelper;
import com.soundcloud.android.testsupport.fixtures.ModelFixtures;
import com.xtremelabs.robolectric.Robolectric;
import org.apache.http.HttpStatus;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import rx.Observable;

import android.app.NotificationManager;
import android.content.ContentResolver;
import android.content.Intent;
import android.net.Uri;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@RunWith(DefaultTestRunner.class)
public class UserAssociationSyncerTest {
    private static final long USER_ID = 133201L;
    UserAssociationSyncer userAssociationSyncer;

    @Mock private ContentResolver resolver;
    @Mock private UserAssociationStorage userAssociationStorage;
    @Mock private UserAssociation mockUserAssociation;
    @Mock private SuggestedUsersOperations suggestedUsersOperations;
    @Mock private AccountOperations accountOperations;
    @Mock private UserAssociation userAssociation;
    @Mock private PublicApiUser user;
    @Mock private FollowingOperations followingOperations;
    @Mock private ApiResponse apiResponse;
    @Mock private NotificationManager notificationManager;
    @Mock private JsonTransformer jsonTransformer;
    @Mock private Navigator navigator;

    @Before
    public void before() {
        TestHelper.setUserId(133201L);
        userAssociationSyncer = new UserAssociationSyncer(Robolectric.application,
                resolver, userAssociationStorage, followingOperations, accountOperations, notificationManager, jsonTransformer, navigator);
        when(userAssociation.getUser()).thenReturn(user);
        when(userAssociation.getLocalSyncState()).thenReturn(UserAssociation.LocalState.NONE);
        when(accountOperations.isUserLoggedIn()).thenReturn(true);
        when(accountOperations.getLoggedInUserId()).thenReturn(133201L);
    }

    @Test
    public void shouldNotSyncWithNoLoggedInUser() throws Exception {
        when(accountOperations.isUserLoggedIn()).thenReturn(false);
        ApiSyncResult result = sync(Content.ME_FOLLOWERS.uri);
        expect(result.success).toBeFalse();
    }

    @Test
    public void shouldSyncFollowers() throws Exception {
        addIdResponse("/me/followers/ids?linked_partitioning=1", 792584, 1255758, 308291);
        addCannedResponse(ApiSyncServiceTest.class, "/me/followers?linked_partitioning=1&limit=" + Consts.LIST_PAGE_SIZE, "users.json");

        ApiSyncResult result = sync(Content.ME_FOLLOWERS.uri);
        expect(result.success).toBeTrue();
        expect(result.synced_at).toBeGreaterThan(0l);
        expect(Content.USERS).toHaveCount(3);

        List<PublicApiUser> followers = TestHelper.loadLocalContent(Content.ME_FOLLOWERS.uri, PublicApiUser.class);
        expect(followers.get(0).getId()).toEqual(308291l);
        for (PublicApiUser u : followers) {
            expect(u.isStale()).toBeFalse();
        }
    }

    @Test
    public void shouldCallUserAssociationStorageWithAllIds() throws Exception {
        addIdResponse("/me/followers/ids?linked_partitioning=1", 792584, 1255758, 308291);
        addCannedResponse(ApiSyncServiceTest.class, "/me/followers?linked_partitioning=1&limit=" + Consts.LIST_PAGE_SIZE, "empty_collection.json");

        userAssociationSyncer.setBulkInsertBatchSize(Integer.MAX_VALUE);
        userAssociationSyncer.syncContent(Content.ME_FOLLOWERS.uri, Intent.ACTION_SYNC);

        verify(userAssociationStorage).insertInBatches(Content.ME_FOLLOWERS, USER_ID, newArrayList(792584L, 1255758L, 308291L), 0, Integer.MAX_VALUE);
    }

    @Test
    public void shouldNotRemoveDirtyAdditionsWhenSyncingLocalToRemote() throws Exception {
        final long user_id = 1L;
        UserAssociation userAssociation = new UserAssociation(Association.Type.FOLLOWING, new PublicApiUser(user_id));
        userAssociation.markForAddition();
        TestHelper.insertWithDependencies(Content.USER_ASSOCIATIONS.uri, userAssociation);

        expect(TestHelper.getUserAssociationByTargetId(Content.ME_FOLLOWINGS.uri, user_id).getLocalSyncState())
                .toEqual(UserAssociation.LocalState.PENDING_ADDITION);

        addIdResponse("/me/followings/ids?linked_partitioning=1", 792584, 1255758, 308291);
        addCannedResponse(ApiSyncServiceTest.class, "/me/followings?linked_partitioning=1&limit=" + Consts.LIST_PAGE_SIZE, "users.json");

        expect(sync(Content.ME_FOLLOWINGS.uri).success).toBeTrue();

        expect(TestHelper.getUserAssociationByTargetId(Content.ME_FOLLOWINGS.uri, user_id).getLocalSyncState())
                .toEqual(UserAssociation.LocalState.PENDING_ADDITION);
    }

    @Test
    public void shouldNotRemoveDirtyRemovalsWhenSyncingLocalToRemote() throws Exception {
        final long user_id = 1L;
        UserAssociation userAssociation = new UserAssociation(Association.Type.FOLLOWING, new PublicApiUser(user_id));
        userAssociation.markForRemoval();
        TestHelper.insertWithDependencies(Content.USER_ASSOCIATIONS.uri, userAssociation);

        expect(TestHelper.getUserAssociationByTargetId(Content.ME_FOLLOWINGS.uri, user_id).getLocalSyncState())
                .toEqual(UserAssociation.LocalState.PENDING_REMOVAL);

        addIdResponse("/me/followings/ids?linked_partitioning=1", 792584, 1255758, 308291);
        addCannedResponse(ApiSyncServiceTest.class, "/me/followings?linked_partitioning=1&limit=" + Consts.LIST_PAGE_SIZE, "users.json");

        expect(sync(Content.ME_FOLLOWINGS.uri).success).toBeTrue();

        expect(TestHelper.getUserAssociationByTargetId(Content.ME_FOLLOWINGS.uri, user_id).getLocalSyncState())
                .toEqual(UserAssociation.LocalState.PENDING_REMOVAL);
    }

    @Test
    public void shouldReturnSuccessAndUnchangedResultForRepeatEmptySync() throws Exception {
        addIdResponse("/me/followers/ids?linked_partitioning=1");
        addCannedResponse(ApiSyncServiceTest.class, "/me/followers?linked_partitioning=1&limit=" + Consts.LIST_PAGE_SIZE, "empty_collection.json");

        ApiSyncResult result = sync(Content.ME_FOLLOWERS.uri);
        expect(result.success).toBeTrue();
        expect(result.synced_at).toBeGreaterThan(0l);
        expect(Content.USERS).toHaveCount(0);
        expect(Content.ME_FOLLOWERS).toHaveCount(0);

        addIdResponse("/me/followers/ids?linked_partitioning=1");
        addCannedResponse(ApiSyncServiceTest.class, "/me/followers?linked_partitioning=1&limit=" + Consts.LIST_PAGE_SIZE, "empty_collection.json");
        result = sync(Content.ME_FOLLOWERS.uri);
        expect(result.success).toBe(true);
        expect(result.change).toEqual(ApiSyncResult.UNCHANGED);
        expect(result.synced_at).toBeGreaterThan(0l);
    }


    @Test
    public void shouldReturnReorderedForUsersIfLocalStateEqualsRemote() throws Exception {
        addIdResponse("/me/followers/ids?linked_partitioning=1", 792584, 1255758, 308291);
        addCannedResponse(ApiSyncServiceTest.class, "/me/followers?linked_partitioning=1&limit=" + Consts.LIST_PAGE_SIZE, "users.json");

        ApiSyncResult result = sync(Content.ME_FOLLOWERS.uri);
        expect(result.success).toBeTrue();
        expect(result.synced_at).toBeGreaterThan(0l);

        // make sure tracks+users got written
        expect(Content.USERS).toHaveCount(3);
        expect(Content.ME_FOLLOWERS).toHaveCount(3);
        assertFirstIdToBe(Content.ME_FOLLOWERS, 308291);

        addIdResponse("/me/followers/ids?linked_partitioning=1", 792584, 1255758, 308291);
        addCannedResponse(ApiSyncServiceTest.class, "/me/followers?linked_partitioning=1&limit=" + Consts.LIST_PAGE_SIZE, "users.json");
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
        when(mockUserAssociation.getUser()).thenReturn(new PublicApiUser(1L));
        expect(userAssociationSyncer.pushUserAssociation(mockUserAssociation)).toBeTrue();
        verify(userAssociationStorage).setFollowingAsSynced(mockUserAssociation);
    }

    @Test
    public void shouldClearSyncStateAndReturnSuccessFromAdditionPushWithResponseCreated() throws Exception {
        Robolectric.setDefaultHttpResponse(HttpStatus.SC_CREATED, "created");
        when(mockUserAssociation.getLocalSyncState()).thenReturn(UserAssociation.LocalState.PENDING_ADDITION);
        when(mockUserAssociation.getUser()).thenReturn(new PublicApiUser(1L));
        expect(userAssociationSyncer.pushUserAssociation(mockUserAssociation)).toBeTrue();
        verify(userAssociationStorage).setFollowingAsSynced(mockUserAssociation);
    }

    @Test
    public void shouldClearSyncStateAndReturnSuccessFromSuccesfulRemovalPushWithResponsOk() throws Exception {
        Robolectric.setDefaultHttpResponse(HttpStatus.SC_OK, "ok");
        when(mockUserAssociation.getLocalSyncState()).thenReturn(UserAssociation.LocalState.PENDING_REMOVAL);
        when(mockUserAssociation.getUser()).thenReturn(new PublicApiUser(1L));
        expect(userAssociationSyncer.pushUserAssociation(mockUserAssociation)).toBeTrue();
        verify(userAssociationStorage).setFollowingAsSynced(mockUserAssociation);
    }

    @Test
    public void shouldClearSyncStateAndReturnSuccessFroRemovalPushWithResponseNotFound() throws Exception {
        Robolectric.setDefaultHttpResponse(HttpStatus.SC_NOT_FOUND, "not found");
        when(mockUserAssociation.getLocalSyncState()).thenReturn(UserAssociation.LocalState.PENDING_REMOVAL);
        when(mockUserAssociation.getUser()).thenReturn(new PublicApiUser(1L));
        expect(userAssociationSyncer.pushUserAssociation(mockUserAssociation)).toBeTrue();
        verify(userAssociationStorage).setFollowingAsSynced(mockUserAssociation);
    }

    @Test
    public void shouldNotClearSyncStateAndReturnSuccessFromCleanAssociation() throws Exception {
        Robolectric.setDefaultHttpResponse(HttpStatus.SC_OK, "ok");
        when(mockUserAssociation.getLocalSyncState()).thenReturn(UserAssociation.LocalState.NONE);
        when(mockUserAssociation.getUser()).thenReturn(new PublicApiUser(1L));
        expect(userAssociationSyncer.pushUserAssociation(mockUserAssociation)).toBeTrue();
        verify(userAssociationStorage, never()).setFollowingAsSynced(mockUserAssociation);
    }

    @Test
    public void shouldReturnSuccessOnEmptyAssociationPush() throws Exception {
        when(userAssociationStorage.hasFollowingsNeedingSync()).thenReturn(false);
        expect(pushSyncMockedStorage(Content.ME_FOLLOWERS.uri).success).toBeTrue();
    }

    @Test
    public void shouldDeleteAssociationsPushedThatReceiveForbiddenResponse() throws Exception {
        ApiRequestException apiRequestException = ApiRequestException.notAllowed(mock(ApiRequest.class));
        List<UserAssociation> userAssociations = getDirtyUserAssociations();

        UserAssociationSyncer.BulkFollowSubscriber bulkFollowObserver = new UserAssociationSyncer.BulkFollowSubscriber(userAssociations, userAssociationStorage, followingOperations);
        final ArrayList<PublicApiUser> users = Lists.newArrayList(userAssociations.get(0).getUser(), userAssociations.get(1).getUser(), userAssociations.get(2).getUser());

        bulkFollowObserver.onError(apiRequestException);
        verify(followingOperations).updateLocalStatus(false, ScModel.getIdList(users));
        verify(userAssociationStorage).deleteFollowings(userAssociations);
    }

    private List<UserAssociation> getDirtyUserAssociations() {
        List<UserAssociation> usersAssociations = ModelFixtures.createDirtyFollowings(3);
        for (UserAssociation association : usersAssociations) {
            association.markForAddition();
        }
        return usersAssociations;
    }

    @Test
    public void shouldNotSetFailedAssociationPushesAsSynced() throws Exception {
        Robolectric.setDefaultHttpResponse(500, "error");
        when(userAssociationStorage.hasFollowingsNeedingSync()).thenReturn(true);

        final List<UserAssociation> usersAssociations = ModelFixtures.createDirtyFollowings(3);
        for (UserAssociation association : usersAssociations) association.markForAddition();

        when(userAssociationStorage.getFollowingsNeedingSync()).thenReturn(usersAssociations);
        when(followingOperations.bulkFollowAssociations(usersAssociations)).thenReturn(
                Observable.<Collection<UserAssociation>>empty());

        expect(pushSyncMockedStorage(Content.ME_FOLLOWINGS.uri).success).toBeFalse();
    }

    @Test(expected = IOException.class)
    public void shouldThrowIOException() throws Exception {
        Robolectric.setDefaultHttpResponse(500, "error");
        sync(Content.ME_FOLLOWERS.uri);
    }

    @Test
    public void shouldNotBulkFollowIfNoUserAssociations() throws IOException {
        when(userAssociationStorage.hasFollowingsNeedingSync()).thenReturn(false);
        userAssociationSyncer.syncContent(Content.ME_FOLLOWINGS.uri, ApiSyncService.ACTION_PUSH);
        verifyZeroInteractions(suggestedUsersOperations);
    }

    @Test
    public void shouldBulkFollowAllAssociations() throws IOException {
        final ArrayList<UserAssociation> userAssociations = newArrayList(userAssociation, userAssociation, userAssociation);
        when(userAssociationStorage.hasFollowingsNeedingSync()).thenReturn(true);
        when(userAssociationStorage.getFollowingsNeedingSync()).thenReturn(userAssociations);
        when(followingOperations.bulkFollowAssociations(userAssociations)).thenReturn(
                Observable.<Collection<UserAssociation>>empty());
        userAssociationSyncer.syncContent(Content.ME_FOLLOWINGS.uri, ApiSyncService.ACTION_PUSH);
        verify(followingOperations).bulkFollowAssociations(userAssociations);
    }

    @Test
    public void shouldSetResultAsErrorIfBulkFollowFails() throws IOException {
        when(userAssociation.hasToken()).thenReturn(true, true);
        when(userAssociationStorage.hasFollowingsNeedingSync()).thenReturn(true);
        when(userAssociationStorage.getFollowingsNeedingSync()).thenReturn(newArrayList(userAssociation, userAssociation));
        when(followingOperations.bulkFollowAssociations(anyListOf(UserAssociation.class))).thenReturn(Observable.<Collection<UserAssociation>>error(new IOException()));
        expect(userAssociationSyncer.syncContent(Content.ME_FOLLOWINGS.uri, ApiSyncService.ACTION_PUSH).success).toBeFalse();
    }

    @Test
    public void shouldSetResultAsSuccessIfBulkFollowSucceeds() throws IOException {
        when(userAssociation.hasToken()).thenReturn(true, true);
        when(userAssociationStorage.hasFollowingsNeedingSync()).thenReturn(true);
        when(userAssociationStorage.getFollowingsNeedingSync()).thenReturn(newArrayList(userAssociation, userAssociation));
        when(followingOperations.bulkFollowAssociations(anyListOf(UserAssociation.class))).thenReturn(Observable.<Collection<UserAssociation>>empty());
        expect(userAssociationSyncer.syncContent(Content.ME_FOLLOWINGS.uri, ApiSyncService.ACTION_PUSH).success).toBeTrue();
    }

    private ApiSyncResult sync(Uri uri, String... fixtures) throws IOException {
        addPendingHttpResponse(ApiSyncServiceTest.class, fixtures);
        UserAssociationSyncer syncer = new UserAssociationSyncer(
                Robolectric.application, accountOperations, followingOperations, notificationManager, jsonTransformer,
                navigator);
        return syncer.syncContent(uri, Intent.ACTION_SYNC);
    }

    private ApiSyncResult pushSyncMockedStorage(Uri uri, String... fixtures) throws IOException {
        addPendingHttpResponse(ApiSyncServiceTest.class, fixtures);
        return userAssociationSyncer.syncContent(uri, ApiSyncService.ACTION_PUSH);
    }

}
