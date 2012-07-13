package com.soundcloud.android.task.create;


import static com.xtremelabs.robolectric.Robolectric.addPendingHttpResponse;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import com.soundcloud.android.model.Connection;
import com.soundcloud.android.robolectric.DefaultTestRunner;
import org.junit.Test;
import org.junit.runner.RunWith;

import android.net.Uri;

@RunWith(DefaultTestRunner.class)
public class NewConnectionTaskTest {
    @Test
    public void shouldReturnUri() throws Exception {
        addPendingHttpResponse(202, "{ \"authorize_url\": \"http://example.com\" }");
        NewConnectionTask task = new NewConnectionTask(DefaultTestRunner.application);
        Uri uri = task.doInBackground(Connection.Service.Myspace);
        assertNotNull(uri);
        assertEquals("http://example.com", uri.toString());
    }

    @Test
    public void shouldReturnNullUriInFailureCase() throws Exception {
        addPendingHttpResponse(400, "Failz");
        NewConnectionTask task = new NewConnectionTask(DefaultTestRunner.application);
        Uri uri = task.doInBackground(Connection.Service.Myspace);
        assertNull(uri);
    }
}
