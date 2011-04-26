package com.soundcloud.android.view;


import static org.junit.Assert.assertEquals;

import com.soundcloud.android.robolectric.DefaultTestRunner;
import com.soundcloud.android.robolectric.RoboApiBaseTests;
import org.junit.Test;
import org.junit.runner.RunWith;

import android.app.Activity;

import java.io.IOException;

@RunWith(DefaultTestRunner.class)
public class ConnectionListTests extends RoboApiBaseTests {

    @Test
    public void shouldLoadConnectionsFromApi() throws Exception {
        expectGetRequestAndReturn(MY_CONNECTIONS, 200, "connections.json");

        ConnectionList list = new ConnectionList(new Activity());
        assertEquals(0, list.postToServiceIds().size());

        ConnectionList.Adapter adapter = new ConnectionList.Adapter(api);
        list.setAdapter(adapter);
        adapter.load();

        assertEquals(1, list.postToServiceIds().size());
        int id = list.postToServiceIds().iterator().next();
        assertEquals(41335, id);
    }

    @Test
    public void shouldOnlyReloadIfNeeded() throws Exception {
        expectGetRequestAndReturn(MY_CONNECTIONS, 200, "connections.json");

        ConnectionList list = new ConnectionList(new Activity());

        ConnectionList.Adapter adapter = new ConnectionList.Adapter(api);
        list.setAdapter(adapter);
        adapter.load();

        assertEquals(1, list.postToServiceIds().size());

        expectGetRequestAndThrow(MY_CONNECTIONS, new IOException());

        list.getAdapter().loadIfNecessary();
        assertEquals(1, list.postToServiceIds().size());
    }
}
