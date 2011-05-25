package com.soundcloud.android.task;


import static org.junit.Assert.*;

import com.soundcloud.android.objects.Connection;
import com.soundcloud.android.robolectric.DefaultTestRunner;
import com.soundcloud.android.robolectric.RoboApiBaseTests;
import com.xtremelabs.robolectric.Robolectric;
import org.junit.Test;
import org.junit.runner.RunWith;

import android.net.Uri;

@RunWith(DefaultTestRunner.class)
public class NewConnectionTaskTest extends RoboApiBaseTests {
    @Test
    public void shouldReturnUri() throws Exception {
        Robolectric.addPendingHttpResponse(202, "{ \"authorize_url\": \"http://example.com\" }");
        NewConnectionTask task = new NewConnectionTask(api);
        Uri uri = task.doInBackground(Connection.Service.Myspace);
        assertNotNull(uri);
        assertEquals("http://example.com", uri.toString());
    }

    @Test
    public void shouldReturnNullUriInFailureCase() throws Exception {
        Robolectric.addPendingHttpResponse(400, "Failz");
        NewConnectionTask task = new NewConnectionTask(api);
        Uri uri = task.doInBackground(Connection.Service.Myspace);
        assertNull(uri);
    }
}
