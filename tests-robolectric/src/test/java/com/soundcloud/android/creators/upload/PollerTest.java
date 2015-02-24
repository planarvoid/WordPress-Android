package com.soundcloud.android.creators.upload;

import static com.soundcloud.android.Expect.expect;
import static org.mockito.Mockito.verify;

import com.soundcloud.android.api.legacy.PublicApi;
import com.soundcloud.android.api.legacy.model.PublicApiTrack;
import com.soundcloud.android.api.legacy.model.PublicApiUser;
import com.soundcloud.android.storage.provider.Content;
import com.soundcloud.android.robolectric.DefaultTestRunner;
import com.soundcloud.android.testsupport.TestHelper;
import com.xtremelabs.robolectric.Robolectric;
import com.xtremelabs.robolectric.tester.org.apache.http.TestHttpResponse;
import com.xtremelabs.robolectric.util.Scheduler;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;

import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.os.HandlerThread;
import android.support.v4.content.LocalBroadcastManager;


@RunWith(DefaultTestRunner.class)
public class PollerTest {
    static final long USER_ID  = 3135930L;
    static final long TRACK_ID = 12345L;
    ContentResolver resolver;

    @Mock private BroadcastReceiver receiver;

    @Before
    public void before() {
        TestHelper.setUserId(USER_ID);
        resolver = DefaultTestRunner.application.getContentResolver();
        LocalBroadcastManager lbm = LocalBroadcastManager.getInstance(Robolectric.application);
        lbm.registerReceiver(receiver, UploadService.getIntentFilter());
    }

    @Test
    public void testPollUploadedSuccess() throws Exception {
        TestHelper.addPendingHttpResponse(getClass(), "track_finished.json");
        addProcessingTrackAndRunPoll(TRACK_ID);
        expectLocalTracksStreamable(TRACK_ID);
    }

    @Test
    public void testPollUploadedProcessSuccess() throws Exception {
        TestHelper.addPendingHttpResponse(getClass(), "track_processing.json", "track_finished.json");
        addProcessingTrackAndRunPoll(TRACK_ID);
        expectLocalTracksStreamable(TRACK_ID);

        // assert transcoding success broadcast got sent out
        ArgumentCaptor<Intent> argument = ArgumentCaptor.forClass(Intent.class);
        verify(receiver).onReceive(Mockito.any(Context.class), argument.capture());
        expect(argument.getValue().getAction()).toEqual(UploadService.TRANSCODING_SUCCESS);

    }

    @Test
    public void testPollUploaded400Success() throws Exception {
        Robolectric.addPendingHttpResponse(400, "failed");
        TestHelper.addPendingHttpResponse(getClass(), "track_finished.json");
        addProcessingTrackAndRunPoll(TRACK_ID);
        expectLocalTracksStreamable(TRACK_ID);
    }

    @Test
    public void testPollUploadedFailure() throws Exception {
        TestHelper.addPendingHttpResponse(getClass(), "track_failed.json");
        addProcessingTrackAndRunPoll(TRACK_ID);
        expectLocalTracksNotStreamable(TRACK_ID);

        // assert transcoding failed broadcast got sent out
        ArgumentCaptor<Intent> argument = ArgumentCaptor.forClass(Intent.class);
        verify(receiver).onReceive(Mockito.any(Context.class), argument.capture());
        expect(argument.getValue().getAction()).toEqual(UploadService.TRANSCODING_FAILED);
    }

    @Test
    public void testPollUploadedProcess400Failure() throws Exception {
        Robolectric.addHttpResponseRule(
            TestHelper.createRegexRequestMatcherForUriWithClientId("GET", "/tracks/12345"),
            new TestHttpResponse(400, "failed"));
        addProcessingTrackAndRunPoll(TRACK_ID);
        expectLocalTracksNotStreamable(TRACK_ID);
    }

    @Test
    public void testPollUploadedProcessTimeoutFailure() throws Exception {
        TestHelper.addCannedResponse(getClass(), "/tracks/12345", "track_processing.json");
        addProcessingTrackAndRunPoll(TRACK_ID);
        expectLocalTracksNotStreamable(TRACK_ID);
    }

    private void addProcessingTrackAndRunPoll(long id) {
        PublicApiTrack t = new PublicApiTrack();
        t.user = new PublicApiUser();
        t.user.setId(USER_ID);
        t.setId(id);
        t.state = PublicApiTrack.State.PROCESSING;
        t.setUpdated();
        TestHelper.insertWithDependencies(t);

        HandlerThread ht = new HandlerThread("poll");
        ht.start();

        Scheduler scheduler = Robolectric.shadowOf(ht.getLooper()).getScheduler();
        new Poller(ht.getLooper(), new PublicApi(DefaultTestRunner.application), id, Content.SOUNDS.uri, 1).start();

        // make sure all messages have been consumed
        do {
            scheduler.advanceToLastPostedRunnable();
        } while (scheduler.size() > 0);
    }

    private void expectLocalTracksStreamable(long id) {
        PublicApiTrack track = getTrack(id);
        expect(track).not.toBeNull();
        expect(track.state.isStreamable()).toBeTrue();
    }

    private void expectLocalTracksNotStreamable(long id) {
        PublicApiTrack track = getTrack(id);
        expect(track).not.toBeNull();
        expect(track.state.isStreamable()).toBeFalse();
    }

    private PublicApiTrack getTrack(long id) {
        try {
            return TestHelper.loadLocalContent(Content.TRACKS.forId(id), PublicApiTrack.class).get(0);
        } catch (Exception e) {
            throw new AssertionError(e);
        }
    }
}
