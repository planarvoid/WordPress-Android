package com.soundcloud.android.service.sync;

import static com.soundcloud.android.Expect.expect;
import static com.soundcloud.android.robolectric.TestHelper.addCannedResponse;
import static com.soundcloud.android.robolectric.TestHelper.addIdResponse;
import static com.soundcloud.android.robolectric.TestHelper.addPendingHttpResponse;
import static com.soundcloud.android.robolectric.TestHelper.assertFirstIdToBe;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.isNull;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import com.soundcloud.android.Consts;
import com.soundcloud.android.dao.ResolverHelper;
import com.soundcloud.android.model.User;
import com.soundcloud.android.provider.Content;
import com.soundcloud.android.robolectric.DefaultTestRunner;
import com.soundcloud.android.robolectric.TestHelper;
import com.soundcloud.android.service.sync.content.UserAssociationSyncer;
import com.xtremelabs.robolectric.Robolectric;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Intent;
import android.net.Uri;

import java.io.IOException;
import java.util.List;

@RunWith(DefaultTestRunner.class)
public class UserAssociationSyncerTest {
    private static final long USER_ID = 133201L;
    ContentResolver resolver;

    @Before
    public void before() {
        DefaultTestRunner.application.setCurrentUserId(USER_ID);
        resolver = DefaultTestRunner.application.getContentResolver();
    }

    @Test
    public void shouldSyncFollowers() throws Exception {
        addIdResponse("/me/followers/ids?linked_partitioning=1", 792584, 1255758, 308291);
        addCannedResponse(getClass(), "/me/followers?linked_partitioning=1&limit=" + Consts.COLLECTION_PAGE_SIZE, "users.json");

        ApiSyncResult result = sync(Content.ME_FOLLOWERS.uri);
        expect(result.success).toBeTrue();
        expect(result.synced_at).toBeGreaterThan(0l);
        expect(Content.USERS).toHaveCount(3);


        List<User> followers = TestHelper.loadLocalContent(Content.ME_FOLLOWERS.uri, User.class);
        expect(followers.get(0).id).toEqual(308291l);
        for (User u : followers){
            expect(u.isStale()).toBeFalse();
        }
    }

    @Test
    public void shouldSyncFollowersInSingleBatchIfCollectionIsSmallEnough() throws Exception {
        addIdResponse("/me/followers/ids?linked_partitioning=1", 792584, 1255758, 308291);
        addCannedResponse(getClass(), "/me/followers?linked_partitioning=1&limit=" + Consts.COLLECTION_PAGE_SIZE, "empty_collection.json");

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
        addCannedResponse(getClass(), "/me/followers?linked_partitioning=1&limit=" + Consts.COLLECTION_PAGE_SIZE, "empty_collection.json");

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
           TestHelper.addResourceResponse(getClass(), "/users?linked_partitioning=1&limit=200&ids=792584%2C1255758%2C308291", "users.json");

           sync(Content.ME_FRIENDS.uri);

           // make sure tracks+users got written
           expect(Content.USERS).toHaveCount(3);
           expect(Content.ME_FRIENDS).toHaveCount(3);
           assertFirstIdToBe(Content.ME_FRIENDS, 308291);
       }

    @Test
        public void shouldReturnReorderedForUsersIfLocalStateEqualsRemote() throws Exception {
            addIdResponse("/me/followers/ids?linked_partitioning=1", 792584, 1255758, 308291);
            addCannedResponse(getClass(), "/me/followers?linked_partitioning=1&limit=" + Consts.COLLECTION_PAGE_SIZE, "users.json");

            ApiSyncResult result = sync(Content.ME_FOLLOWERS.uri);
            expect(result.success).toBeTrue();
            expect(result.synced_at).toBeGreaterThan(0l);

            // make sure tracks+users got written
            expect(Content.USERS).toHaveCount(3);
            expect(Content.ME_FOLLOWERS).toHaveCount(3);
            assertFirstIdToBe(Content.ME_FOLLOWERS, 308291);


            addIdResponse("/me/followers/ids?linked_partitioning=1", 792584, 1255758, 308291);
            addCannedResponse(getClass(), "/me/followers?linked_partitioning=1&limit=" + Consts.COLLECTION_PAGE_SIZE, "users.json");
            result = sync(Content.ME_FOLLOWERS.uri);
            expect(result.success).toBe(true);
            expect(result.change).toEqual(ApiSyncResult.REORDERED);
            expect(result.synced_at).toBeGreaterThan(0l);
            expect(result.extra).toBeNull();
        }

    @Test(expected = IOException.class)
    public void shouldThrowIOException() throws Exception {
        Robolectric.setDefaultHttpResponse(500, "error");
        sync(Content.ME_FOLLOWERS.uri);
    }

    private ApiSyncResult sync(Uri uri, String... fixtures) throws IOException {
        addPendingHttpResponse(getClass(), fixtures);
        UserAssociationSyncer syncer = new UserAssociationSyncer(Robolectric.application, Robolectric.application.getContentResolver());
        return syncer.syncContent(uri, Intent.ACTION_SYNC);
    }

}
