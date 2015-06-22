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
public class LegacySyncRequestTest {
    @Mock LegacySyncJob.Factory collectionSyncRequestFactory;
    @Mock ApiSyncerFactory apiSyncerFactory;
    @Mock SyncStateManager syncStateManager;

    @Test
    public void shouldCreateCollectionSyncRequests() throws Exception {
        LegacySyncRequest req = new LegacySyncRequest(new Intent(Intent.ACTION_SYNC, Content.ME_FOLLOWERS.uri), collectionSyncRequestFactory);

        expect(req.getPendingJobs().size()).toEqual(1);
    }

    @Test
    public void shouldCreateCollectionSyncRequestsFromExtraSyncUris() throws Exception {
        final Intent intent = new Intent(Intent.ACTION_SYNC);

        ArrayList<Uri> uris = new ArrayList<>();
        uris.add(Content.ME_FOLLOWERS.uri);
        uris.add(Content.ME_LIKES.uri);

        intent.putParcelableArrayListExtra(ApiSyncService.EXTRA_SYNC_URIS, uris);
        LegacySyncRequest req = new LegacySyncRequest(intent, collectionSyncRequestFactory);

        expect(req.getPendingJobs().size()).toEqual(2);
    }

    @Test
    public void isHighPriorityIsTrueForUIRequest() throws Exception {
        final Intent intent = new Intent(Intent.ACTION_SYNC);
        intent.putExtra(ApiSyncService.EXTRA_IS_UI_REQUEST,true);

        LegacySyncRequest req = new LegacySyncRequest(intent, collectionSyncRequestFactory);

        expect(req.isHighPriority()).toBeTrue();
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

        final LegacySyncJob legacySyncItem = new LegacySyncJob(
                Robolectric.application, Content.ME_FOLLOWERS.uri, null, false, apiSyncerFactory, syncStateManager);
        when(collectionSyncRequestFactory.create(any(Uri.class), anyString(), anyBoolean())).thenReturn(legacySyncItem);

        LegacySyncRequest req = new LegacySyncRequest(intent, collectionSyncRequestFactory);

        expect(req.isSatisfied()).toBeFalse();
        expect(req.isWaitingForJob(legacySyncItem)).toBeTrue();

        req.processJobResult(legacySyncItem);
        expect(req.isSatisfied()).toBeTrue();
        req.finish();

        expect(executed[0]).toBeTrue();
    }
}

