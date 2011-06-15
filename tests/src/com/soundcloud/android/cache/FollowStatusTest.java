package com.soundcloud.android.cache;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import com.soundcloud.android.robolectric.ApiTests;
import com.soundcloud.android.robolectric.DefaultTestRunner;
import com.xtremelabs.robolectric.Robolectric;
import org.junit.Test;
import org.junit.runner.RunWith;

import android.os.Handler;
import android.os.Message;


@RunWith(DefaultTestRunner.class)
public class FollowStatusTest extends ApiTests {
    static final String FOLLOWINGS = "[ 1, 2, 3, 4, 5 ]";

    @Test
    public void shouldCacheFollowerList() throws Exception {
        Robolectric.addPendingHttpResponse(200, FOLLOWINGS);
        FollowStatus status = new FollowStatus();

        final boolean[] called = new boolean[1];
        status.requestUserFollowings(api, new FollowStatus.Listener() {
                    @Override
                    public void onFollowings(boolean success, FollowStatus status) {
                        called[0] = true;
                        assertThat(success, is(true));
                        assertThat(status.isFollowing(1), is(true));
                        assertThat(status.isFollowing(2), is(true));
                        assertThat(status.isFollowing(6), is(false));

                    }
                }, false);
        assertThat(called[0], is(true));
    }

    @Test
    public void testToggleFollowing() throws Exception {
        FollowStatus status = new FollowStatus();
        assertThat(status.isFollowing(10), is(false));
        status.toggleFollowing(10);
        assertThat(status.isFollowing(10), is(true));
        status.toggleFollowing(10);
        assertThat(status.isFollowing(10), is(false));
    }

    @Test
    public void testToggleFollowingWithApiCall() throws Exception {
        FollowStatus status = new FollowStatus();
        Robolectric.addPendingHttpResponse(200, "OK");
        status.toggleFollowing(10, api, new Handler() {
            @Override
            public void handleMessage(Message msg) {
                assertThat(msg.arg1, is(1));
            }
        });
        assertThat(status.isFollowing(10), is(true));
    }

    @Test
    public void testToggleFollowingWithFailedApiCall() throws Exception {
        FollowStatus status = new FollowStatus();
        Robolectric.addPendingHttpResponse(500, "ERROR");
        status.toggleFollowing(23, api, null);
        assertThat(status.isFollowing(23), is(false));
    }

    @Test
    public void updateFollowing() throws Exception {
        FollowStatus status = new FollowStatus();
        status.updateFollowing(10, true);
        assertThat(status.isFollowing(10), is(true));
        status.updateFollowing(10, false);
        assertThat(status.isFollowing(10), is(false));
    }
}
