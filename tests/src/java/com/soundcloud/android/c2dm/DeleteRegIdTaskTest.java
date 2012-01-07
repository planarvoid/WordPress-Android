package com.soundcloud.android.c2dm;

import com.soundcloud.android.robolectric.ApiTests;
import com.xtremelabs.robolectric.tester.org.apache.http.FakeHttpLayer;
import com.xtremelabs.robolectric.tester.org.apache.http.TestHttpResponse;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.soundcloud.android.robolectric.DefaultTestRunner;

import com.xtremelabs.robolectric.Robolectric;

import static com.soundcloud.android.Expect.expect;
import static com.xtremelabs.robolectric.Robolectric.addHttpResponseRule;

import java.io.IOException;

@RunWith(DefaultTestRunner.class)
public class DeleteRegIdTaskTest extends ApiTests {

    @Test
    public void testExecuteSuccess() throws Exception {
        addHttpResponseRule("DELETE", "https://api.soundcloud.com/me/devices/1234",
                new TestHttpResponse(200, ""));

        expect(new DeleteRegIdTask(api, null).doInBackground("https://api.soundcloud.com/me/devices/1234"))
                .toBeTrue();

    }

    @Test
    public void testExecuteFailure() throws Exception {
        Robolectric.getFakeHttpLayer().addHttpResponseRule(new FakeHttpLayer.RequestMatcherResponseRule(
                new FakeHttpLayer.RequestMatcherBuilder()
                        .method("DELETE")
                        .path("me/devices/1234"), new IOException("boom")));

        expect(new DeleteRegIdTask(api, null).doInBackground("https://api.soundcloud.com/me/devices/1234"))
                .toBeFalse();
    }

    @Test
    public void nonOkResponseShouldBeFailure() throws Exception {
        addHttpResponseRule("DELETE", "https://api.soundcloud.com/me/devices/1234",
                new TestHttpResponse(402, ""));
        expect(new DeleteRegIdTask(api, null).doInBackground("https://api.soundcloud.com/me/devices/1234"))
                .toBeFalse();
    }

    @Test
    public void forbiddenResponseShouldBeTreatedAsSuccess() throws Exception {
        addHttpResponseRule("DELETE", "https://api.soundcloud.com/me/devices/1234",
                new TestHttpResponse(403, ""));
        expect(new DeleteRegIdTask(api, null).doInBackground("https://api.soundcloud.com/me/devices/1234"))
                .toBeTrue();
    }


    @Test
    public void notFoundResponseShouldBeSuccess() throws Exception {
        addHttpResponseRule("DELETE", "https://api.soundcloud.com/me/devices/1234",
                new TestHttpResponse(404, ""));
        expect(new DeleteRegIdTask(api, null).doInBackground("https://api.soundcloud.com/me/devices/1234"))
                .toBeTrue();
    }
}
