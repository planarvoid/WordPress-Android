package com.soundcloud.android.cache;

import static com.soundcloud.android.Expect.expect;

import com.soundcloud.android.model.User;
import com.soundcloud.android.provider.Content;
import com.soundcloud.android.provider.DBHelper;
import com.soundcloud.android.robolectric.DefaultTestRunner;
import com.xtremelabs.robolectric.Robolectric;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;

import android.content.ContentValues;
import android.os.Handler;
import android.os.Message;


@RunWith(DefaultTestRunner.class)
public class FollowStatusTest  {

    final static long USER_ID = 1L;

    @Test
    @Ignore
    public void shouldCacheFollowerList() throws Exception {
        // AsyncQueryHandler does not seem to be implemented in Robolectic yet. However, this query is tested elsewhere

        DefaultTestRunner.application.setCurrentUserId(USER_ID);

        final FollowStatus status = new FollowStatus(DefaultTestRunner.application);
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
        FollowStatus status = new FollowStatus(Robolectric.application);
        expect(status.isFollowing(10)).toBeFalse();
        final User user = new User(10);
        status.toggleFollowing(user);
        expect(status.isFollowing(user)).toBeTrue();
        status.toggleFollowing(user);
        expect(status.isFollowing(user)).toBeFalse();
    }

    @Test
    public void updateFollowing() throws Exception {
        FollowStatus status = new FollowStatus(Robolectric.application);
        status.updateFollowing(10, true);
        expect(status.isFollowing(10)).toBeTrue();
        status.updateFollowing(10, false);
        expect(status.isFollowing(10)).toBeFalse();
    }
}
