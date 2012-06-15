package com.soundcloud.android.view.create;


import static org.junit.Assert.assertEquals;

import com.soundcloud.android.cache.Connections;
import com.soundcloud.android.robolectric.DefaultTestRunner;
import com.soundcloud.android.robolectric.TestHelper;
import com.soundcloud.api.Endpoints;
import com.xtremelabs.robolectric.annotation.DisableStrictI18n;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import android.app.Activity;

@RunWith(DefaultTestRunner.class)
public class ConnectionListTest {

    @Before
    public void before() {
        Connections.set(null);
    }

    @Test @DisableStrictI18n
    public void shouldLoadConnectionsFromApi() throws Exception {
        TestHelper.addCannedResponse(ConnectionListTest.class, Endpoints.MY_CONNECTIONS, "connections.json");

        ConnectionList list = new ConnectionList(new Activity());
        assertEquals(0, list.postToServiceIds().size());

        ConnectionList.Adapter adapter = new ConnectionList.Adapter(DefaultTestRunner.application);
        list.setAdapter(adapter);
        adapter.load();

        assertEquals(1, list.postToServiceIds().size());
        int id = list.postToServiceIds().iterator().next();
        assertEquals(41335, id);
    }

    @Test @DisableStrictI18n
    public void shouldOnlyReloadIfNeeded() throws Exception {
        TestHelper.addCannedResponse(ConnectionListTest.class, Endpoints.MY_CONNECTIONS, "connections.json");

        ConnectionList list = new ConnectionList(new Activity());

        ConnectionList.Adapter adapter = new ConnectionList.Adapter(DefaultTestRunner.application);
        list.setAdapter(adapter);
        adapter.load();

        assertEquals(1, list.postToServiceIds().size());

        TestHelper.addPendingIOException(Endpoints.MY_CONNECTIONS);

        list.getAdapter().loadIfNecessary();
        assertEquals(1, list.postToServiceIds().size());
    }
}
