package com.soundcloud.android.service.upload;

import static com.soundcloud.android.Expect.expect;

import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.model.Track;
import com.soundcloud.android.model.User;
import com.soundcloud.android.provider.Content;
import com.soundcloud.android.provider.SoundCloudDB;
import com.soundcloud.android.robolectric.DefaultTestRunner;
import com.soundcloud.android.robolectric.TestHelper;
import com.xtremelabs.robolectric.Robolectric;
import com.xtremelabs.robolectric.tester.org.apache.http.TestHttpResponse;
import com.xtremelabs.robolectric.util.Scheduler;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import android.content.ContentResolver;
import android.net.Uri;
import android.os.Looper;


@RunWith(DefaultTestRunner.class)
public class PollUploadedTrackTest {
    static final long USER_ID = 3135930L;
    static final long TRACK_ID = 12345L;
    ContentResolver resolver;
    Scheduler scheduler;

    @Before
    public void before() {
        DefaultTestRunner.application.setCurrentUserId(USER_ID);
        resolver = DefaultTestRunner.application.getContentResolver();
        scheduler = Robolectric.getUiThreadScheduler();
    }

    @After
    public void after() {
        expect(scheduler.size()).toEqual(0);
    }

    @Test
    public void testPollUploadedSuccess() throws Exception {
        TestHelper.addCannedResponses(getClass(), "track_finished.json");
        addProcessingTrackAndRunPoll(TRACK_ID);
        expectLocalTracksStreamable(TRACK_ID);
    }

    @Test
    public void testPollUploadedProcessSuccess() throws Exception {
        TestHelper.addCannedResponses(getClass(), "track_storing.json", "track_finished.json");
        addProcessingTrackAndRunPoll(TRACK_ID);
        expectLocalTracksStreamable(TRACK_ID);
    }

    @Test
    public void testPollUploaded400Success() throws Exception {
        Robolectric.addPendingHttpResponse(400, "failed");
        TestHelper.addCannedResponses(getClass(), "track_finished.json");
        addProcessingTrackAndRunPoll(TRACK_ID);
        expectLocalTracksStreamable(TRACK_ID);
    }

    @Test
    public void testPollUploadedFailure() throws Exception {
        TestHelper.addCannedResponses(getClass(), "track_failed.json");
        addProcessingTrackAndRunPoll(TRACK_ID);
        expectLocalTracksNotStreamable(TRACK_ID);
    }

    @Test
    public void testPollUploadedProcess400Failure() throws Exception {
        Robolectric.addHttpResponseRule("GET", "/tracks/12345", new TestHttpResponse(400, "failed"));
        addProcessingTrackAndRunPoll(TRACK_ID);
        expectLocalTracksNotStreamable(TRACK_ID);
    }

    @Test
    public void testPollUploadedProcessTimeoutFailure() throws Exception {
        TestHelper.addCannedResponses(getClass(), "track_storing.json", "track_storing.json", "track_finished.json");
        addProcessingTrackAndRunPoll(TRACK_ID, 1l);
        expectLocalTracksNotStreamable(TRACK_ID);
    }

    private void addProcessingTrackAndRunPoll(long id) {
        addProcessingTrackAndRunPoll(id, 5000l);
    }

    private void addProcessingTrackAndRunPoll(long id, long maxTime) {
        Track t = new Track();
        t.user = new User();
        t.user.id = USER_ID;
        t.id = id;
        t.state = Track.State.STORING;
        Uri newUri = t.commitLocally(resolver, SoundCloudApplication.TRACK_CACHE);
        expect(newUri).not.toBeNull();

        new Poller(Looper.myLooper(), DefaultTestRunner.application, id, Content.ME_TRACKS.uri, 1, maxTime).start();

        // make sure all messages have been consumed
        do {
            scheduler.advanceToLastPostedRunnable();
        } while (scheduler.size() > 0);
    }

    private void expectLocalTracksStreamable(long id) {
        Track track = SoundCloudApplication.TRACK_CACHE.get(id);
        expect(track).not.toBeNull();
        expect(track.state.isStreamable()).toBeTrue();

        track = SoundCloudDB.getTrackById(resolver, id);
        expect(track).not.toBeNull();
        expect(track.state.isStreamable()).toBeTrue();
    }

    private void expectLocalTracksNotStreamable(long id) {
        Track track = SoundCloudApplication.TRACK_CACHE.get(id);
        expect(track).not.toBeNull();
        expect(track.state.isStreamable()).toBeFalse();

        track = SoundCloudDB.getTrackById(resolver, id);
        expect(track).not.toBeNull();
        expect(track.state.isStreamable()).toBeFalse();
    }
}
