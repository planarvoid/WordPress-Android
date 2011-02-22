package com.soundcloud.android.task;

import com.soundcloud.utils.ApiWrapper;
import com.xtremelabs.robolectric.Robolectric;
import com.xtremelabs.robolectric.RobolectricTestRunner;
import com.xtremelabs.robolectric.tester.org.apache.http.FakeHttpLayer;
import com.xtremelabs.robolectric.tester.org.apache.http.HttpEntityStub;
import com.xtremelabs.robolectric.tester.org.apache.http.HttpResponseStub;
import org.apache.http.HttpEntity;
import org.apache.http.HttpException;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.hamcrest.CoreMatchers;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;

import java.io.IOException;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.matches;
import static org.mockito.Mockito.mock;


@RunWith(RobolectricTestRunner.class)
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
