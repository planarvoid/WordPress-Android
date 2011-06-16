package com.soundcloud.android.task;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.assertThat;

import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.activity.ScActivity;
import com.soundcloud.android.adapter.LazyBaseAdapter;
import com.soundcloud.android.adapter.LazyEndlessAdapter;
import com.soundcloud.android.model.Track;
import com.soundcloud.android.model.User;
import com.soundcloud.android.robolectric.DefaultTestRunner;
import com.soundcloud.api.Request;
import com.xtremelabs.robolectric.Robolectric;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.IOException;
import java.io.InputStream;


@RunWith(DefaultTestRunner.class)
// XXX should extend ApiTests and test against AndroidCloudAPI
public class AppendTaskTest {
    ScActivity activity;
    SoundCloudApplication app;

    @Before
    public void setup() {
        app = (SoundCloudApplication) Robolectric.application;
        activity = new ScActivity() {
            @Override
            public SoundCloudApplication getApp() {
                return app;
            }
        };
    }

    @Test @Ignore
    public void shouldDeserializeTracks() throws Exception {
        AppendTask task = new AppendTask(app);

        LazyEndlessAdapter adapter = new LazyEndlessAdapter(activity,
                new LazyBaseAdapter(null, null,  Track.class), null);

        task.setAdapter(adapter);

        Robolectric.addPendingHttpResponse(200, slurp("tracks.json"));
        task.doInBackground(Request.to("http://foo.com"));

        assertThat(task.newItems, not(nullValue()));
        assertThat(task.newItems.size(), equalTo(11));
    }


    @Test @Ignore
    public void shouldDeserializeUsers() throws Exception {
        AppendTask task = new AppendTask(app);
        ScActivity activity = new ScActivity() {
            @Override
            public SoundCloudApplication getApp() {
                return app;
            }
        };

        LazyEndlessAdapter adapter = new LazyEndlessAdapter(activity,
                new LazyBaseAdapter(null, null, User.class), null);

        task.setAdapter(adapter);

        Robolectric.addPendingHttpResponse(200, slurp("users.json"));
        task.doInBackground(Request.to("http://foo.com"));

        assertThat(task.newItems, not(nullValue()));
        assertThat(task.newItems.size(), equalTo(1));
    }


    private String slurp(String res) throws IOException {
        StringBuilder sb = new StringBuilder(65536);
        int n;
        byte[] buffer = new byte[8192];
        InputStream is = getClass().getResourceAsStream(res);
        while ((n = is.read(buffer)) != -1) sb.append(new String(buffer, 0, n));
        return sb.toString();
    }
}
