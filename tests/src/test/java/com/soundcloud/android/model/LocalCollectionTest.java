package com.soundcloud.android.model;

import android.net.Uri;
import com.soundcloud.android.provider.Content;
import com.soundcloud.android.robolectric.DefaultTestRunner;
import com.soundcloud.android.service.sync.SyncConfig;
import org.junit.Test;
import org.junit.runner.RunWith;

import static com.soundcloud.android.Expect.expect;

@RunWith(DefaultTestRunner.class)
public class LocalCollectionTest {

    @Test
    public void shouldSupportEqualsAndHashcode() throws Exception {
        LocalCollection c1 = new LocalCollection(1, Uri.parse("foo"), 1, 1, 0, 0, null);
        LocalCollection c2 = new LocalCollection(1, Uri.parse("foo"), 1, 1, 0, 0, null);
        LocalCollection c3 = new LocalCollection(100, Uri.parse("foo"), 1, 1, 0, 0, null);
        expect(c1).toEqual(c2);
        expect(c2).not.toEqual(c3);
    }


    @Test
    public void shouldAutoRefreshFavorites() throws Exception {
        LocalCollection lc = new LocalCollection(1,
                Content.ME_LIKES.uri,
                System.currentTimeMillis() - SyncConfig.DEFAULT_ATTEMPT_DELAY,
                0, 0, 100, null);
        expect(lc.shouldAutoRefresh()).toBeTrue();
    }

    @Test
    public void shouldNotAutoRefreshFavorites() throws Exception {
        LocalCollection lc = new LocalCollection(1,
                Content.ME_LIKES.uri,
                System.currentTimeMillis(),
                0, 0, 100, null);
        expect(lc.shouldAutoRefresh()).toBeFalse();
    }

    @Test
    public void shouldNotAutoRefreshFavorites2() throws Exception {
        LocalCollection lc = new LocalCollection(1,
                Content.ME_LIKES.uri,
                System.currentTimeMillis() - SyncConfig.DEFAULT_ATTEMPT_DELAY,
                System.currentTimeMillis() - SyncConfig.TRACK_STALE_TIME + 10000,
                0, 100, null);
        expect(lc.shouldAutoRefresh()).toBeFalse();
    }

    @Test
    public void shouldAutoRefreshFollowings() throws Exception {
        LocalCollection lc = new LocalCollection(1, Content.ME_FOLLOWINGS.uri, 1, 0, 0, 100, null);
        expect(lc.shouldAutoRefresh()).toBeTrue();
    }

    @Test
    public void shouldNotAutoRefreshFollowings() throws Exception {
        LocalCollection lc = new LocalCollection(1, Content.ME_FOLLOWINGS.uri, 0, 1, 0, 100, null);
        expect(lc.shouldAutoRefresh()).toBeFalse();
    }
}
