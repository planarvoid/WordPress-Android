package com.soundcloud.android.sync;


import static com.soundcloud.android.Expect.expect;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.when;

import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.android.storage.provider.Content;
import com.xtremelabs.robolectric.Robolectric;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.ResultReceiver;

import java.util.ArrayList;

@RunWith(SoundCloudTestRunner.class)
public class SyncIntentTest {
    @Mock CollectionSyncRequest.Factory collectionSyncRequestFactory;

    @Test
    public void shouldCreateCollectionSyncRequests() throws Exception {
        SyncIntent req = new SyncIntent(new Intent(Intent.ACTION_SYNC, Content.ME_FOLLOWERS.uri), collectionSyncRequestFactory);

        expect(req.collectionSyncRequests.size()).toEqual(1);
    }

    @Test
    public void shouldCreateCollectionSyncRequestsFromExtraSyncUris() throws Exception {
        final Intent intent = new Intent(Intent.ACTION_SYNC);

        ArrayList<Uri> uris = new ArrayList<Uri>();
        uris.add(Content.ME_FOLLOWERS.uri);
        uris.add(Content.ME_LIKES.uri);

        intent.putParcelableArrayListExtra(ApiSyncService.EXTRA_SYNC_URIS, uris);
        SyncIntent req = new SyncIntent(intent, collectionSyncRequestFactory);

        expect(req.collectionSyncRequests.size()).toEqual(2);
    }

    @Test
    public void shouldCallResultReceiverWhenAllRequestsHaveBeenExecuted() throws Exception {
        final Boolean[] executed = new Boolean[1];
        final Intent intent = new Intent(Intent.ACTION_SYNC, Content.ME_FOLLOWERS.uri);
        intent.putExtra(ApiSyncService.EXTRA_STATUS_RECEIVER, new ResultReceiver(null) {
            @Override protected void onReceiveResult(int resultCode, Bundle resultData) {
                executed[0] = true;
            }
        });

        final CollectionSyncRequest collectionSyncRequest = new CollectionSyncRequest(Robolectric.application, Content.ME_FOLLOWERS.uri, null, false);
        when(collectionSyncRequestFactory.create(any(Uri.class), anyString(), anyBoolean())).thenReturn(collectionSyncRequest);

        SyncIntent req = new SyncIntent(intent, collectionSyncRequestFactory);

        expect(req.onUriResult(collectionSyncRequest)).toBeTrue();
        expect(executed[0]).toBeTrue();
    }
}

