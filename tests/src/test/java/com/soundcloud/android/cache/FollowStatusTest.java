package com.soundcloud.android.cache;

import static com.soundcloud.android.Expect.expect;

import com.soundcloud.android.model.User;
import com.soundcloud.android.model.UserAssociation;
import com.soundcloud.android.provider.Content;
import com.soundcloud.android.provider.DBHelper;
import com.soundcloud.android.robolectric.DefaultTestRunner;
import com.soundcloud.android.robolectric.TestHelper;
import com.xtremelabs.robolectric.Robolectric;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;

import android.content.ContentValues;

import java.util.List;


@RunWith(DefaultTestRunner.class)
public class FollowStatusTest {

    final static long USER_ID = 1L;
    public static final long ID = 10L;
    FollowStatus status;

    @Before
    public void before() {
        status = new FollowStatus(Robolectric.application);
    }

    @Test
    @Ignore
    public void shouldCacheFollowerList() throws Exception {
        // AsyncQueryHandler does not seem to be implemented in Robolectic yet. However, this query is tested elsewhere

        TestHelper.setUserId(USER_ID);

        final int SIZE = 5;
        ContentValues[] cv = new ContentValues[SIZE];
        for (int i = 0; i < SIZE; i++) {
            cv[i] = new ContentValues();
            cv[i].put(DBHelper.CollectionItems.POSITION, i);
            cv[i].put(DBHelper.CollectionItems.ITEM_ID, i);
            cv[i].put(DBHelper.CollectionItems.USER_ID, USER_ID);
        }

        Robolectric.application.getContentResolver().bulkInsert(Content.ME_LIKES.uri, cv);


        final boolean[] called = new boolean[1];
        status.requestUserFollowings(new FollowStatus.Listener() {
            @Override
            public void onFollowChanged() {
                called[0] = true;
                expect(status.isFollowing(1)).toBeTrue();
                expect(status.isFollowing(2)).toBeTrue();
                expect(status.isFollowing(6)).toBeFalse();

            }
        });
        expect(called[0]).toBeTrue();
    }

    @Test
    public void testToggleFollowing() throws Exception {
        expect(status.isFollowing(ID)).toBeFalse();
        final User user = new User(ID);
        status.toggleFollowing(user);
        checkFollowStatus(user, true);
        status.toggleFollowing(user);
        checkFollowStatus(user, false);
    }

    @Test
    public void testToggleMultipleFollowings() throws Exception {
        List<User> users = TestHelper.createUsers(3);
        checkFollowStatus(users.get(0), false);

        status.toggleFollowing(users.get(0));
        checkFollowStatus(users.get(0), true);

        status.toggleFollowing(users.toArray(new User[users.size()]));
        checkFollowStatus(users.get(0), false);
        checkFollowStatus(users.get(1), true);
        checkFollowStatus(users.get(2), true);
    }

    private void checkFollowStatus(User user, boolean shouldBeFollowing) {
        checkFollowStatus(user.getId(), shouldBeFollowing);
    }

    /**
     * Checks both the cache and the local storage
     */
    private void checkFollowStatus(long id, boolean shouldBeFollowing) {
        final UserAssociation userAssociation = TestHelper.getUserAssociationByTargetId(Content.ME_FOLLOWINGS.uri, id);
        if (shouldBeFollowing) {
            expect(status.isFollowing(id)).toBeTrue();
            expect(userAssociation.getLocalSyncState()).not.toEqual(UserAssociation.LocalState.PENDING_REMOVAL);
        } else {
            expect(status.isFollowing(id)).toBeFalse();
            if (userAssociation != null) {
                expect(userAssociation.getLocalSyncState()).toEqual(UserAssociation.LocalState.PENDING_REMOVAL);
            }
        }


    }
}
