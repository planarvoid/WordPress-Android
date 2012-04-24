package com.soundcloud.android.service.sync;


import android.content.ContentResolver;
import android.content.Intent;
import android.net.Uri;
import com.soundcloud.android.provider.Content;
import com.soundcloud.android.robolectric.DefaultTestRunner;
import com.xtremelabs.robolectric.Robolectric;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import static com.soundcloud.android.Expect.expect;

@RunWith(DefaultTestRunner.class)
public class ApiSyncRequestTest {
    ContentResolver resolver;
    static final long USER_ID = 100L;

    @Before public void before() {
        resolver = Robolectric.application.getContentResolver();
        DefaultTestRunner.application.setCurrentUserId(USER_ID);
    }

    @Test
    public void shouldMarkResultAsChanged() throws Exception {

        final Uri uri = Content.ME_FOLLOWINGS.uri;
        ApiSyncRequest apiSyncRequest = new ApiSyncRequest(DefaultTestRunner.application,new Intent(Intent.ACTION_SYNC, uri).putExtra(ApiSyncService.EXTRA_IS_UI_RESPONSE,true));
        UriSyncRequest uriSyncRequest = apiSyncRequest.uriRequests.get(0);

        // setup result
        uriSyncRequest.result = new ApiSyncer.Result(uri);
        uriSyncRequest.result.requiresUiRefresh = true;
        uriSyncRequest.result.wasChanged = false;
        apiSyncRequest.onUriResult(uriSyncRequest);

        expect(apiSyncRequest.resultData.get(uri.toString())).toBe(true);
    }

    @Test
    public void shouldNotMarkResultAsChanged() throws Exception {

        final Uri uri = Content.ME_FOLLOWINGS.uri;
        ApiSyncRequest apiSyncRequest = new ApiSyncRequest(DefaultTestRunner.application,new Intent(Intent.ACTION_SYNC, uri).putExtra(ApiSyncService.EXTRA_IS_UI_RESPONSE,false));
        UriSyncRequest uriSyncRequest = apiSyncRequest.uriRequests.get(0);

        // setup result
        uriSyncRequest.result = new ApiSyncer.Result(uri);
        uriSyncRequest.result.requiresUiRefresh = true;
        uriSyncRequest.result.wasChanged = false;
        apiSyncRequest.onUriResult(uriSyncRequest);

        expect(apiSyncRequest.resultData.get(uri.toString())).toBe(false);
    }


}
