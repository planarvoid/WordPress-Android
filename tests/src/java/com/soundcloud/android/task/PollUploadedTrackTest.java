package com.soundcloud.android.task;

import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.model.Track;
import com.soundcloud.android.provider.Content;
import com.soundcloud.android.provider.SoundCloudDB;
import com.soundcloud.android.robolectric.DefaultTestRunner;
import com.soundcloud.android.task.fetch.FetchTrackTask;
import com.soundcloud.api.Endpoints;
import com.soundcloud.api.Request;
import com.xtremelabs.robolectric.Robolectric;
import com.xtremelabs.robolectric.tester.org.apache.http.TestHttpResponse;
import org.junit.Test;
import org.junit.runner.RunWith;

import static com.soundcloud.android.Expect.expect;
import static com.soundcloud.android.utils.IOUtils.readInputStream;
import static com.xtremelabs.robolectric.Robolectric.addHttpResponseRule;


@RunWith(DefaultTestRunner.class)
public class PollUploadedTrackTest {
    FetchTrackTask.FetchTrackListener listener;

    @Test
    public void testPollUploadedSuccess() throws Exception {
        addHttpResponseRule("GET", "/tracks/12345", new TestHttpResponse(200, readInputStream(getClass().getResourceAsStream("track.json"))));
        final long id = 12345l;
        final Track track = addProcessingTrackAndRunPoll(id);

        expect(track).not.toBeNull();
        expect(track.state.isStreamable()).toBeTrue();
        checkFinishedInStorage(id);
    }

    @Test
    public void testPollUploadedProcessFailureThenSuccess() throws Exception {
        addHttpResponseRule("GET", "/tracks/12345", new TestHttpResponse(400, "failed"));
        addHttpResponseRule("GET", "/tracks/12345", new TestHttpResponse(200, readInputStream(getClass().getResourceAsStream("track.json"))));
        final long id = 12345l;
        final Track track = addProcessingTrackAndRunPoll(id);

        expect(track).not.toBeNull();
        expect(track.state.isStreamable()).toBeTrue();
        checkFinishedInStorage(id);
    }

    private Track addProcessingTrackAndRunPoll(long id) {
        PollUploadedTrackTask task = new PollUploadedTrackTask(DefaultTestRunner.application, 0, Content.ME_TRACKS.uri);
        Track t = new Track();
        t.id = id;
        t.state = Track.State.STORING;
        SoundCloudApplication.TRACK_CACHE.put(t);

        final Track[] track = {null};
        listener = new FetchTrackTask.FetchTrackListener() {
            @Override
            public void onSuccess(Track t, String action) {
                track[0] = t;
            }

            @Override
            public void onError(long modelId) {
            }
        };

        task.addListener(listener);
        task.execute(Request.to(Endpoints.TRACK_DETAILS, id));
        return track[0];
    }


    private void checkFinishedInStorage(long id) throws Exception {
        Track t;
        t = SoundCloudDB.getTrackById(Robolectric.application.getContentResolver(), id);
        expect(t).toBeNull();
        expect(t.state.isStreamable()).toBeTrue();

        t = SoundCloudApplication.TRACK_CACHE.get(id);
        expect(t).not.toBeNull();
        expect(t.state.isStreamable()).toBeTrue();
    }

}
