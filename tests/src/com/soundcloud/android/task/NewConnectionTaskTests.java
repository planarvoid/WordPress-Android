package com.soundcloud.android.task;


import android.net.Uri;

import com.soundcloud.android.robolectric.DefaultTestRunner;
import com.soundcloud.api.BaseApiTest;
import com.soundcloud.android.objects.Connection;
import com.soundcloud.api.ApiWrapper;
import com.xtremelabs.robolectric.Robolectric;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.*;

@RunWith(DefaultTestRunner.class)
public class NewConnectionTaskTests extends BaseApiTest {

    @Test
    public void shouldReturnUri() throws Exception {
        Robolectric.addPendingHttpResponse(202, "{ \"authorize_url\": \"http://example.com\" }");

        NewConnectionTask task = new NewConnectionTask(new ApiWrapper());
        Uri uri = task.doInBackground(Connection.Service.Myspace);

        assertNotNull(uri);
        assertEquals("http://example.com", uri.toString());
    }

    @Test
    public void shouldReturnNullUriInFailureCase() throws Exception {
        Robolectric.addPendingHttpResponse(400, "Failz");

        NewConnectionTask task = new NewConnectionTask(new ApiWrapper());
        Uri uri = task.doInBackground(Connection.Service.Myspace);
        assertNull(uri);
    }
}
