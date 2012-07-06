package com.soundcloud.android.service.sync;


import static com.soundcloud.android.Expect.expect;

import com.soundcloud.android.AndroidCloudAPI;
import com.soundcloud.android.provider.Content;
import com.soundcloud.android.robolectric.DefaultTestRunner;
import com.xtremelabs.robolectric.Robolectric;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.net.URLEncoder;

@RunWith(DefaultTestRunner.class)
public class CollectionSyncRequestTest {

    static final String NON_INTERACTIVE =
            "&"+ URLEncoder.encode(AndroidCloudAPI.Wrapper.BACKGROUND_PARAMETER) + "=1";

    @Test
    public void shouldHaveEquals() throws Exception {
        CollectionSyncRequest r1 = new CollectionSyncRequest(DefaultTestRunner.application,
                Content.ME_FOLLOWER.uri, "someAction", false);

        CollectionSyncRequest r2 = new CollectionSyncRequest(DefaultTestRunner.application,
                Content.ME_FOLLOWER.uri, "someAction", false);

        CollectionSyncRequest r3 = new CollectionSyncRequest(DefaultTestRunner.application,
                Content.ME_FOLLOWER.uri, "someOtherAction", false);

        expect(r1).toEqual(r2);
        expect(r3).not.toEqual(r2);
    }

    @Test(expected = IllegalStateException.class)
    public void shouldThrowIllegalStateExceptionIfExecuteWithoutOnQueued() throws Exception {
        new CollectionSyncRequest(DefaultTestRunner.application,
                Content.ME_FOLLOWER.uri, "someAction", false).execute();
    }


    @Test
    public void shouldSetTheBackgroundParameterIfNonUiRequest() throws Exception {
        CollectionSyncRequest nonUi = new CollectionSyncRequest(DefaultTestRunner.application,
                Content.ME_FOLLOWER.uri, "someAction", false);
        CollectionSyncRequest ui = new CollectionSyncRequest(DefaultTestRunner.application,
                Content.ME_FOLLOWER.uri, "someAction", true);

        ui.onQueued();
        nonUi.onQueued();

        Robolectric.addHttpResponseRule("/me/followers/ids?linked_partitioning=1"
                + NON_INTERACTIVE, "whatevs");

        nonUi.execute();
        Robolectric.clearHttpResponseRules();
        Robolectric.addHttpResponseRule("/me/followers/ids?linked_partitioning=1", "whatevs");
        ui.execute();
    }
}
