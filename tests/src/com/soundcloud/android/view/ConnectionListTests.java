package com.soundcloud.android.view;


import android.app.Activity;
import com.soundcloud.android.api.ApiTest;
import com.soundcloud.android.objects.Connection;
import com.xtremelabs.robolectric.RobolectricTestRunner;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;

import java.io.IOException;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.reset;

@RunWith(RobolectricTestRunner.class)
public class ConnectionListTests extends ApiTest {

    @Test
    public void testLoadingConnections() throws Exception {
        fakeApi(Connection.REQUEST, "connections.json");

        ConnectionList list = new ConnectionList(new Activity());
        assertEquals(0, list.postToServiceIds().size());

        ConnectionList.Adapter adapter = new ConnectionList.Adapter();
        list.setAdapter(adapter);
        adapter.load(api);

        assertEquals(1, list.postToServiceIds().size());
        int id = list.postToServiceIds().iterator().next();
        assertEquals(41335, id);
    }

    @Test
    public void onlyReloadIfNeeded() throws Exception {
        fakeApi(Connection.REQUEST, "connections.json");

        ConnectionList list = new ConnectionList(new Activity());

        ConnectionList.Adapter adapter = new ConnectionList.Adapter();
        list.setAdapter(adapter);
        adapter.load(api);

        assertEquals(1, list.postToServiceIds().size());

        reset(api);
        fakeApi(Connection.REQUEST, new IOException());

        list.getAdapter().loadIfNecessary(api);
        assertEquals(1, list.postToServiceIds().size());
    }
}
