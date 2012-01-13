package com.soundcloud.android.service.sync;


import static com.soundcloud.android.Expect.expect;
import static com.soundcloud.android.service.sync.ApiSyncServiceTest.Utils.assertContentUriCount;

import android.net.Uri;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.model.LocalCollection;
import com.soundcloud.android.provider.Content;
import com.soundcloud.android.robolectric.DefaultTestRunner;
import com.soundcloud.android.robolectric.FileMap;
import com.soundcloud.android.robolectric.TestHelper;
import com.xtremelabs.robolectric.Robolectric;
import com.xtremelabs.robolectric.tester.org.apache.http.TestHttpResponse;
import com.xtremelabs.robolectric.util.DatabaseConfig;
import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;

import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.ResultReceiver;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

@RunWith(DefaultTestRunner.class)
@DatabaseConfig.UsingDatabaseMap(FileMap.class)
public class ApiSyncServiceTest {

    @After public void after() {
        expect(Robolectric.getFakeHttpLayer().hasPendingResponses()).toBeFalse();
    }

    @Test
    public void shouldProvideFeedbackViaResultReceiver() throws Exception {
        ApiSyncService svc = new ApiSyncService();
        Intent intent = new Intent(Intent.ACTION_SYNC, Content.ME_TRACKS.uri);

        final LinkedHashMap<Integer, Bundle> received = new LinkedHashMap<Integer, Bundle>();
        intent.putExtra(ApiSyncService.EXTRA_STATUS_RECEIVER, new ResultReceiver(new Handler(Looper.myLooper())) {
            @Override
            protected void onReceiveResult(int resultCode, Bundle resultData) {
                received.put(resultCode, resultData);
            }
        });

        addIdResponse("/me/tracks/ids?linked_partitioning=1", 1, 2, 3);
        addResourceResponse("/tracks?linked_partitioning=1&limit=200&ids=1%2C2%2C3", "tracks.json");

        svc.onHandleIntent(intent);

        expect(received.size()).toBe(1);
        expect(received.containsKey(ApiSyncService.STATUS_SYNC_ERROR)).toBeFalse();
    }

    @Test
    public void shouldQueue() throws Exception {
        ApiSyncService svc = new ApiSyncService();

        SoundCloudApplication app = DefaultTestRunner.application;

        Intent intent = new Intent(Intent.ACTION_SYNC);
        List<String> urisToSync = new ArrayList<String>();
        urisToSync.add(Content.ME_TRACKS.uri.toString());
        urisToSync.add(Content.ME_FAVORITES.uri.toString());
        urisToSync.add(Content.ME_FOLLOWERS.uri.toString());
        intent.putStringArrayListExtra("syncUris", (ArrayList<String>) urisToSync);

        ApiSyncService.ApiSyncRequest request1 = new ApiSyncService.ApiSyncRequest(app, intent);
        ApiSyncService.ApiSyncRequest request2 = new ApiSyncService.ApiSyncRequest(app, new Intent(Intent.ACTION_SYNC, Content.ME_FAVORITES.uri));
        ApiSyncService.ApiSyncRequest request3 = new ApiSyncService.ApiSyncRequest(app, new Intent(Intent.ACTION_SYNC, Content.ME_FOLLOWINGS.uri));

        svc.enqueueRequest(request1);
        expect(svc.mUriRequests.size()).toBe(3);
        svc.enqueueRequest(request2);

        expect(svc.mUriRequests.size()).toBe(3);
        svc.enqueueRequest(request3);
        expect(svc.mUriRequests.size()).toBe(4);
    }

    @Test
    public void shouldRemove() throws Exception {
        ApiSyncService svc = new ApiSyncService();

        SoundCloudApplication app = DefaultTestRunner.application;

        svc.mRunningRequests.add(Content.ME_FAVORITES.uri);
        svc.mRunningRequests.add(Content.ME_FOLLOWINGS.uri);

        ApiSyncService.UriSyncRequest.Result result = new ApiSyncService.UriSyncRequest.Result(Content.ME_FAVORITES.uri);
        result.success = true;

        svc.onUriSyncResult(result);
        expect(svc.mRunningRequests.size()).toBe(1);
    }

    @Test
    public void shouldSync() throws Exception {
        ApiSyncService svc = new ApiSyncService();

        addIdResponse("/me/tracks/ids?linked_partitioning=1", 1, 2, 3);
        addResourceResponse("/tracks?linked_partitioning=1&limit=200&ids=1%2C2%2C3", "tracks.json");

        svc.onHandleIntent(new Intent(Intent.ACTION_SYNC, Content.ME_TRACKS.uri));
        // make sure tracks+users got written
        assertContentUriCount(Content.TRACKS, 3);
        assertContentUriCount(Content.COLLECTION_ITEMS, 3);
        assertContentUriCount(Content.USERS, 1);
    }

    @Test
    public void shouldSyncActivities() throws Exception {
        ApiSyncService svc = new ApiSyncService();

        TestHelper.addCannedResponses(SyncAdapterServiceTest.class,
                "incoming_1.json",
                "incoming_2.json");

        svc.onHandleIntent(new Intent(Intent.ACTION_SYNC, Content.ME_SOUND_STREAM.uri));

        assertContentUriCount(Content.COLLECTIONS, 1);
        LocalCollection collection = LocalCollection.fromContentUri(
                        Robolectric.application.getContentResolver(),
                        Content.ME_SOUND_STREAM.uri);

        expect(collection).not.toBeNull();
        expect(collection.last_sync).toBeGreaterThan(0L);
        expect(collection.sync_state).toEqual("https://api.soundcloud.com/me/activities/tracks?uuid[to]=e46666c4-a7e6-11e0-8c30-73a2e4b61738");

        assertContentUriCount(Content.ME_SOUND_STREAM, 100);
    }

    private void addResourceResponse(String url, String resource) throws IOException {
        TestHelper.addCannedResponse(getClass(), url, resource);
    }

    private void addIdResponse(String url, int... ids) {
        StringBuilder sb = new StringBuilder();
        sb.append("{ \"collection\": [");
        for (int i = 0; i < ids.length; i++) {
            sb.append(ids[i]);
            if (i < ids.length - 1) sb.append(", ");
        }
        sb.append("] }");
        Robolectric.addHttpResponseRule(url, new TestHttpResponse(200, sb.toString()));
    }


    static class Utils {
        public static void assertContentUriCount(Content content, int count) {
            Cursor c = Robolectric.application.getContentResolver().query(content.uri, null, null, null, null);
            expect(c).not.toBeNull();
            expect(c.getCount()).toBe(count);
        }
    }
}
