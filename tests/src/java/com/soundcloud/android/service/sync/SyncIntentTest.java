package com.soundcloud.android.service.sync;


import static com.soundcloud.android.Expect.expect;

import com.soundcloud.android.provider.Content;
import com.soundcloud.android.robolectric.DefaultTestRunner;
import com.xtremelabs.robolectric.Robolectric;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import android.content.ContentResolver;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.ResultReceiver;

import java.util.ArrayList;

@RunWith(DefaultTestRunner.class)
public class SyncIntentTest {
    ContentResolver resolver;
    static final long USER_ID = 100L;

    @Before public void before() {
        resolver = Robolectric.application.getContentResolver();
        DefaultTestRunner.application.setCurrentUserId(USER_ID);
    }

    @Test
    public void shouldCreateCollectionSyncRequests() throws Exception {
        SyncIntent req = new SyncIntent(DefaultTestRunner.application,
                new Intent(Intent.ACTION_SYNC, Content.ME_FOLLOWERS.uri));

        expect(req.collectionSyncRequests.size()).toEqual(1);
    }

    @Test
    public void shouldCreateCollectionSyncRequestsFromExtraSyncUris() throws Exception {
        final Intent intent = new Intent(Intent.ACTION_SYNC);

        ArrayList<Uri> uris = new ArrayList<Uri>();
        uris.add(Content.ME_FOLLOWERS.uri);
        uris.add(Content.ME_FAVORITES.uri);

        intent.putParcelableArrayListExtra(ApiSyncService.EXTRA_SYNC_URIS, uris);
        SyncIntent req = new SyncIntent(DefaultTestRunner.application, intent);

        expect(req.collectionSyncRequests.size()).toEqual(2);
    }

    @Test
    public void shouldCallResultReceiverWhenAllRequestsHaveBeenExecuted() throws Exception {
        Robolectric.setDefaultHttpResponse(500, "error");

        final Boolean[] executed = new Boolean[1];
        final Intent intent = new Intent(Intent.ACTION_SYNC, Content.ME_FOLLOWERS.uri);
        intent.putExtra(ApiSyncService.EXTRA_STATUS_RECEIVER, new ResultReceiver(null) {
            @Override protected void onReceiveResult(int resultCode, Bundle resultData) {
                executed[0] = true;
            }
        });

        SyncIntent req = new SyncIntent(DefaultTestRunner.application, intent);

        final CollectionSyncRequest syncRequest = req.collectionSyncRequests.get(0);
        syncRequest.onQueued();

        expect(req.onUriResult(syncRequest.execute())).toBeTrue();
        expect(executed[0]).toBeTrue();
        expect(syncRequest.result.success).toBeFalse();
    }
}

