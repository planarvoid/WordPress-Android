package com.soundcloud.android.task;

import com.soundcloud.android.CloudAPI;
import com.soundcloud.android.objects.Connection;
import com.soundcloud.android.task.LoadConnectionsTask;
import com.xtremelabs.robolectric.Robolectric;
import com.xtremelabs.robolectric.RobolectricTestRunner;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.*;

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

        assertEquals(1, connections.size());
        assertEquals("Fri Feb 04 16:05:26 CET 2011", connections.get(0).created_at.toString());

        verify(api);
    }
}
