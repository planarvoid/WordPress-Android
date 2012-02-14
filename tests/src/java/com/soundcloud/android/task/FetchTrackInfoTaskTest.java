package com.soundcloud.android.task;

import static com.soundcloud.android.utils.IOUtils.readInputStream;
import static com.xtremelabs.robolectric.Robolectric.addHttpResponseRule;
import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.assertThat;

import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.model.Track;
import com.soundcloud.android.provider.SoundCloudDB;
import com.soundcloud.android.robolectric.DefaultTestRunner;
import com.soundcloud.android.task.fetch.FetchTrackTask;
import com.soundcloud.api.Endpoints;
import com.soundcloud.api.Request;
import com.xtremelabs.robolectric.Robolectric;
import com.xtremelabs.robolectric.tester.org.apache.http.TestHttpResponse;
import org.junit.Test;
import org.junit.runner.RunWith;


@RunWith(DefaultTestRunner.class)
public class FetchTrackInfoTaskTest {
    FetchTrackTask.FetchTrackListener listener;

    @Test
    public void testFetchTrackInfo() throws Exception {
        FetchTrackTask task = new FetchTrackTask(DefaultTestRunner.application, 0);

        addHttpResponseRule("GET", "/tracks/12345",
                new TestHttpResponse(200, readInputStream(getClass().getResourceAsStream("track.json"))));

        Track t = new Track();
        t.id = 12345;
        t.title = "Old Title";
        t.filelength = 9999;
        ((SoundCloudApplication) Robolectric.application).TRACK_CACHE.put(t);

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
        assertThat(track[0], not(nullValue()));
        assertThat(track[0].title, equalTo("recording on sunday night"));

        t = SoundCloudDB.getTrackById(Robolectric.application.getContentResolver(),12345);
        assertThat(t, not(nullValue()));
        assertThat(t.title, equalTo("recording on sunday night"));

        t = ((SoundCloudApplication) Robolectric.application).TRACK_CACHE.get(12345l);
        assertThat(t, not(nullValue()));
        assertThat(t.title, equalTo("recording on sunday night"));
        assertThat(t.filelength, equalTo(9999l));


    }
}
