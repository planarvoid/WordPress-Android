package com.soundcloud.android.sync.affiliations;

import static com.soundcloud.android.Expect.expect;
import static com.soundcloud.android.testsupport.TestHelper.addCannedResponse;
import static com.soundcloud.android.testsupport.TestHelper.addIdResponse;
import static com.soundcloud.android.testsupport.TestHelper.addPendingHttpResponse;
import static com.soundcloud.android.testsupport.TestHelper.assertFirstIdToBe;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.Consts;
import com.soundcloud.android.Navigator;
import com.soundcloud.android.accounts.AccountOperations;
import com.soundcloud.android.api.ApiResponse;
import com.soundcloud.android.api.json.JsonTransformer;
import com.soundcloud.android.api.legacy.model.Association;
import com.soundcloud.android.api.legacy.model.PublicApiUser;
import com.soundcloud.android.api.legacy.model.UserAssociation;
import com.soundcloud.android.associations.NextFollowingOperations;
import com.soundcloud.android.robolectric.DefaultTestRunner;
import com.soundcloud.android.storage.LegacyUserAssociationStorage;
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

import android.app.NotificationManager;
import android.content.ContentResolver;
import android.content.Intent;
import android.net.Uri;

import java.io.IOException;
import java.util.List;

@RunWith(DefaultTestRunner.class)
public class MyFollowingsSyncerTest {

    private MyFollowingsSyncer userAssociationSyncer;

    @Mock private ContentResolver resolver;
    @Mock private LegacyUserAssociationStorage userAssociationStorage;
    @Mock private UserAssociation mockUserAssociation;
    @Mock private AccountOperations accountOperations;
    @Mock private UserAssociation userAssociation;
    @Mock private PublicApiUser user;
    @Mock private NextFollowingOperations nextFollowingOperations;
    @Mock private ApiResponse apiResponse;
    @Mock private NotificationManager notificationManager;
    @Mock private JsonTransformer jsonTransformer;
    @Mock private Navigator navigator;

    @Before
    public void before() {
        TestHelper.setUserId(133201L);
        userAssociationSyncer = new MyFollowingsSyncer(Robolectric.application,
                resolver, userAssociationStorage, accountOperations, nextFollowingOperations,
                notificationManager, jsonTransformer, navigator);
        when(userAssociation.getUser()).thenReturn(user);
        when(userAssociation.getLocalSyncState()).thenReturn(UserAssociation.LocalState.NONE);
        when(accountOperations.isUserLoggedIn()).thenReturn(true);
        when(accountOperations.getLoggedInUserId()).thenReturn(133201L);
    }

    @Test
    public void shouldNotSyncWithNoLoggedInUser() throws Exception {
        when(accountOperations.isUserLoggedIn()).thenReturn(false);
        ApiSyncResult result = sync();
        expect(result.success).toBeFalse();
    }

    @Test
    public void shouldSyncFollowers() throws Exception {
        addIdResponse("/me/followings/ids?linked_partitioning=1", 792584, 1255758, 308291);
        addCannedResponse(ApiSyncServiceTest.class, "/me/followings?linked_partitioning=1&limit=" + Consts.LIST_PAGE_SIZE, "users.json");

        ApiSyncResult result = sync();
        expect(result.success).toBeTrue();
        expect(result.synced_at).toBeGreaterThan(0l);
        expect(Content.USERS).toHaveCount(3);

        List<PublicApiUser> followers = TestHelper.loadLocalContent(Content.ME_FOLLOWINGS.uri, PublicApiUser.class);
        expect(followers.get(0).getId()).toEqual(308291l);
        for (PublicApiUser u : followers) {
            expect(u.isStale()).toBeFalse();
        }
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

        expect(sync().success).toBeTrue();

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

        expect(sync().success).toBeTrue();

        expect(TestHelper.getUserAssociationByTargetId(Content.ME_FOLLOWINGS.uri, user_id).getLocalSyncState())
                .toEqual(UserAssociation.LocalState.PENDING_REMOVAL);
    }

    @Test
    public void shouldReturnSuccessAndUnchangedResultForRepeatEmptySync() throws Exception {
        addIdResponse("/me/followings/ids?linked_partitioning=1");
        addCannedResponse(ApiSyncServiceTest.class, "/me/followings?linked_partitioning=1&limit=" + Consts.LIST_PAGE_SIZE, "empty_collection.json");

        ApiSyncResult result = sync();
        expect(result.success).toBeTrue();
        expect(result.synced_at).toBeGreaterThan(0l);
        expect(Content.USERS).toHaveCount(0);
        expect(Content.ME_FOLLOWINGS).toHaveCount(0);

        addIdResponse("/me/followings/ids?linked_partitioning=1");
        addCannedResponse(ApiSyncServiceTest.class, "/me/followings?linked_partitioning=1&limit=" + Consts.LIST_PAGE_SIZE, "empty_collection.json");
        result = sync();
        expect(result.success).toBe(true);
        expect(result.change).toEqual(ApiSyncResult.UNCHANGED);
        expect(result.synced_at).toBeGreaterThan(0l);
    }


    @Test
    public void shouldReturnReorderedForUsersIfLocalStateEqualsRemote() throws Exception {
        addIdResponse("/me/followings/ids?linked_partitioning=1", 792584, 1255758, 308291);
        addCannedResponse(ApiSyncServiceTest.class, "/me/followings?linked_partitioning=1&limit=" + Consts.LIST_PAGE_SIZE, "users.json");

        ApiSyncResult result = sync();
        expect(result.success).toBeTrue();
        expect(result.synced_at).toBeGreaterThan(0l);

        // make sure tracks+users got written
        expect(Content.USERS).toHaveCount(3);
        expect(Content.ME_FOLLOWINGS).toHaveCount(3);
        assertFirstIdToBe(Content.ME_FOLLOWINGS, 308291);

        addIdResponse("/me/followings/ids?linked_partitioning=1", 792584, 1255758, 308291);
        addCannedResponse(ApiSyncServiceTest.class, "/me/followings?linked_partitioning=1&limit=" + Consts.LIST_PAGE_SIZE, "users.json");
        result = sync();
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
        expect(pushSyncMockedStorage(Content.ME_FOLLOWINGS.uri).success).toBeTrue();
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

        expect(pushSyncMockedStorage(Content.ME_FOLLOWINGS.uri).success).toBeFalse();
    }

    @Test(expected = IOException.class)
    public void shouldThrowIOException() throws Exception {
        Robolectric.setDefaultHttpResponse(500, "error");
        sync();
    }

    private ApiSyncResult sync() throws IOException {
        MyFollowingsSyncer syncer = new MyFollowingsSyncer(
                Robolectric.application, accountOperations, nextFollowingOperations, notificationManager, jsonTransformer,
                navigator);
        return syncer.syncContent(Content.ME_FOLLOWINGS.uri, Intent.ACTION_SYNC);
    }

    private ApiSyncResult pushSyncMockedStorage(Uri uri, String... fixtures) throws IOException {
        addPendingHttpResponse(ApiSyncServiceTest.class, fixtures);
        return userAssociationSyncer.syncContent(uri, ApiSyncService.ACTION_PUSH);
    }

}
