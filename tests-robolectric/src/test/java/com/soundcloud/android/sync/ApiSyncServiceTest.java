package com.soundcloud.android.sync;


import static com.soundcloud.android.Expect.expect;
import static com.soundcloud.android.testsupport.InjectionSupport.lazyOf;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;

import com.soundcloud.android.api.model.ApiPlaylist;
import com.soundcloud.android.api.model.ApiTrack;
import com.soundcloud.android.robolectric.DefaultTestRunner;
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
import rx.Observable;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.ResultReceiver;

import java.util.ArrayList;
import java.util.LinkedHashMap;

@RunWith(DefaultTestRunner.class)
public class ApiSyncServiceTest {
    private static final long USER_ID = 100L;

    private LegacySyncJob.Factory collectionSyncRequestFactory;
    private SyncRequestFactory syncRequestFactory;

    @Mock private SyncerRegistry syncerRegistry;
    @Mock private SingleJobRequestFactory singleJobRequestFactory;
    @Mock private ApiSyncerFactory apiSyncerFactory;
    @Mock private LikesSyncer<ApiTrack> trackLikesSyncer;
    @Mock private LikesSyncer<ApiPlaylist> playlistLikesSyncer;
    @Mock private EntitySyncRequestFactory entitySyncRequestFactory;
    @Mock private SinglePlaylistSyncerFactory singlePlaylistSyncerFactory;
    @Mock private RecommendationsSyncer recommendationsSyncer;
    @Mock private SyncStateManager syncStateManager;

    @Before
    public void before() {
        collectionSyncRequestFactory = new LegacySyncJob.Factory(apiSyncerFactory, syncStateManager);
        TestHelper.setUserId(USER_ID);

        when(syncStateManager.updateLastSyncAttemptAsync(any(Uri.class))).thenReturn(Observable.just(true));

        syncRequestFactory = new SyncRequestFactory(
                syncerRegistry,
                singleJobRequestFactory,
                new LegacySyncRequest.Factory(collectionSyncRequestFactory),
                lazyOf(new SyncTrackLikesJob(InjectionSupport.lazyOf(trackLikesSyncer))),
                lazyOf(new SyncPlaylistLikesJob(InjectionSupport.lazyOf(playlistLikesSyncer))),
                entitySyncRequestFactory,
                singlePlaylistSyncerFactory,
                lazyOf(recommendationsSyncer),
                new TestEventBus());
    }

    @After
    public void after() {
        expect(Robolectric.getFakeHttpLayer().hasPendingResponses()).toBeFalse();
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
        urisToSync.add(Content.ME_FOLLOWINGS.uri);

        intent.putParcelableArrayListExtra(ApiSyncService.EXTRA_SYNC_URIS, urisToSync);

        SyncRequestFactory syncRequestFactory = new SyncRequestFactory(
                syncerRegistry,
                singleJobRequestFactory,
                new LegacySyncRequest.Factory(collectionSyncRequestFactory),
                null,
                null,
                entitySyncRequestFactory,
                singlePlaylistSyncerFactory,
                lazyOf(recommendationsSyncer),
                new TestEventBus()
        );
        SyncRequest request1 = syncRequestFactory.create(intent);
        SyncRequest request2 = syncRequestFactory.create(new Intent(Intent.ACTION_SYNC, Content.ME_LIKES.uri).putExtra(
                ApiSyncService.EXTRA_IS_UI_REQUEST,
                true));

        svc.enqueueRequest(request1);
        expect(svc.pendingJobs.size()).toBe(3);

        svc.enqueueRequest(request2);
        expect(svc.pendingJobs.size()).toBe(3);

        // make sure favorites is queued on front
        expect(((LegacySyncJob) svc.pendingJobs.peek()).getContentUri()).toBe(Content.ME_LIKES.uri);

        SyncRequest request4 = syncRequestFactory.create(new Intent(Intent.ACTION_SYNC,
                Content.ME_FOLLOWINGS.uri).putExtra(ApiSyncService.EXTRA_IS_UI_REQUEST,
                true));
        svc.enqueueRequest(request4);
        expect(((LegacySyncJob) svc.pendingJobs.peek()).getContentUri()).toBe(Content.ME_FOLLOWINGS.uri);

        // make sure all requests can be executed
        Robolectric.setDefaultHttpResponse(404, "");
        svc.flushSyncRequests();
    }

    @Test
    public void shouldRemoveSyncRequestAfterCompletion() throws Exception {
        ApiSyncService svc = new ApiSyncService();
        svc.runningJobs.add(new LegacySyncJob(Content.ME_LIKES.uri, null, false, apiSyncerFactory, syncStateManager));
        svc.runningJobs.add(new LegacySyncJob(Content.ME_FOLLOWINGS.uri,
                null,
                false,
                apiSyncerFactory,
                syncStateManager));

        LegacySyncResult result = new LegacySyncResult(Content.ME_LIKES.uri);
        result.success = true;

        svc.onSyncJobCompleted(new LegacySyncJob(Content.ME_LIKES.uri,
                null,
                false,
                apiSyncerFactory,
                syncStateManager));
        expect(svc.runningJobs.size()).toBe(1);
    }
}
