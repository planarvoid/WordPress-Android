package com.soundcloud.android.task;

import com.soundcloud.android.CloudUtils;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.activity.ScActivity;
import com.soundcloud.android.adapter.LazyBaseAdapter;
import com.soundcloud.android.adapter.LazyEndlessAdapter;
import com.soundcloud.android.api.ApiTest;
import org.apache.http.RequestLine;
import org.apache.http.client.methods.HttpUriRequest;
import org.hamcrest.CoreMatchers;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.xtremelabs.robolectric.Robolectric;
import com.xtremelabs.robolectric.RobolectricTestRunner;
import org.mockito.Mockito;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;


@RunWith(RobolectricTestRunner.class)
public class AppendTaskTests {

    @Before
    public void before() {
        Robolectric.bindShadowClass(ApiTest.ShadowLog.class);
    }

    @Test @Ignore
    public void shouldDeserializeTracks() throws Exception {
        // XXX need to make this easier testable
        final SoundCloudApplication app = new SoundCloudApplication() {
            @Override
            protected String getConsumerKey(boolean production) {
                return "";
            }

            @Override
            protected String getConsumerSecret(boolean production) {
                return "";
            }
        };
        app.onCreate();

        AppendTask task = new AppendTask(app);
        
        ScActivity activity = new ScActivity() {
            @Override
            public SoundCloudApplication getSoundCloudApplication() {
                return app;
            }
        };


        LazyEndlessAdapter adapter = new LazyEndlessAdapter(activity,
                new LazyBaseAdapter(null, null),
                "",  CloudUtils.Model.track);

        task.setAdapter(adapter);

        HttpUriRequest request = mock(HttpUriRequest.class);
        when(request.getURI()).thenReturn(URI.create("http://foo.com"));
        when(request.getMethod()).thenReturn("GET");
        RequestLine line = mock(RequestLine.class);
        when(line.getMethod()).thenReturn("GET");
        when(request.getRequestLine()).thenReturn(line);


        Robolectric.addPendingHttpResponse(200, slurp("tracks.json"));
        task.doInBackground(request);

        assertThat(task.newItems, not(nullValue()));
        assertThat(task.newItems.size(), equalTo(11));
    }


    @Test @Ignore
    public void shouldDeserializeUsers() throws Exception {
        // XXX need to make this easier testable
        final SoundCloudApplication app = new SoundCloudApplication() {
            @Override
            protected String getConsumerKey(boolean production) {
                return "";
            }

            @Override
            protected String getConsumerSecret(boolean production) {
                return "";
            }
        };
        app.onCreate();
        
        AppendTask task = new AppendTask(app);

        ScActivity activity = new ScActivity() {
            @Override
            public SoundCloudApplication getSoundCloudApplication() {
                return app;
            }
        };


        LazyEndlessAdapter adapter = new LazyEndlessAdapter(activity,
                new LazyBaseAdapter(null, null),
                "",  CloudUtils.Model.user);

        task.setAdapter(adapter);

        HttpUriRequest request = mock(HttpUriRequest.class);
        when(request.getURI()).thenReturn(URI.create("http://foo.com"));
        when(request.getMethod()).thenReturn("GET");
        RequestLine line = mock(RequestLine.class);
        when(line.getMethod()).thenReturn("GET");
        when(request.getRequestLine()).thenReturn(line);


        Robolectric.addPendingHttpResponse(200, slurp("users.json"));
        task.doInBackground(request);

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
