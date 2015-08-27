package com.soundcloud.android.sync;


import static com.soundcloud.android.Expect.expect;
import static com.soundcloud.android.testsupport.InjectionSupport.lazyOf;

import com.soundcloud.android.api.legacy.model.LocalCollection;
import com.soundcloud.android.api.model.ApiPlaylist;
import com.soundcloud.android.api.model.ApiTrack;
import com.soundcloud.android.robolectric.DefaultTestRunner;
import com.soundcloud.android.stations.StationsSyncRequestFactory;
import com.soundcloud.android.storage.LocalCollectionDAO;
import com.soundcloud.android.storage.provider.Content;
import com.soundcloud.android.sync.entities.EntitySyncRequestFactory;
import com.soundcloud.android.sync.likes.LikesSyncer;
import com.soundcloud.android.sync.likes.SyncPlaylistLikesJob;
import com.soundcloud.android.sync.likes.SyncTrackLikesJob;
import com.soundcloud.android.sync.playlists.SinglePlaylistSyncerFactory;
import com.soundcloud.android.sync.recommendations.RecommendationsSyncer;
import com.soundcloud.android.testsupport.InjectionSupport;
import com.soundcloud.android.testsupport.TestHelper;
import com.soundcloud.rx.eventbus.TestEventBus;
import com.xtremelabs.robolectric.Robolectric;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;

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
    private static final long USER_ID = 100L;

    private ContentResolver resolver;
    private SyncStateManager syncStateManager;
    private LegacySyncJob.Factory collectionSyncRequestFactory;
    private SyncRequestFactory syncRequestFactory;

    @Mock private ApiSyncerFactory apiSyncerFactory;
    @Mock private LikesSyncer<ApiTrack> trackLikesSyncer;
    @Mock private LikesSyncer<ApiPlaylist> playlistLikesSyncer;
    @Mock private EntitySyncRequestFactory entitySyncRequestFactory;
    @Mock private SinglePlaylistSyncerFactory singlePlaylistSyncerFactory;
    @Mock private RecommendationsSyncer recommendationsSyncer;
    @Mock private StationsSyncRequestFactory stationsSyncRequestFactory;

    @Before
    public void before() {
        resolver = Robolectric.application.getContentResolver();
        syncStateManager = new SyncStateManager(resolver, new LocalCollectionDAO(resolver));
        collectionSyncRequestFactory = new LegacySyncJob.Factory(Robolectric.application, apiSyncerFactory, syncStateManager);
        TestHelper.setUserId(USER_ID);

        syncRequestFactory = new SyncRequestFactory(
                new LegacySyncRequest.Factory(collectionSyncRequestFactory),
                lazyOf(new SyncTrackLikesJob(InjectionSupport.lazyOf(trackLikesSyncer))),
                lazyOf(new SyncPlaylistLikesJob(InjectionSupport.lazyOf(playlistLikesSyncer))),
                entitySyncRequestFactory,
                singlePlaylistSyncerFactory,
                lazyOf(recommendationsSyncer),
                stationsSyncRequestFactory,
                new TestEventBus());
    }

    @After
    public void after() {
        expect(Robolectric.getFakeHttpLayer().hasPendingResponses()).toBeFalse();
    }

    @Test
    public void shouldProvideFeedbackViaResultReceiver() throws Exception {
        ApiSyncService svc = new ApiSyncService();
        Intent intent = new Intent(Intent.ACTION_SYNC, Content.ME_ACTIVITIES.uri);

        final LinkedHashMap<Integer, Bundle> received = new LinkedHashMap<>();
        intent.putExtra(ApiSyncService.EXTRA_STATUS_RECEIVER, new ResultReceiver(new Handler(Looper.myLooper())) {
            @Override
            protected void onReceiveResult(int resultCode, Bundle resultData) {
                received.put(resultCode, resultData);
            }
        });

        TestHelper.addPendingHttpResponse(getClass(), "e1_activities_1_oldest.json");
        svc.onStart(intent, 0);

        expect(received.size()).toBe(1);
        expect(received.containsKey(ApiSyncService.STATUS_SYNC_ERROR)).toBeFalse();
    }

    @Test
    public void shouldProvideFeedbackViaResultReceiverNoSyncIntent() throws Exception {
        ApiSyncService svc = new ApiSyncService();
        Intent intent = new Intent(Intent.ACTION_SYNC, null);

        final LinkedHashMap<Integer, Bundle> received = new LinkedHashMap<>();
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
    public void shouldNotEnqueueSameIntentTwice() throws Exception {
        final Intent syncIntent = new Intent(ApiSyncService.ACTION_PUSH, null);
        syncIntent.setData(Content.ME_FOLLOWINGS.uri);

        ApiSyncService svc = new ApiSyncService();
        svc.enqueueRequest(syncRequestFactory.create(syncIntent));
        svc.enqueueRequest(syncRequestFactory.create(syncIntent));
        expect(svc.pendingJobs.size()).toBe(1);
    }

    @Test
    public void doesNotQueueTrackLikesJobOnTopOfMyLikesJob() throws Exception {
        ApiSyncService svc = new ApiSyncService();
        svc.enqueueRequest(syncRequestFactory.create(new Intent(Intent.ACTION_SYNC, Content.ME_LIKES.uri)));
        svc.enqueueRequest(syncRequestFactory.create(new Intent(SyncActions.SYNC_TRACK_LIKES, null)));
        expect(svc.pendingJobs.size()).toBe(1);
    }

    @Test
    public void queuesMyLikesJobEvenIfTrackLikesJobAlreadyInQueue() throws Exception {
        ApiSyncService svc = new ApiSyncService();
        svc.enqueueRequest(syncRequestFactory.create(new Intent(SyncActions.SYNC_TRACK_LIKES, null)));
        expect(svc.pendingJobs.size()).toBe(1);
        svc.enqueueRequest(syncRequestFactory.create(new Intent(Intent.ACTION_SYNC, Content.ME_LIKES.uri)));
        expect(svc.pendingJobs.size()).toBe(2);

    }

    @Test
    public void shouldHandleComplexQueueSituation() throws Exception {
        ApiSyncService svc = new ApiSyncService();

        Intent intent = new Intent(Intent.ACTION_SYNC, null);
        ArrayList<Uri> urisToSync = new ArrayList<>();
        urisToSync.add(Content.ME_SOUNDS.uri);
        urisToSync.add(Content.ME_LIKES.uri);
        urisToSync.add(Content.ME_FOLLOWERS.uri);

        intent.putParcelableArrayListExtra(ApiSyncService.EXTRA_SYNC_URIS, urisToSync);

        SyncRequestFactory syncRequestFactory = new SyncRequestFactory(
                new LegacySyncRequest.Factory(collectionSyncRequestFactory),
                null, null, entitySyncRequestFactory, singlePlaylistSyncerFactory,
                lazyOf(recommendationsSyncer), stationsSyncRequestFactory, new TestEventBus()
        );
        SyncRequest request1 = syncRequestFactory.create(intent);
        SyncRequest request2 = syncRequestFactory.create(new Intent(Intent.ACTION_SYNC, Content.ME_LIKES.uri).putExtra(ApiSyncService.EXTRA_IS_UI_REQUEST, true));
        SyncRequest request3 = syncRequestFactory.create(new Intent(Intent.ACTION_SYNC, Content.ME_FOLLOWINGS.uri));

        svc.enqueueRequest(request1);
        expect(svc.pendingJobs.size()).toBe(3);

        svc.enqueueRequest(request2);
        expect(svc.pendingJobs.size()).toBe(3);

        svc.enqueueRequest(request3);
        expect(svc.pendingJobs.size()).toBe(4);

        // make sure favorites is queued on front
        expect(((LegacySyncJob) svc.pendingJobs.peek()).getContentUri()).toBe(Content.ME_LIKES.uri);

        SyncRequest request4 = syncRequestFactory.create(new Intent(Intent.ACTION_SYNC, Content.ME_FOLLOWINGS.uri).putExtra(ApiSyncService.EXTRA_IS_UI_REQUEST, true));
        svc.enqueueRequest(request4);
        expect(((LegacySyncJob) svc.pendingJobs.peek()).getContentUri()).toBe(Content.ME_FOLLOWINGS.uri);

        // make sure all requests can be executed
        Robolectric.setDefaultHttpResponse(404, "");
        svc.flushSyncRequests();
    }

    @Test
    public void shouldRemoveSyncRequestAfterCompletion() throws Exception {
        ApiSyncService svc = new ApiSyncService();
        Context context = DefaultTestRunner.application;
        svc.runningJobs.add(new LegacySyncJob(context, Content.ME_LIKES.uri, null, false, apiSyncerFactory, syncStateManager));
        svc.runningJobs.add(new LegacySyncJob(context, Content.ME_FOLLOWINGS.uri, null, false, apiSyncerFactory, syncStateManager));

        ApiSyncResult result = new ApiSyncResult(Content.ME_LIKES.uri);
        result.success = true;

        svc.onSyncJobCompleted(new LegacySyncJob(context, Content.ME_LIKES.uri, null, false, apiSyncerFactory, syncStateManager));
        expect(svc.runningJobs.size()).toBe(1);
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

        for (int i = 0; i < 3; i++)
            append(svc, Content.ME_ACTIVITIES, "own_append.json");

        expect(Content.ME_ACTIVITIES).toHaveCount(18);
        expect(Content.COMMENTS).toHaveCount(6);
    }

    @Test
    public void shouldClearSyncStatuses() throws Exception {
        ApiSyncService svc = new ApiSyncService();
        svc.onDestroy();
        expect(syncStateManager.fromContent(Content.ME_SOUNDS).sync_state).toEqual(LocalCollection.SyncState.IDLE);
    }

    private void sync(ApiSyncService svc, Content content, String... fixtures) throws IOException {
        serviceAction(svc, Intent.ACTION_SYNC, content, fixtures);
    }

    private void append(ApiSyncService svc, Content content, String... fixtures) throws IOException {
        serviceAction(svc, ApiSyncService.ACTION_APPEND, content, fixtures);
    }

    private void serviceAction(ApiSyncService svc, String action, Content content, String... fixtures) throws IOException {
        TestHelper.addPendingHttpResponse(SyncAdapterServiceTest.class, fixtures);
        svc.onStart(new Intent(action, content.uri), 1);
    }

}
