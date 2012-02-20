package com.soundcloud.android.task.create;


import static com.xtremelabs.robolectric.Robolectric.addPendingHttpResponse;
import static org.junit.Assert.*;

import com.soundcloud.android.model.Connection;
import com.soundcloud.android.robolectric.DefaultTestRunner;
import com.soundcloud.android.robolectric.ApiTests;
import com.soundcloud.android.task.create.NewConnectionTask;
import org.junit.Test;
import org.junit.runner.RunWith;

import android.net.Uri;

@RunWith(DefaultTestRunner.class)
public class NewConnectionTaskTest extends ApiTests {
    @Test
    public void shouldReturnUri() throws Exception {
        addPendingHttpResponse(202, "{ \"authorize_url\": \"http://example.com\" }");
        NewConnectionTask task = new NewConnectionTask(api);
        Uri uri = task.doInBackground(Connection.Service.Myspace);
        assertNotNull(uri);
        assertEquals("http://example.com", uri.toString());
    }

    @Test
    public void shouldReturnNullUriInFailureCase() throws Exception {
        addPendingHttpResponse(400, "Failz");
        NewConnectionTask task = new NewConnectionTask(api);
        Uri uri = task.doInBackground(Connection.Service.Myspace);
        assertNull(uri);
    }
}
