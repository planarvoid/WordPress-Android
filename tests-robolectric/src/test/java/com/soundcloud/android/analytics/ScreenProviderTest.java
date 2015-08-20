package com.soundcloud.android.analytics;

import static com.soundcloud.android.Expect.expect;

import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.ScreenEvent;
import com.soundcloud.android.events.UIEvent;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.rx.eventbus.TestEventBus;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(SoundCloudTestRunner.class)
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
        eventBus.publish(EventQueue.TRACKING, ScreenEvent.create(Screen.EXPLORE_TRENDING_MUSIC.get("postfix")));
        expect(screenProvider.getLastScreenTag()).toEqual("explore:trending_music:postfix");
    }

    @Test
    public void ignoresNonScreenEvent() throws Exception {
        eventBus.publish(EventQueue.TRACKING, ScreenEvent.create(Screen.EXPLORE_TRENDING_MUSIC.get("postfix")));
        eventBus.publish(EventQueue.TRACKING, UIEvent.fromExploreNav());
        expect(screenProvider.getLastScreenTag()).toEqual("explore:trending_music:postfix");
    }
}
