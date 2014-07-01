package com.soundcloud.android.associations;

import static com.soundcloud.android.Expect.expect;

import com.soundcloud.android.model.ScModel;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.model.User;
import com.soundcloud.android.storage.TableColumns;
import com.soundcloud.android.storage.provider.Content;
import com.soundcloud.android.robolectric.DefaultTestRunner;
import com.soundcloud.android.robolectric.TestHelper;
import com.soundcloud.android.sync.SyncStateManager;
import com.xtremelabs.robolectric.Robolectric;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;

import android.content.ContentValues;

import java.util.List;


@RunWith(DefaultTestRunner.class)
public class FollowStatusTest {

    final static long USER_ID = 1L;
    public static final long ID = 10L;
    private FollowStatus status;

    @Mock private SyncStateManager syncStateManager;

    @Before
    public void before() {
        status = new FollowStatus(Robolectric.application, syncStateManager);
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
            cv[i].put(TableColumns.CollectionItems.POSITION, i);
            cv[i].put(TableColumns.CollectionItems.ITEM_ID, i);
            cv[i].put(TableColumns.CollectionItems.USER_ID, USER_ID);
        }

        Robolectric.application.getContentResolver().bulkInsert(Content.ME_LIKES.uri, cv);


        final boolean[] called = new boolean[1];
        status.requestUserFollowings(new FollowingOperations.FollowStatusChangedListener() {
            @Override
            public void onFollowChanged() {
                called[0] = true;
                expect(status.isFollowing(Urn.forUser(1))).toBeTrue();
                expect(status.isFollowing(Urn.forUser(2))).toBeTrue();
                expect(status.isFollowing(Urn.forUser(6))).toBeFalse();

            }
        });
        expect(called[0]).toBeTrue();
    }

    @Test
    public void testToggleFollowing() throws Exception {
        final User user = new User(ID);
        expect(status.isFollowing(user)).toBeFalse();
        status.toggleFollowing(user.getId());
        expect(status.isFollowing(user)).toBeTrue();
        status.toggleFollowing(user.getId());
        expect(status.isFollowing(user)).toBeFalse();
    }

    @Test
    public void testToggleMultipleFollowings() throws Exception {
        List<User> users = TestHelper.createUsers(3);
        expect(status.isFollowing(users.get(0))).toBeFalse();

        status.toggleFollowing(users.get(0).getId());
        expect(status.isFollowing(users.get(0))).toBeTrue();

        status.toggleFollowing(ScModel.getIdList(users));
        expect(status.isFollowing(users.get(0))).toBeFalse();
        expect(status.isFollowing(users.get(1))).toBeTrue();
        expect(status.isFollowing(users.get(2))).toBeTrue();
    }
}
