package com.soundcloud.android.task;

import static com.soundcloud.android.utils.CloudUtils.readInputStream;
import static com.xtremelabs.robolectric.Robolectric.addHttpResponseRule;
import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.assertThat;

import com.soundcloud.android.model.Track;
import com.soundcloud.android.robolectric.DefaultTestRunner;
import com.soundcloud.api.Endpoints;
import com.soundcloud.api.Request;
import com.xtremelabs.robolectric.tester.org.apache.http.TestHttpResponse;
import org.junit.Test;
import org.junit.runner.RunWith;


@RunWith(DefaultTestRunner.class)
public class LoadTrackInfoTaskTest {
    LoadTrackInfoTask.LoadTrackInfoListener listener;

    @Test
    public void testLoadTrackInfo() throws Exception {
        LoadTrackInfoTask task = new LoadTrackInfoTask(DefaultTestRunner.application, 0, true, true);

        addHttpResponseRule("GET", "/tracks/12345",
                new TestHttpResponse(200, readInputStream(getClass().getResourceAsStream("track.json"))));

        final Track[] track = {null};
        listener = new LoadTrackInfoTask.LoadTrackInfoListener() {
            @Override
            public void onTrackInfoLoaded(Track t, String action) {
                track[0] = t;
            }
            @Override
            public void onTrackInfoError(long trackId) {
            }
        };

        task.setListener(listener);
        task.execute(Request.to(Endpoints.TRACK_DETAILS, 12345));
        assertThat(track[0], not(nullValue()));
        assertThat(track[0].title, equalTo("recording on sunday night"));
    }
}
