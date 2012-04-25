package com.soundcloud.android.service.sync;


import static com.soundcloud.android.Expect.expect;

import com.soundcloud.android.provider.Content;
import com.soundcloud.android.robolectric.DefaultTestRunner;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(DefaultTestRunner.class)
public class CollectionSyncRequestTest {
    @Test
    public void shouldHaveEquals() throws Exception {
        CollectionSyncRequest r1 = new CollectionSyncRequest(DefaultTestRunner.application,
                Content.ME_FOLLOWER.uri, "someAction");

        CollectionSyncRequest r2 = new CollectionSyncRequest(DefaultTestRunner.application,
                Content.ME_FOLLOWER.uri, "someAction");

        CollectionSyncRequest r3 = new CollectionSyncRequest(DefaultTestRunner.application,
                Content.ME_FOLLOWER.uri, "someOtherAction");

        expect(r1).toEqual(r2);
        expect(r3).not.toEqual(r2);
    }

    @Test(expected = IllegalStateException.class)
    public void shouldThrowIllegalStateExceptionIfExecuteWithoutOnQueued() throws Exception {
        new CollectionSyncRequest(DefaultTestRunner.application,
                Content.ME_FOLLOWER.uri, "someAction").execute();
    }
}
