package com.soundcloud.android.task;


import static org.junit.Assert.*;

import com.soundcloud.android.objects.Connection;
import com.soundcloud.android.robolectric.DefaultTestRunner;
import com.soundcloud.android.robolectric.RoboApiBaseTests;
import org.junit.Test;
import org.junit.runner.RunWith;

import android.net.Uri;

@RunWith(DefaultTestRunner.class)
public class NewConnectionTaskTests extends RoboApiBaseTests {

    @Test
    public void shouldReturnUri() throws Exception {
        expectPostRequestAndReturn(MY_CONNECTIONS, 202, "{ \"authorize_url\": \"http://example.com\" }");

        NewConnectionTask task = new NewConnectionTask(api);
        Uri uri = task.doInBackground(Connection.Service.Myspace);

        assertNotNull(uri);
        assertEquals("http://example.com", uri.toString());
    }

    @Test
    public void shouldReturnNullUriInFailureCase() throws Exception {
        expectPostRequestAndReturn(MY_CONNECTIONS, 400, "Failz");

        NewConnectionTask task = new NewConnectionTask(api);
        Uri uri = task.doInBackground(Connection.Service.Myspace);
        assertNull(uri);
    }
}
