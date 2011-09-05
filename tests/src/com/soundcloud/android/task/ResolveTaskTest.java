package com.soundcloud.android.task;

import com.xtremelabs.robolectric.tester.org.apache.http.TestHttpResponse;
import org.apache.http.message.BasicHeader;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.soundcloud.android.robolectric.DefaultTestRunner;

import com.xtremelabs.robolectric.Robolectric;

import static com.xtremelabs.robolectric.Robolectric.addHttpResponseRule;
import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.assertThat;

import android.net.Uri;


@RunWith(DefaultTestRunner.class)
public class ResolveTaskTest {
    ResolveTask.ResolveListener listener;

    @Test
    public void testResolving() throws Exception {
        ResolveTask task = new ResolveTask(DefaultTestRunner.application);

        addHttpResponseRule("GET", "/resolve?url=http%3A%2F%2Fsoundcloud.com%2Ffoo%2Fbar",
                new TestHttpResponse(304, "", new BasicHeader("Location", "http://foo.com")));

        final Uri[] result = {null};
        listener = new ResolveTask.ResolveListener() {
            @Override public void onUrlResolved(Uri uri) {
                result[0] = uri;
            }
            @Override public void onUrlError() {
            }
        };

        task.setListener(listener);
        task.execute(Uri.parse("http://soundcloud.com/foo/bar"));
        assertThat(result[0], equalTo(Uri.parse("http://foo.com")));
    }
}
