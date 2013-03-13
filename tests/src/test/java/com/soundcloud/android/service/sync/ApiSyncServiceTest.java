package com.soundcloud.android.service.sync;


import static com.soundcloud.android.Expect.expect;
import static com.soundcloud.android.robolectric.TestHelper.addCannedResponse;
import static com.soundcloud.android.robolectric.TestHelper.addIdResponse;

import com.soundcloud.android.dao.LocalCollectionDAO;
import com.soundcloud.android.model.LocalCollection;
import com.soundcloud.android.provider.Content;
import com.soundcloud.android.robolectric.DefaultTestRunner;
import com.soundcloud.android.robolectric.TestHelper;
import com.xtremelabs.robolectric.Robolectric;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import android.content.ContentResolver;
import android.content.Context;
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
public class ApiSyncServiceTest {
    ContentResolver resolver;
    static final long USER_ID = 100L;

    @Before public void before() {
        resolver = Robolectric.application.getContentResolver();
        DefaultTestRunner.application.setCurrentUserId(USER_ID);
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

        addIdResponse("/me/tracks/ids?linked_partitioning=1"+ CollectionSyncRequestTest.NON_INTERACTIVE, 1, 2, 3);
        addResourceResponse("/tracks?linked_partitioning=1&limit=200&ids=1%2C2%2C3"+ CollectionSyncRequestTest.NON_INTERACTIVE,
                "tracks.json");

        svc.onStart(intent, 0);

        expect(received.size()).toBe(1);
        expect(received.containsKey(ApiSyncService.STATUS_SYNC_ERROR)).toBeFalse();
    }

    @Test
    public void shouldProvideFeedbackViaResultReceiverNoSyncIntent() throws Exception {
        ApiSyncService svc = new ApiSyncService();
        Intent intent = new Intent(Intent.ACTION_SYNC, null);

        final LinkedHashMap<Integer, Bundle> received = new LinkedHashMap<Integer, Bundle>();
        intent.putExtra(ApiSyncService.EXTRA_STATUS_RECEIVER, new ResultReceiver(new Handler(Looper.myLooper())) {
            @Override
            protected void onReceiveResult(int resultCode, Bundle resultData) {
                received.put(resultCode, resultData);
            }
        });
        svc.onStart(intent, 0);
        expect(received.size()).toBe(1);
        expect(received.containsKey(ApiSyncService.STATUS_SYNC_FINISHED)).toBeTrue();
    }

    @Test
    public void shouldHandleComplexQueueSituation() throws Exception {
        ApiSyncService svc = new ApiSyncService();
        Context context = DefaultTestRunner.application;

        Intent intent = new Intent(Intent.ACTION_SYNC);
        ArrayList<Uri> urisToSync = new ArrayList<Uri>();
        urisToSync.add(Content.ME_TRACKS.uri);
        urisToSync.add(Content.ME_LIKES.uri);
        urisToSync.add(Content.ME_FOLLOWERS.uri);

        intent.putParcelableArrayListExtra(ApiSyncService.EXTRA_SYNC_URIS, urisToSync);
        SyncIntent request1 = new SyncIntent(context, intent);
        SyncIntent request2 = new SyncIntent(context, new Intent(Intent.ACTION_SYNC, Content.ME_LIKES.uri).putExtra(ApiSyncService.EXTRA_IS_UI_REQUEST,true));
        SyncIntent request3 = new SyncIntent(context, new Intent(Intent.ACTION_SYNC, Content.ME_FOLLOWINGS.uri));

        svc.enqueueRequest(request1);
        expect(svc.mPendingRequests.size()).toBe(3);

        svc.enqueueRequest(request2);
        expect(svc.mPendingRequests.size()).toBe(3);

        svc.enqueueRequest(request3);
        expect(svc.mPendingRequests.size()).toBe(4);

        // make sure favorites is queued on front
        expect(svc.mPendingRequests.peek().contentUri).toBe(Content.ME_LIKES.uri);
        expect(svc.mPendingRequests.get(1).contentUri).toBe(Content.ME_TRACKS.uri);

        SyncIntent request4 = new SyncIntent(context, new Intent(Intent.ACTION_SYNC, Content.ME_FOLLOWINGS.uri).putExtra(ApiSyncService.EXTRA_IS_UI_REQUEST,true));
        svc.enqueueRequest(request4);
        expect(svc.mPendingRequests.peek().contentUri).toBe(Content.ME_FOLLOWINGS.uri);

        // make sure all requests can be executed
        Robolectric.setDefaultHttpResponse(404, "");
        svc.flushSyncRequests();
    }

    @Test
    public void shouldRemoveSyncRequestAfterCompletion() throws Exception {
        ApiSyncService svc = new ApiSyncService();
        Context context = DefaultTestRunner.application;
        svc.mRunningRequests.add(new CollectionSyncRequest(context, Content.ME_LIKES.uri, null, false));
        svc.mRunningRequests.add(new CollectionSyncRequest(context, Content.ME_FOLLOWINGS.uri, null, false));

        ApiSyncer.Result result = new ApiSyncer.Result(Content.ME_LIKES.uri);
        result.success = true;

        svc.onUriSyncResult(new CollectionSyncRequest(context, Content.ME_LIKES.uri, null, false));
        expect(svc.mRunningRequests.size()).toBe(1);
    }

    @Test
    public void shouldUpdateLocalCollectionEntryAfterSync() throws Exception {
        ApiSyncService svc = new ApiSyncService();
        sync(svc, Content.ME_SOUND_STREAM,
                "e1_stream.json",
                "e1_stream_oldest.json");

        expect(Content.COLLECTIONS).toHaveCount(1);
        LocalCollection collection = LocalCollectionDAO.fromContent(Content.ME_SOUND_STREAM, resolver, false);
        expect(collection).not.toBeNull();
        expect(collection.last_sync_success).toBeGreaterThan(0L);
        expect(collection.extra).toEqual("https://api.soundcloud.com/e1/me/stream?uuid%5Bto%5D=ee57b180-0959-11e2-8afd-9083bddf9fde");
    }

    @Test
    public void shouldSyncActivitiesAndUpdateFutureHrefOnLocalCollection() throws Exception {
        ApiSyncService svc = new ApiSyncService();

        sync(svc, Content.ME_SOUND_STREAM,
                "e1_stream_1.json",
                "e1_stream_2_oldest.json");

        expect(Content.COLLECTIONS).toHaveCount(1);
        LocalCollection collection = LocalCollectionDAO.fromContent(Content.ME_SOUND_STREAM, resolver, false);

        expect(collection).not.toBeNull();
        expect(collection.last_sync_success).toBeGreaterThan(0L);
        expect(collection.extra).toEqual("https://api.soundcloud.com/e1/me/stream?uuid%5Bto%5D=ee57b180-0959-11e2-8afd-9083bddf9fde");

        addCannedResponse(SyncAdapterServiceTest.class,
                "https://api.soundcloud.com/e1/me/stream?uuid%5Bto%5D=ee57b180-0959-11e2-8afd-9083bddf9fde&limit=100"+ CollectionSyncRequestTest.NON_INTERACTIVE,
                "activities_empty.json");

        // next sync request should go this url
        sync(svc, Content.ME_SOUND_STREAM);

        expect(Content.COLLECTIONS).toHaveCount(1);
        collection = LocalCollectionDAO.fromContent(Content.ME_SOUND_STREAM, resolver, false);
        expect(collection.extra).toEqual("https://api.soundcloud.com/me/activities/tracks?uuid[to]=future-href-incoming-1");
    }

    @Test
    public void shouldSyncThenAppend() throws Exception {
        ApiSyncService svc = new ApiSyncService();
        sync(svc, Content.ME_ACTIVITIES,
                "e1_activities_1.json",
                "e1_activities_2.json");

        expect(Content.ME_ACTIVITIES).toHaveCount(17);
        expect(Content.COMMENTS).toHaveCount(5);

        append(svc, Content.ME_ACTIVITIES, "own_append.json");

        expect(Content.ME_ACTIVITIES).toHaveCount(18);
        expect(Content.COMMENTS).toHaveCount(6);
    }

    @Test
    public void shouldStopSyncingIfAppendReturnsSameResult() throws Exception {
        ApiSyncService svc = new ApiSyncService();
        sync(svc, Content.ME_ACTIVITIES,
                "e1_activities_1.json",
                "e1_activities_2.json");

        expect(Content.ME_ACTIVITIES).toHaveCount(17);

        for (int i=0; i<3; i++)
            append(svc, Content.ME_ACTIVITIES, "own_append.json");

        expect(Content.ME_ACTIVITIES).toHaveCount(18);
        expect(Content.COMMENTS).toHaveCount(6);
    }

    @Test
    public void shouldClearSyncStatuses() throws Exception {
        ApiSyncService svc = new ApiSyncService();
        ContentResolver resolver = DefaultTestRunner.application.getContentResolver();
        svc.onDestroy();
        expect(LocalCollectionDAO.fromContentUri(Content.ME_TRACKS.uri, resolver, true).sync_state).toBe(LocalCollection.SyncState.IDLE);
    }

    private void addResourceResponse(String url, String resource) throws IOException {
        TestHelper.addCannedResponse(getClass(), url, resource);
    }

    private void sync(ApiSyncService svc, Content content, String... fixtures) throws IOException {
        serviceAction(svc, Intent.ACTION_SYNC, content, fixtures);
    }

    private  void append(ApiSyncService svc, Content content, String... fixtures) throws IOException {
        serviceAction(svc, ApiSyncService.ACTION_APPEND, content, fixtures);
    }

    private void serviceAction(ApiSyncService svc, String action, Content content, String... fixtures) throws IOException {
        TestHelper.addPendingHttpResponse(SyncAdapterServiceTest.class, fixtures);
        svc.onStart(new Intent(action, content.uri), 1);
    }
}
