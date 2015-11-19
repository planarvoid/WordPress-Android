package com.soundcloud.android.utils;


import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.soundcloud.android.events.ConnectionType;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.rx.eventbus.TestEventBus;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import android.content.Intent;
import android.net.ConnectivityManager;

public class NetworkConnectivityListenerTest extends AndroidUnitTest {

    @Mock NetworkConnectionHelper networkConnectionHelper;
    private TestEventBus eventBus = new TestEventBus();

    private NetworkConnectivityListener listener;

    @Before
    public void before() {
        listener = new NetworkConnectivityListener(context(), networkConnectionHelper, eventBus);
        listener.startListening();
    }

    @Test
    public void shouldReactToConnectivityBroadcasts() {
        when(networkConnectionHelper.getCurrentConnectionType()).thenReturn(ConnectionType.FOUR_G, ConnectionType.TWO_G);

        context().sendBroadcast(new Intent(ConnectivityManager.CONNECTIVITY_ACTION));

        assertThat(eventBus.eventsOn(EventQueue.NETWORK_CONNECTION_CHANGED)).containsExactly(ConnectionType.UNKNOWN, ConnectionType.FOUR_G);

        context().sendBroadcast(new Intent(ConnectivityManager.CONNECTIVITY_ACTION));

        assertThat(eventBus.eventsOn(EventQueue.NETWORK_CONNECTION_CHANGED)).containsExactly(ConnectionType.UNKNOWN, ConnectionType.FOUR_G, ConnectionType.TWO_G);

        listener.stopListening();
    }

    @Test
    public void shouldNotReactToConnectivityBroadcastsAfterStopListening() {
        when(networkConnectionHelper.getCurrentConnectionType()).thenReturn(ConnectionType.FOUR_G, ConnectionType.TWO_G);

        context().sendBroadcast(new Intent(ConnectivityManager.CONNECTIVITY_ACTION));

        assertThat(eventBus.eventsOn(EventQueue.NETWORK_CONNECTION_CHANGED)).containsExactly(ConnectionType.UNKNOWN, ConnectionType.FOUR_G);

        listener.stopListening();
        context().sendBroadcast(new Intent(ConnectivityManager.CONNECTIVITY_ACTION));

        assertThat(eventBus.eventsOn(EventQueue.NETWORK_CONNECTION_CHANGED)).containsExactly(ConnectionType.UNKNOWN, ConnectionType.FOUR_G);
    }
}
