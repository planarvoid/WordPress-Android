package com.soundcloud.android.analytics;

import static org.assertj.core.api.Java6Assertions.assertThat;

import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.ScreenEvent;
import com.soundcloud.android.events.UIEvent;
import com.soundcloud.android.main.Screen;
import com.soundcloud.rx.eventbus.TestEventBus;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class ScreenProviderTest {
    private ScreenProvider screenProvider;
    private TestEventBus eventBus;

    @Before
    public void setUp() throws Exception {
        eventBus = new TestEventBus();
        screenProvider = new ScreenProvider(eventBus);
        screenProvider.subscribe();
    }

    @Test
    public void returnsScreenFromLastScreenEvent() throws Exception {
        eventBus.publish(EventQueue.TRACKING, ScreenEvent.create(Screen.DISCOVER.get("postfix")));
        assertThat(screenProvider.getLastScreenTag()).isEqualTo("discovery:main:postfix");
    }

    @Test
    public void ignoresNonScreenEvent() throws Exception {
        eventBus.publish(EventQueue.TRACKING, ScreenEvent.create(Screen.DISCOVER.get("postfix")));
        eventBus.publish(EventQueue.TRACKING, UIEvent.fromPlayerClickOpen(false));
        assertThat(screenProvider.getLastScreenTag()).isEqualTo("discovery:main:postfix");
    }
}
