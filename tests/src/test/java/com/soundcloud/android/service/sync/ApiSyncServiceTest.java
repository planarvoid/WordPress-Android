package com.soundcloud.android.service.sync;


import static com.soundcloud.android.Expect.expect;
import static com.soundcloud.android.robolectric.TestHelper.*;

import com.soundcloud.android.Consts;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.model.act.Activities;
import com.soundcloud.android.model.act.Activity;
import com.soundcloud.android.model.act.TrackActivity;
import com.soundcloud.android.model.act.TrackSharingActivity;
import com.soundcloud.android.model.LocalCollection;
import com.soundcloud.android.model.Track;
import com.soundcloud.android.model.User;
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
    public void shouldComplexQueue() throws Exception {
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
    public void shouldRemove() throws Exception {
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
    public void shouldSyncTracks() throws Exception {
        ApiSyncService svc = new ApiSyncService();

        addIdResponse("/me/tracks/ids?linked_partitioning=1"+ CollectionSyncRequestTest.NON_INTERACTIVE, 1, 2, 3);
        addResourceResponse("/tracks?linked_partitioning=1&limit=200&ids=1%2C2%2C3"+ CollectionSyncRequestTest.NON_INTERACTIVE, "tracks.json");

        svc.onStart(new Intent(Intent.ACTION_SYNC, Content.ME_TRACKS.uri), 1);
        // make sure tracks+users got written
        expect(Content.TRACKS).toHaveCount(3);
        expect(Content.COLLECTION_ITEMS).toHaveCount(3);
        expect(Content.USERS).toHaveCount(1);
        assertResolverNotified(Content.ME_TRACKS.uri, Content.TRACKS.uri, Content.USERS.uri);
    }

    @Test
    public void shouldSyncMe() throws Exception {
        addCannedResponses(getClass(), "me.json");
        ApiSyncService svc = new ApiSyncService();

        expect(Content.ME).toBeEmpty();
        svc.onStart(new Intent(Intent.ACTION_SYNC, Content.ME.uri), 1);
        expect(Content.ME).toHaveCount(1);
        expect(Content.USERS).toHaveCount(1);
    }
    @Test
    public void shouldSyncFollowers() throws Exception {
        ApiSyncService svc = new ApiSyncService();

        addIdResponse("/me/followers/ids?linked_partitioning=1"+ CollectionSyncRequestTest.NON_INTERACTIVE, 792584, 1255758, 308291);
        addResourceResponse("/me/followers?linked_partitioning=1&limit=" + Consts.COLLECTION_PAGE_SIZE+ CollectionSyncRequestTest.NON_INTERACTIVE,
                "users.json");

        svc.onStart(new Intent(Intent.ACTION_SYNC, Content.ME_FOLLOWERS.uri), 1);
        // make sure tracks+users got written
        expect(Content.USERS).toHaveCount(3);
        expect(Content.ME_FOLLOWERS).toHaveCount(3);
        assertFirstIdToBe(Content.ME_FOLLOWERS, 792584);
    }

    @Test
    public void shouldSyncActivitiesIncoming() throws Exception {
        ApiSyncService svc = new ApiSyncService();
        sync(svc, Content.ME_SOUND_STREAM,
                "e1_stream.json",
                "e1_stream_oldest.json");

        expect(Content.COLLECTIONS).toHaveCount(1);
        LocalCollection collection = LocalCollection.fromContent(Content.ME_SOUND_STREAM, resolver, false);

        expect(collection).not.toBeNull();
        expect(collection.last_sync_success).toBeGreaterThan(0L);
        expect(collection.extra).toEqual("https://api.soundcloud.com/e1/me/stream?uuid%5Bto%5D=ee57b180-0959-11e2-8afd-9083bddf9fde");

        expect(Content.ME_SOUND_STREAM).toHaveCount(112);
        expect(Content.ME_EXCLUSIVE_STREAM).toHaveCount(0);
        expect(Content.TRACKS).toHaveCount(111);
        expect(Content.USERS).toHaveCount(27);
        //expect(Content.PLAYLISTS).toHaveCount(8);

        Activities incoming = Activities.getSince(Content.ME_SOUND_STREAM, resolver, -1);

        expect(incoming.size()).toEqual(112);
        expect(incoming.getUniqueTracks().size()).toEqual(111); // currently excluding playlists
        assertResolverNotified(Content.ME_SOUND_STREAM.uri, Content.TRACKS.uri, Content.USERS.uri);
    }

    @Test
    public void shouldNotOverwriteDataFromPreviousSyncRuns() throws Exception {
        ApiSyncService svc = new ApiSyncService();
        addIdResponse("/me/tracks/ids?linked_partitioning=1"+ CollectionSyncRequestTest.NON_INTERACTIVE, 1, 2, 3);
        addResourceResponse("/tracks?linked_partitioning=1&limit=200&ids=1%2C2%2C3"+ CollectionSyncRequestTest.NON_INTERACTIVE, "tracks.json");

        svc.onStart(new Intent(Intent.ACTION_SYNC, Content.ME_TRACKS.uri), 1);
        expect(Content.TRACKS).toHaveCount(3);

        // sync activities concerning these tracks
        sync(svc, Content.ME_ACTIVITIES, "tracks_activities.json");

        Track t = SoundCloudApplication.MODEL_MANAGER.getTrack(10853436L);
        expect(t).not.toBeNull();
        expect(t.duration).toEqual(782);
        // title should get changed from the activity json
        expect(t.title).toEqual("recording on sunday night (edit)");

        User u = SoundCloudApplication.MODEL_MANAGER.getUser(3135930L);
        expect(u).not.toBeNull();
        expect(u.username).toEqual("I'm your father");
        // permalink was set in first sync run, not present in second
        expect(u.permalink).toEqual("soundcloud-android-mwc");
    }

    @Test
    public void shouldNotOverwriteDataFromPreviousSyncRuns2() throws Exception {
        ApiSyncService svc = new ApiSyncService();
        sync(svc, Content.ME_ACTIVITIES, "tracks_activities.json");

        addIdResponse("/me/tracks/ids?linked_partitioning=1"+ CollectionSyncRequestTest.NON_INTERACTIVE, 1, 2, 3);
        addResourceResponse("/tracks?linked_partitioning=1&limit=200&ids=1%2C2%2C3"+ CollectionSyncRequestTest.NON_INTERACTIVE, "tracks.json");

        svc.onStart(new Intent(Intent.ACTION_SYNC, Content.ME_TRACKS.uri), 1);

        expect(Content.TRACKS).toHaveCount(3);

        Track t = SoundCloudApplication.MODEL_MANAGER.getTrack(10853436L);
        expect(t).not.toBeNull();
        expect(t.duration).toEqual(782);
    }

    @Test
    public void shouldSyncActivitiesOwn() throws Exception {
        ApiSyncService svc = new ApiSyncService();

        sync(svc, Content.ME_ACTIVITIES,
                "e1_activities_1.json",
                "e1_activities_2.json");

        expect(Content.ME_ACTIVITIES).toHaveCount(9);
        expect(Content.COMMENTS).toHaveCount(5);

        Activities own = Activities.getSince(Content.ME_ACTIVITIES, resolver, -1);
        expect(own.size()).toEqual(9);
        assertResolverNotified(Content.TRACKS.uri,
                Content.USERS.uri,
                Content.COMMENTS.uri,
                Content.ME_ACTIVITIES.uri);
    }

    @Test
    public void shouldSyncThenAppend() throws Exception {
        ApiSyncService svc = new ApiSyncService();
        sync(svc, Content.ME_ACTIVITIES,
                "e1_activities_1.json",
                "e1_activities_2.json");

        expect(Content.ME_ACTIVITIES).toHaveCount(9);
        expect(Content.COMMENTS).toHaveCount(5);

        append(svc, Content.ME_ACTIVITIES,
                "own_append.json");

        expect(Content.ME_ACTIVITIES).toHaveCount(10);
        expect(Content.COMMENTS).toHaveCount(6);
    }

    @Test
    public void shouldStopSyncingIfAppendReturnsSameResult() throws Exception {
        ApiSyncService svc = new ApiSyncService();
        sync(svc, Content.ME_ACTIVITIES,
                "e1_activities_1.json",
                                "e1_activities_2.json");

        expect(Content.ME_ACTIVITIES).toHaveCount(9);

        for (int i=0; i<3; i++)
            append(svc, Content.ME_ACTIVITIES,
                    "own_append.json");

        expect(Content.ME_ACTIVITIES).toHaveCount(10);
        expect(Content.COMMENTS).toHaveCount(6);
    }


    @Test
    public void shouldSyncDifferentEndoints() throws Exception {
        ApiSyncService svc = new ApiSyncService();
        sync(svc, Content.ME_ACTIVITIES,
                "e1_activities_1.json",
                "e1_activities_2.json");

        sync(svc, Content.ME_SOUND_STREAM,
                "e1_stream.json",
                "e1_stream_oldest.json");

        expect(Content.ME_SOUND_STREAM).toHaveCount(112);
        expect(Content.ME_ACTIVITIES).toHaveCount(9);
        expect(Content.ME_EXCLUSIVE_STREAM).toHaveCount(0);
        expect(Content.ME_ALL_ACTIVITIES).toHaveCount(137);
    }

    @Test
    public void shouldNotProduceDuplicatesWhenSyncing() throws Exception {
        ApiSyncService svc = new ApiSyncService();
        sync(svc, Content.ME_SOUND_STREAM,
                "e1_stream_1_oldest.json");

        sync(svc, Content.ME_EXCLUSIVE_STREAM,
                "e1_stream_1_oldest.json");

        expect(Content.ME_SOUND_STREAM).toHaveCount(20);
        expect(Content.ME_EXCLUSIVE_STREAM).toHaveCount(20);
        expect(Content.ME_ALL_ACTIVITIES).toHaveCount(44);
    }

    @Test
    public void shouldSyncActivitiesWithFutureHref() throws Exception {
        ApiSyncService svc = new ApiSyncService();

        sync(svc, Content.ME_SOUND_STREAM,
                "e1_stream_1.json",
                "e1_stream_2_oldest.json");

        expect(Content.COLLECTIONS).toHaveCount(1);
        LocalCollection collection = LocalCollection.fromContent(Content.ME_SOUND_STREAM, resolver, false);

        expect(collection).not.toBeNull();
        expect(collection.last_sync_success).toBeGreaterThan(0L);
        expect(collection.extra).toEqual("https://api.soundcloud.com/e1/me/stream?uuid%5Bto%5D=ee57b180-0959-11e2-8afd-9083bddf9fde");

        addCannedResponse(SyncAdapterServiceTest.class,
                "https://api.soundcloud.com/e1/me/stream?uuid%5Bto%5D=ee57b180-0959-11e2-8afd-9083bddf9fde&limit=100"+ CollectionSyncRequestTest.NON_INTERACTIVE,
                "activities_empty.json");

        // next sync request should go this url
        sync(svc, Content.ME_SOUND_STREAM);

        expect(Content.COLLECTIONS).toHaveCount(1);
        collection = LocalCollection.fromContent(Content.ME_SOUND_STREAM, resolver, false);
        expect(collection.extra).toEqual("https://api.soundcloud.com/me/activities/tracks?uuid[to]=future-href-incoming-1");
    }

    @Test
    public void shouldFilterOutDuplicateTrackAndSharingsAndKeepSharings() throws Exception {

        // TODO, removed duplicate handling. Figure out how to handle with reposts now

        ApiSyncService svc = new ApiSyncService();
        //  1 unrelated track + 2 track-sharing/track with same id
        sync(svc, Content.ME_SOUND_STREAM,
                "track_and_track_sharing.json");

        expect(Content.ME_SOUND_STREAM).toHaveCount(3);
        Activities incoming = Activities.getSince(Content.ME_SOUND_STREAM, resolver, -1);

        expect(incoming.size()).toEqual(3); // this is with a duplicate
        Activity a1 = incoming.get(0);
        Activity a2 = incoming.get(1);

        expect(a1 instanceof TrackActivity).toBeTrue();
        expect(a1.getTrack().permalink).toEqual("bastard-amo1-edit");
        expect(a2 instanceof TrackSharingActivity).toBeTrue();
        expect(a2.getTrack().permalink).toEqual("leotrax06-leo-zero-boom-bam");
    }

    @Test
    public void shouldClearSyncStatuses() throws Exception {
        ApiSyncService svc = new ApiSyncService();
        ContentResolver resolver = DefaultTestRunner.application.getContentResolver();
        svc.onDestroy();
        expect(LocalCollection.fromContentUri(Content.ME_TRACKS.uri, resolver, true).sync_state).toBe(LocalCollection.SyncState.IDLE);
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
        TestHelper.addCannedResponses(SyncAdapterServiceTest.class, fixtures);
        svc.onStart(new Intent(action, content.uri), 1);
    }
}
