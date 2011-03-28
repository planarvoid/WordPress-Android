package com.soundcloud.android.task;

import com.soundcloud.android.robolectric.DefaultTestRunner;
import com.soundcloud.api.ApiWrapper;
import com.xtremelabs.robolectric.Robolectric;
import com.xtremelabs.robolectric.RobolectricTestRunner;
import com.xtremelabs.robolectric.tester.org.apache.http.HttpEntityStub;
import org.apache.http.HttpException;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.IOException;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;


@RunWith(DefaultTestRunner.class)
public class CheckFollowingStatusTaskTests {

    @Test
    public void shouldReturnFalseIfNotFollowing() throws Exception {
        Robolectric.addPendingHttpResponse(404, null);

        CheckFollowingStatusTask task = new CheckFollowingStatusTask(new ApiWrapper());
        assertThat(task.doInBackground(1000), equalTo(false));
    }

    @Test
    public void shouldReturnTrueIfFollowing() throws Exception {
        Robolectric.addPendingHttpResponse(303, null);

        CheckFollowingStatusTask task = new CheckFollowingStatusTask(new ApiWrapper());
        assertThat(task.doInBackground(1000), equalTo(true));
    }

    @Test
    public void shouldReturnNullIfUndecided() throws Exception {
        Robolectric.addPendingHttpResponse(666, null);

        CheckFollowingStatusTask task = new CheckFollowingStatusTask(new ApiWrapper());
        assertThat(task.doInBackground(1000), nullValue());
    }

    @Test
    public void shouldReturnNullIfExceptionRaised() throws Exception {
        Robolectric.getFakeHttpLayer().addHttpResponseRule(THROWING);
        CheckFollowingStatusTask task = new CheckFollowingStatusTask(new ApiWrapper());
        assertThat(task.doInBackground(1000), nullValue());
    }

    public static final HttpEntityStub.ResponseRule THROWING = new HttpEntityStub.ResponseRule() {
        @Override
        public boolean matches(HttpRequest request) {
            return true;
        }

        @Override
        public HttpResponse getResponse() throws HttpException, IOException {
            throw new IOException();
        }
    };
}
