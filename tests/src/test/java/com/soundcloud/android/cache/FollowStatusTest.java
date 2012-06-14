package com.soundcloud.android.cache;

import static com.soundcloud.android.Expect.expect;

import com.soundcloud.android.robolectric.DefaultTestRunner;
import com.xtremelabs.robolectric.Robolectric;
import org.junit.Test;
import org.junit.runner.RunWith;

import android.os.Handler;
import android.os.Message;


@RunWith(DefaultTestRunner.class)
public class FollowStatusTest  {
    static final String FOLLOWINGS = "[ 1, 2, 3, 4, 5 ]";

    @Test
    public void shouldCacheFollowerList() throws Exception {
        Robolectric.addPendingHttpResponse(200, FOLLOWINGS);
        FollowStatus status = new FollowStatus();

        final boolean[] called = new boolean[1];
        status.requestUserFollowings(DefaultTestRunner.application, new FollowStatus.Listener() {
                    @Override
                    public void onChange(boolean success, FollowStatus status) {
                        called[0] = true;
                        expect(success).toBeTrue();
                        expect(status.isFollowing(1)).toBeTrue();
                        expect(status.isFollowing(2)).toBeTrue();
                        expect(status.isFollowing(6)).toBeFalse();

                    }
                }, false);
        expect(called[0]).toBeTrue();
    }

    @Test
    public void testToggleFollowing() throws Exception {
        FollowStatus status = new FollowStatus();
        expect(status.isFollowing(10)).toBeFalse();
        status.toggleFollowing(10);
        expect(status.isFollowing(10)).toBeTrue();
        status.toggleFollowing(10);
        expect(status.isFollowing(10)).toBeFalse();
    }

    @Test
    public void testToggleFollowingWithApiCall() throws Exception {
        FollowStatus status = new FollowStatus();
        Robolectric.addPendingHttpResponse(201, "CREATED");
        status.toggleFollowing(10, DefaultTestRunner.application, new Handler() {
            @Override
            public void handleMessage(Message msg) {
                expect(msg.what).toEqual(1);
            }
        });
        expect(status.isFollowing(10)).toBeTrue();
    }

    @Test
    public void testToggleFollowingWithFailedApiCall() throws Exception {
        FollowStatus status = new FollowStatus();
        Robolectric.addPendingHttpResponse(500, "ERROR");
        status.toggleFollowing(23, DefaultTestRunner.application, null);
        expect(status.isFollowing(23)).toBeFalse();
    }

    @Test
    public void updateFollowing() throws Exception {
        FollowStatus status = new FollowStatus();
        status.updateFollowing(10, true);
        expect(status.isFollowing(10)).toBeTrue();
        status.updateFollowing(10, false);
        expect(status.isFollowing(10)).toBeFalse();
    }

    @Test
    public void shouldGenerateFilename() throws Exception {
        expect(
                FollowStatus.getFilename(10)).toEqual("follow-status-cache-10");
    }
}
