package com.soundcloud.android.task.fetch;

import static com.soundcloud.android.Expect.expect;
import static com.soundcloud.android.utils.IOUtils.readInputStream;
import static com.xtremelabs.robolectric.Robolectric.addHttpResponseRule;

import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.model.Track;
import com.soundcloud.android.robolectric.DefaultTestRunner;
import com.soundcloud.api.Endpoints;
import com.soundcloud.api.Request;
import com.xtremelabs.robolectric.tester.org.apache.http.TestHttpResponse;
import org.junit.Test;
import org.junit.runner.RunWith;


@RunWith(DefaultTestRunner.class)
public class FetchTrackTaskTest {
    @Test
    public void testFetchTrack() throws Exception {
        FetchTrackTask task = new FetchTrackTask(DefaultTestRunner.application.getCloudAPI());

        addHttpResponseRule("GET", "/tracks/12345",
                new TestHttpResponse(200, readInputStream(getClass().getResourceAsStream("../track.json"))));

        Track t = new Track();
        t.setId(12345);
        t.title = "Old Title";
        SoundCloudApplication.MODEL_MANAGER.cache(t);

        final Track[] track = {null};
        FetchModelTask.Listener<Track> listener = new FetchModelTask.Listener<Track>() {
            @Override
            public void onSuccess(Track t) {
                track[0] = t;
            }

            @Override
            public void onError(Object context) {
            }
        };

        task.addListener(listener);
        task.execute(Request.to(Endpoints.TRACK_DETAILS, 12345));
        expect(track[0]).not.toBeNull();
        expect(track[0].title).toEqual("recording on sunday night");

        t = SoundCloudApplication.MODEL_MANAGER.getTrack(12345);
        expect(t).not.toBeNull();
        expect(t.title).toEqual("recording on sunday night");

        t = SoundCloudApplication.MODEL_MANAGER.getCachedTrack(12345l);
        expect(t).not.toBeNull();
        expect(t.title).toEqual("recording on sunday night");
    }
}
