package com.soundcloud.android.task.fetch;

import static com.soundcloud.android.Expect.expect;
import static com.soundcloud.android.utils.IOUtils.readInputStream;
import static com.xtremelabs.robolectric.Robolectric.addHttpResponseRule;

import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.model.Track;
import com.soundcloud.android.provider.SoundCloudDB;
import com.soundcloud.android.robolectric.DefaultTestRunner;
import com.soundcloud.api.Endpoints;
import com.soundcloud.api.Request;
import com.xtremelabs.robolectric.Robolectric;
import com.xtremelabs.robolectric.tester.org.apache.http.TestHttpResponse;
import org.junit.Test;
import org.junit.runner.RunWith;


@RunWith(DefaultTestRunner.class)
public class FetchTrackTaskTest {
    FetchTrackTask.FetchTrackListener listener;

    @Test
    public void testFetchTrack() throws Exception {
        FetchTrackTask task = new FetchTrackTask(DefaultTestRunner.application, 0);

        addHttpResponseRule("GET", "/tracks/12345",
                new TestHttpResponse(200, readInputStream(getClass().getResourceAsStream("../track.json"))));

        Track t = new Track();
        t.id = 12345;
        t.title = "Old Title";
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
        task.execute(Request.to(Endpoints.TRACK_DETAILS, 12345));
        expect(track[0]).not.toBeNull();
        expect(track[0].title).toEqual("recording on sunday night");

        t = SoundCloudDB.getTrackById(Robolectric.application.getContentResolver(),12345);
        expect(t).not.toBeNull();
        expect(t.title).toEqual("recording on sunday night");

        t = SoundCloudApplication.TRACK_CACHE.get(12345l);
        expect(t).not.toBeNull();
        expect(t.title).toEqual("recording on sunday night");
    }
}
