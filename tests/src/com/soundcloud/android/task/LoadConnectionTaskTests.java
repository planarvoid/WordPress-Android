package com.soundcloud.android.task;

import com.soundcloud.android.robolectric.DefaultTestRunner;
import com.soundcloud.api.BaseApiTest;
import com.soundcloud.android.objects.Connection;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

@RunWith(DefaultTestRunner.class)
public class LoadConnectionTaskTests extends BaseApiTest {
    @Test
    public void shouldDeserializeJsonProperly() throws Exception {
        expectGetRequestAndReturn(CONNECTIONS, "connections.json");

        LoadConnectionsTask task = new LoadConnectionsTask(api);
        List<Connection> connections = task.list(CONNECTIONS, Connection.class);

        assertEquals(6, connections.size());
        // make sure date gets deserialized properly
        Connection conn = connections.get(0);

        assertEquals("16 Dec 2009 18:52:56 GMT", conn.created_at.toGMTString());
        assertEquals("twitter", conn.service);
        assertEquals(Connection.Service.Twitter, conn.service());
        assertEquals("twitter", conn.service);
        assertEquals(true, conn.post_publish);
        assertEquals(false, conn.post_favorite);
        assertEquals("foo", conn.display_name);
        assertEquals(41335, conn.id);
        assertEquals("https://api.sandbox-soundcloud.com/connections/41335", conn.uri.toString());


        int unknown = 0;
        for (Connection c : connections) {
            assertNotNull(c.service());
            if (c.service() == Connection.Service.Unknown) unknown++;
        }
        assertEquals(1, unknown);
        assertEquals(Connection.Service.Unknown, connections.get(connections.size()-1).service());
    }
}
