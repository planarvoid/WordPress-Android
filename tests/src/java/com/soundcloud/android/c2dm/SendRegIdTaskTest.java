package com.soundcloud.android.c2dm;

import com.soundcloud.android.robolectric.ApiTests;
import com.xtremelabs.robolectric.tester.org.apache.http.FakeHttpLayer;
import org.apache.http.HttpStatus;
import org.apache.http.message.BasicHeader;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.soundcloud.android.robolectric.DefaultTestRunner;

import com.xtremelabs.robolectric.Robolectric;

import static com.soundcloud.android.Expect.expect;

import java.io.IOException;

@RunWith(DefaultTestRunner.class)
public class SendRegIdTaskTest extends ApiTests {

    @Test
    public void testExecuteSuccess() throws Exception {
        final String location = "https://api.soundcloud.com/me/devices/1234";

        Robolectric.addPendingHttpResponse(HttpStatus.SC_CREATED, "",
                new BasicHeader("Location", location));

        expect(new SendRegIdTask(api, null).doInBackground("reg_id", "com.soundcloud.android")).toEqual(location);
    }

    @Test
    public void testExecuteFailure() throws Exception {
        Robolectric.addPendingHttpResponse(HttpStatus.SC_FORBIDDEN, "");
        expect(new SendRegIdTask(api, null).doInBackground("reg_id", "com.soundcloud.android")).toBeNull();
    }

    @Test
    public void testExecuteIOException() throws Exception {
        Robolectric.getFakeHttpLayer().addHttpResponseRule(new FakeHttpLayer.RequestMatcherResponseRule(
                new FakeHttpLayer.RequestMatcherBuilder()
                        .method("POST")
                        .path("me/devices"), new IOException("boom")));

        expect(new SendRegIdTask(api, null).doInBackground("reg_id", "com.soundcloud.android")).toBeNull();
    }
}
