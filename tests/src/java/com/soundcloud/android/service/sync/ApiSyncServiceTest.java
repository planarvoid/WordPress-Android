package com.soundcloud.android.service.sync;


import static com.soundcloud.android.Expect.expect;
import static com.soundcloud.android.robolectric.TestHelper.addCannedResponse;
import static com.soundcloud.android.robolectric.TestHelper.assertContentUriCount;

import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.model.Activities;
import com.soundcloud.android.model.LocalCollection;
import com.soundcloud.android.provider.Content;
import com.soundcloud.android.robolectric.DefaultTestRunner;
import com.soundcloud.android.robolectric.TestHelper;
import com.xtremelabs.robolectric.Robolectric;
import com.xtremelabs.robolectric.tester.org.apache.http.TestHttpResponse;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import android.content.ContentResolver;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.ResultReceiver;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;

@RunWith(DefaultTestRunner.class)
//@DatabaseConfig.UsingDatabaseMap(FileMap.class)
public class ApiSyncServiceTest {
    ContentResolver resolver;

    @Before public void before() {
        resolver = Robolectric.application.getContentResolver();
    }

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

        svc.onStart(intent, 0);

        expect(received.size()).toBe(1);
        expect(received.containsKey(ApiSyncService.STATUS_SYNC_ERROR)).toBeFalse();
    }

    @Test
    public void shouldQueue() throws Exception {
        ApiSyncService svc = new ApiSyncService();

        SoundCloudApplication app = DefaultTestRunner.application;

        Intent intent = new Intent(Intent.ACTION_SYNC);
        ArrayList<Uri> urisToSync = new ArrayList<Uri>();
        urisToSync.add(Content.ME_TRACKS.uri);
        urisToSync.add(Content.ME_FAVORITES.uri);
        urisToSync.add(Content.ME_FOLLOWERS.uri);

        intent.putParcelableArrayListExtra(ApiSyncService.EXTRA_SYNC_URIS, urisToSync);
        ApiSyncService.ApiSyncRequest request1 = new ApiSyncService.ApiSyncRequest(app, intent);
        ApiSyncService.ApiSyncRequest request2 = new ApiSyncService.ApiSyncRequest(app, new Intent(Intent.ACTION_SYNC, Content.ME_FAVORITES.uri));
        ApiSyncService.ApiSyncRequest request3 = new ApiSyncService.ApiSyncRequest(app, new Intent(Intent.ACTION_SYNC, Content.ME_FOLLOWINGS.uri).putExtra(ApiSyncService.EXTRA_IS_UI_RESPONSE,true));

        svc.enqueueRequest(request1);
        expect(svc.mPendingUriRequests.size()).toBe(3);
        svc.enqueueRequest(request2);

        expect(svc.mPendingUriRequests.size()).toBe(3);
        svc.enqueueRequest(request3);
        expect(svc.mPendingUriRequests.size()).toBe(4);
        expect(svc.mPendingUriRequests.poll().getUri()).toBe(Content.ME_FOLLOWINGS.uri);
        expect(svc.mPendingUriRequests.size()).toBe(3);
    }

    @Test
    public void shouldRemove() throws Exception {
        ApiSyncService svc = new ApiSyncService();

        svc.mRunningRequestUris.add(Content.ME_FAVORITES.uri);
        svc.mRunningRequestUris.add(Content.ME_FOLLOWINGS.uri);

        ApiSyncService.UriSyncRequest.Result result = new ApiSyncService.UriSyncRequest.Result(Content.ME_FAVORITES.uri);
        result.success = true;

        svc.onUriSyncResult(result);
        expect(svc.mRunningRequestUris.size()).toBe(1);
    }

    @Test
    public void shouldSyncTracks() throws Exception {
        ApiSyncService svc = new ApiSyncService();

        addIdResponse("/me/tracks/ids?linked_partitioning=1", 1, 2, 3);
        addResourceResponse("/tracks?linked_partitioning=1&limit=200&ids=1%2C2%2C3", "tracks.json");

        svc.onStart(new Intent(Intent.ACTION_SYNC, Content.ME_TRACKS.uri), 1);
        // make sure tracks+users got written
        assertContentUriCount(Content.TRACKS, 3);
        assertContentUriCount(Content.COLLECTION_ITEMS, 3);
        assertContentUriCount(Content.USERS, 1);
    }

    @Test
    public void shouldSyncActivitiesIncoming() throws Exception {
        ApiSyncService svc = new ApiSyncService();
        sync(svc, Content.ME_SOUND_STREAM,
                "incoming_1.json",
                "incoming_2.json");

        assertContentUriCount(Content.COLLECTIONS, 1);
        LocalCollection collection = LocalCollection.fromContent(Content.ME_SOUND_STREAM, resolver);

        expect(collection).not.toBeNull();
        expect(collection.last_sync).toBeGreaterThan(0L);
        expect(collection.sync_state).toEqual("https://api.soundcloud.com/me/activities/tracks?uuid[to]=future-href-incoming-1");

        assertContentUriCount(Content.ME_SOUND_STREAM, 100);
        assertContentUriCount(Content.ME_EXCLUSIVE_STREAM, 1);
        assertContentUriCount(Content.TRACKS, 99);
        assertContentUriCount(Content.USERS, 52);

        Activities incoming = Activities.get(Content.ME_SOUND_STREAM, resolver, -1);

        expect(incoming.size()).toEqual(100);
    }

    @Test
    public void shouldSyncActivitiesOwn() throws Exception {
        ApiSyncService svc = new ApiSyncService();

        sync(svc, Content.ME_ACTIVITIES,
                "own_1.json",
                "own_2.json");

        assertContentUriCount(Content.ME_ACTIVITIES, 42);
        assertContentUriCount(Content.COMMENTS, 15);

        Activities own = Activities.get(Content.ME_ACTIVITIES, resolver, -1);
        expect(own.size()).toEqual(42);
    }

    @Test
    public void shouldSyncDifferentEndoints() throws Exception {
        ApiSyncService svc = new ApiSyncService();
        sync(svc, Content.ME_ACTIVITIES,
                "own_1.json",
                "own_2.json");

        sync(svc, Content.ME_SOUND_STREAM,
                "incoming_1.json",
                "incoming_2.json");

        assertContentUriCount(Content.ME_SOUND_STREAM, 100);
        assertContentUriCount(Content.ME_ACTIVITIES, 42);
        assertContentUriCount(Content.ME_EXCLUSIVE_STREAM, 1);
        assertContentUriCount(Content.ME_ALL_ACTIVITIES, 142);
    }

    @Test
    public void shouldSyncActivitiesWithFutureHref() throws Exception {
        ApiSyncService svc = new ApiSyncService();

        sync(svc, Content.ME_SOUND_STREAM,
                "incoming_1.json",
                "incoming_2.json");

        assertContentUriCount(Content.COLLECTIONS, 1);
        LocalCollection collection = LocalCollection.fromContent(Content.ME_SOUND_STREAM, resolver);

        expect(collection).not.toBeNull();
        expect(collection.last_sync).toBeGreaterThan(0L);
        expect(collection.sync_state).toEqual("https://api.soundcloud.com/me/activities/tracks?uuid[to]=future-href-incoming-1");

        addCannedResponse(SyncAdapterServiceTest.class,
                "https://api.soundcloud.com/me/activities/tracks?uuid%5Bto%5D=future-href-incoming-1&limit=20",
                "empty_events.json");

        // next sync request should go this url
        sync(svc, Content.ME_SOUND_STREAM);

        assertContentUriCount(Content.COLLECTIONS, 1);
        collection = LocalCollection.fromContent(Content.ME_SOUND_STREAM, resolver);
        expect(collection.sync_state).toEqual("https://api.soundcloud.com/me/activities/tracks?uuid[to]=e46666c4-a7e6-11e0-8c30-73a2e4b61738");
    }

    private void addResourceResponse(String url, String resource) throws IOException {
        TestHelper.addCannedResponse(getClass(), url, resource);
    }

    private void sync(ApiSyncService svc, Content content, String... fixtures) throws IOException {
        TestHelper.addCannedResponses(SyncAdapterServiceTest.class, fixtures);
        svc.onStart(new Intent(Intent.ACTION_SYNC, content.uri), 1);
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
}
