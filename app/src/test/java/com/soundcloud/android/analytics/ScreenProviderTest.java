package com.soundcloud.android.analytics;

import static org.assertj.core.api.Assertions.assertThat;

import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.ScreenEvent;
import com.soundcloud.android.events.UIEvent;
import com.soundcloud.android.main.Screen;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.rx.eventbus.TestEventBus;
import org.junit.Before;
import org.junit.Test;

public class ScreenProviderTest extends AndroidUnitTest {
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
        eventBus.publish(EventQueue.TRACKING, ScreenEvent.create(Screen.MUSIC_GENRES.get("postfix")));
        assertThat(screenProvider.getLastScreenTag()).isEqualTo("charts:music_genres:postfix");
    }

    @Test
    public void ignoresNonScreenEvent() throws Exception {
        eventBus.publish(EventQueue.TRACKING, ScreenEvent.create(Screen.MUSIC_GENRES.get("postfix")));
        eventBus.publish(EventQueue.TRACKING, UIEvent.fromPlayerOpen(false));
        assertThat(screenProvider.getLastScreenTag()).isEqualTo("charts:music_genres:postfix");
    }
}
