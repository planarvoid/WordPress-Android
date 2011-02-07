package com.soundcloud.android.task;

import com.soundcloud.android.CloudAPI;
import com.soundcloud.android.objects.Connection;
import com.xtremelabs.robolectric.RobolectricTestRunner;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(RobolectricTestRunner.class)
public class LoadConnectionTaskTests {
    CloudAPI api;

    @Before
    public void setUp() throws Exception {
        api = mock(CloudAPI.class);
    }

    @Test
    public void testDeserialisation() throws Exception {
        when(api.executeRequest("/me/connections.json")).thenReturn(getClass().getResourceAsStream("connections.json"));

        LoadConnectionsTask task = new LoadConnectionsTask(api);
        List<Connection> connections = task.list("/me/connections.json", Connection.class);

        assertEquals(2, connections.size());
        // make sure date gets deserialized properly
        Connection conn = connections.get(0);

        assertEquals("Wed Dec 16 19:52:56 CET 2009", conn.created_at.toString());
        assertEquals("twitter", conn.type);
        assertEquals("twitter", conn.service);
        assertEquals(true, conn.post_publish);
        assertEquals(false, conn.post_favorite);
        assertEquals("foo", conn.display_name);
        assertEquals(41335, conn.id);
        assertEquals("https://api.sandbox-soundcloud.com/connections/41335", conn.uri);
    }
}
