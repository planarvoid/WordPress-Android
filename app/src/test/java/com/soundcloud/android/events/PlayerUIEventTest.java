package com.soundcloud.android.events;

import static com.soundcloud.android.Expect.expect;

import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(SoundCloudTestRunner.class)
public class PlayerUIEventTest {

    @Test
    public void shouldCreateEventFromPlayerExpanded() {
        PlayerUIEvent event = PlayerUIEvent.fromPlayerExpanded();
        expect(event.getKind()).toEqual(0);
    }

    @Test
    public void shouldCreateEventFromPlayerCollapsed() {
        PlayerUIEvent event = PlayerUIEvent.fromPlayerCollapsed();
        expect(event.getKind()).toEqual(1);
    }

    @Test
    public void shouldCreateEventForPlayTriggered() {
        PlayerUIEvent event = PlayerUIEvent.forPlayTriggered();
        expect(event.getKind()).toEqual(2);
    }

}