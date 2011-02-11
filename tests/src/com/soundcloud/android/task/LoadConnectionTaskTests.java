package com.soundcloud.android.task;

import com.soundcloud.android.api.ApiTest;
import com.soundcloud.android.objects.Connection;
import com.xtremelabs.robolectric.RobolectricTestRunner;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

@RunWith(RobolectricTestRunner.class)
public class LoadConnectionTaskTests extends ApiTest {
    @Test
    public void testDeserialisation() throws
            Exception {
        fakeApi(CONNECTIONS, "connections.json");

        LoadConnectionsTask task = new LoadConnectionsTask(api);
        List<Connection> connections = task.list(CONNECTIONS, Connection.class);

        assertEquals(6, connections.size());
        // make sure date gets deserialized properly
        Connection conn = connections.get(0);

        assertEquals("Wed Dec 16 19:52:56 CET 2009", conn.created_at.toString());
        assertEquals("twitter", conn.type);
        assertEquals(Connection.Service.Twitter, conn.type());
        assertEquals("twitter", conn.service);
        assertEquals(true, conn.post_publish);
        assertEquals(false, conn.post_favorite);
        assertEquals("foo", conn.display_name);
        assertEquals(41335, conn.id);
        assertEquals("https://api.sandbox-soundcloud.com/connections/41335", conn.uri.toString());


        int unknown = 0;
        for (Connection c : connections) {
            assertNotNull(c.type());
            if (c.type() == Connection.Service.Unknown) unknown++;
        }
        assertEquals(1, unknown);
        assertEquals(Connection.Service.Unknown, connections.get(connections.size()-1).type());
    }
}
