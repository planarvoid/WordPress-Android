package com.soundcloud.android.view;


import android.app.Activity;
import com.soundcloud.android.CloudAPI;
import com.soundcloud.android.api.ApiTest;
import com.xtremelabs.robolectric.RobolectricTestRunner;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.IOException;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.reset;

@RunWith(RobolectricTestRunner.class)
public class ConnectionListTests extends ApiTest implements CloudAPI.Enddpoints {

    @Test
    public void shouldLoadConnectionsFromApi() throws Exception {
        fakeApi(CONNECTIONS, "connections.json");

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
        fakeApi(CONNECTIONS, "connections.json");

        ConnectionList list = new ConnectionList(new Activity());

        ConnectionList.Adapter adapter = new ConnectionList.Adapter(api);
        list.setAdapter(adapter);
        adapter.load();

        assertEquals(1, list.postToServiceIds().size());

        reset(api);
        fakeApi(CONNECTIONS, new IOException());

        list.getAdapter().loadIfNecessary();
        assertEquals(1, list.postToServiceIds().size());
    }
}
