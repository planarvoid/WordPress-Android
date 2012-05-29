package com.soundcloud.android.c2dm;

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
public class SendRegIdTaskTest {

    @Test(expected = IllegalArgumentException.class)
    public void shouldCheckNumberOfParameters() throws Exception {
        new SendRegIdTask(DefaultTestRunner.application, null).doInBackground("reg_id", "com.soundcloud.android");
    }

    @Test
    public void testExecuteSuccess() throws Exception {
        final String location = "https://api.soundcloud.com/me/devices/1234";

        Robolectric.addPendingHttpResponse(HttpStatus.SC_CREATED, "",
                new BasicHeader("Location", location));

        expect(new SendRegIdTask(DefaultTestRunner.application, null)
                .doInBackground("reg_id", "com.soundcloud.android", "some_id")).toEqual(location);
    }

    @Test
    public void testExecuteFailure() throws Exception {
        Robolectric.addPendingHttpResponse(HttpStatus.SC_FORBIDDEN, "");
        expect(new SendRegIdTask(DefaultTestRunner.application, null).doInBackground("reg_id", "com.soundcloud.android", "some_id")).toBeNull();
    }

    @Test
    public void testExecuteIOException() throws Exception {
        Robolectric.getFakeHttpLayer().addHttpResponseRule(new FakeHttpLayer.RequestMatcherResponseRule(
                new FakeHttpLayer.RequestMatcherBuilder()
                        .method("POST")
                        .path("me/devices"), new IOException("boom")));

        expect(new SendRegIdTask(DefaultTestRunner.application, null)
                .doInBackground("reg_id", "com.soundcloud.android", "some_id")).toBeNull();
    }
}
