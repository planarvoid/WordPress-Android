package com.soundcloud.android.streaming;


import com.soundcloud.android.robolectric.DefaultTestRunner;
import com.xtremelabs.robolectric.Robolectric;
import com.xtremelabs.robolectric.tester.org.apache.http.FakeHttpLayer;
import com.xtremelabs.robolectric.tester.org.apache.http.TestHttpResponse;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.IOException;

import static com.soundcloud.android.Expect.expect;

@RunWith(DefaultTestRunner.class)
public class PlaycountTaskTest {

    @Test
    public void testLogPlaycountWithOldApi() throws Exception {
        Robolectric.addHttpResponseRule(
                new FakeHttpLayer.RequestMatcherBuilder()
//                        .header("Range", "bytes=0-1")
                        .method("GET")
                        .path("tracks/12345/stream"), new TestHttpResponse(302, ""));

        StreamItem item = new StreamItem("https://api.soundcloud.com/tracks/12345/stream");
        PlaycountTask t = new PlaycountTask(item, DefaultTestRunner.application, false);
        expect(t.execute().getBoolean("success", false)).toBeTrue();
    }

    @Test(expected = IOException.class)
    public void testLogPlaycountWithOldApiShouldRetry() throws Exception {
        Robolectric.addHttpResponseRule(
                new FakeHttpLayer.RequestMatcherBuilder()
//                        .header("Range", "bytes=0-1")
                        .method("GET")
                        .path("tracks/12345/stream"), new TestHttpResponse(503, ""));

        StreamItem item = new StreamItem("https://api.soundcloud.com/tracks/12345/stream");
        new PlaycountTask(item, DefaultTestRunner.application, false).execute();
    }

    @Test
    public void testLogPlaycountWithTrustedApi() throws Exception {

        Robolectric.addHttpResponseRule(
                new FakeHttpLayer.RequestMatcherBuilder()
                        .method("POST")
                        .path("tracks/12345/plays"), new TestHttpResponse(202, ""));

        StreamItem item = new StreamItem("https://api.soundcloud.com/tracks/12345/stream");
        PlaycountTask t = new PlaycountTask(item, DefaultTestRunner.application, true);
        expect(t.execute().getBoolean("success", false)).toBeTrue();
    }

    @Test(expected = IOException.class)
    public void testLogPlaycountWithTrustedApiShouldRetry() throws Exception {

        Robolectric.addHttpResponseRule(
                new FakeHttpLayer.RequestMatcherBuilder()
                        .method("POST")
                        .path("tracks/12345/plays"), new TestHttpResponse(503, ""));

        StreamItem item = new StreamItem("https://api.soundcloud.com/tracks/12345/stream");
        new PlaycountTask(item, DefaultTestRunner.application, true).execute();
    }

    @Test
    public void shouldNotLogIfItemIsUnavailable() throws Exception {
        StreamItem item = new StreamItem("https://api.soundcloud.com/tracks/12345/stream");
        item.markUnavailable();
        PlaycountTask t = new PlaycountTask(item, DefaultTestRunner.application, true);
        expect(t.execute().getBoolean("success", false)).toBeFalse();
    }
}
