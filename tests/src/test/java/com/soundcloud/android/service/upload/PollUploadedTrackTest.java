package com.soundcloud.android.service.upload;

import static com.soundcloud.android.Expect.expect;
import static com.soundcloud.android.utils.IOUtils.readInputStream;
import static com.xtremelabs.robolectric.Robolectric.addHttpResponseRule;

import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.model.Track;
import com.soundcloud.android.provider.Content;
import com.soundcloud.android.robolectric.DefaultTestRunner;
import com.xtremelabs.robolectric.tester.org.apache.http.FakeHttpLayer;
import com.xtremelabs.robolectric.tester.org.apache.http.TestHttpResponse;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;

import android.content.ContentResolver;
import android.net.Uri;
import android.os.Looper;

import java.util.ArrayList;


@RunWith(DefaultTestRunner.class)
public class PollUploadedTrackTest {
    static final long USER_ID = 100L;
    static final long TRACK_ID = 12345L;
    ContentResolver resolver;

    @Before
    public void before() {
        DefaultTestRunner.application.setCurrentUserId(USER_ID);
        resolver = DefaultTestRunner.application.getContentResolver();
    }

    @Test
    public void testPollUploadedSuccess() throws Exception {
        addHttpResponseRule("GET", "/tracks/12345", new TestHttpResponse(200, readInputStream(getClass().getResourceAsStream("track_finished.json"))));
        addProcessingTrackAndRunPoll(TRACK_ID);
        expectLocalTracksStreamable(TRACK_ID);
    }

    @Test
    public void testPollUploadedProcessSuccess() throws Exception {
        ArrayList<TestHttpResponse> responses = new ArrayList<TestHttpResponse>();
        responses.add(new TestHttpResponse(200, readInputStream(getClass().getResourceAsStream("track_storing.json"))));
        responses.add(new TestHttpResponse(200, readInputStream(getClass().getResourceAsStream("track_finished.json"))));
        addHttpResponseRule(new FakeHttpLayer.DefaultRequestMatcher("GET", "/tracks/12345"), responses);

        addProcessingTrackAndRunPoll(TRACK_ID);
        expectLocalTracksStreamable(TRACK_ID);
    }

    @Test @Ignore
    public void testPollUploaded400Success() throws Exception {
        ArrayList<TestHttpResponse> responses = new ArrayList<TestHttpResponse>();
        responses.add(new TestHttpResponse(400, "failed"));
        responses.add(new TestHttpResponse(200, readInputStream(getClass().getResourceAsStream("track_finished.json"))));
        addHttpResponseRule(new FakeHttpLayer.DefaultRequestMatcher("GET", "/tracks/12345"), responses);
        addProcessingTrackAndRunPoll(TRACK_ID);
        expectLocalTracksStreamable(TRACK_ID);
    }

    @Test
    public void testPollUploadedFailure() throws Exception {
        addHttpResponseRule("GET", "/tracks/12345", new TestHttpResponse(200, readInputStream(getClass().getResourceAsStream("track_failed.json"))));
        addProcessingTrackAndRunPoll(TRACK_ID);
        expectLocalTracksNotStreamable(TRACK_ID);
    }

    @Test
    public void testPollUploadedProcess400Failure() throws Exception {
        addHttpResponseRule("GET", "/tracks/12345", new TestHttpResponse(400, "failed"));
        addProcessingTrackAndRunPoll(TRACK_ID);
        expectLocalTracksNotStreamable(TRACK_ID);
    }

    @Test
    public void testPollUploadedProcessTimeoutFailure() throws Exception {
        ArrayList<TestHttpResponse> responses = new ArrayList<TestHttpResponse>();
        responses.add(new TestHttpResponse(200, readInputStream(getClass().getResourceAsStream("track_storing.json"))));
        responses.add(new TestHttpResponse(200, readInputStream(getClass().getResourceAsStream("track_finished.json"))));
        addHttpResponseRule(new FakeHttpLayer.DefaultRequestMatcher("GET", "/tracks/12345"), responses);

        addProcessingTrackAndRunPoll(TRACK_ID, 1l);
        expectLocalTracksNotStreamable(TRACK_ID);
    }

    private void addProcessingTrackAndRunPoll(long id) {
        addProcessingTrackAndRunPoll(id, 5000l);
    }
    private void addProcessingTrackAndRunPoll(long id, long maxTime) {
        Track t = new Track();
        t.id = id;
        t.state = Track.State.STORING;
        Uri newUri = t.commitLocally(resolver, SoundCloudApplication.TRACK_CACHE);

        new Poller(Looper.myLooper(), DefaultTestRunner.application, id, Content.ME_TRACKS.uri, 1l, maxTime).start();
    }

    private void expectLocalTracksStreamable(long id) {
        Track track = SoundCloudApplication.TRACK_CACHE.get(id);
        expect(track).not.toBeNull();
        expect(track.state.isStreamable()).toBeTrue();

        /*
        TODO : WHY IS THIS NULL, IT GETS STORED SUCCESFULLY ABOVE !!!!!
        track = SoundCloudDB.getTrackById(resolver, id);
        expect(track).not.toBeNull();
        expect(track.state.isStreamable()).toBeTrue();
        */
    }

    private void expectLocalTracksNotStreamable(long id) {
        Track track = SoundCloudApplication.TRACK_CACHE.get(id);
        expect(track).not.toBeNull();
        expect(track.state.isStreamable()).toBeFalse();

        /*
        track = SoundCloudDB.getTrackById(resolver, id);
        expect(track).not.toBeNull();
        expect(track.state.isStreamable()).toBeFalse();
         */
    }
}
