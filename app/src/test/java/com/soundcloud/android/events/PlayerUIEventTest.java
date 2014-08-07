package com.soundcloud.android.events;

import static com.soundcloud.android.Expect.expect;

import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(SoundCloudTestRunner.class)
public class PlayerUIEventTest {

    @Test
    public void createsEventFromPlayerExpanding() {
        PlayerUIEvent event = PlayerUIEvent.fromPlayerExpanding();
        expect(event.getKind()).toEqual(0);
    }

    @Test
    public void createsEventFromPlayerCollapsing() {
        PlayerUIEvent event = PlayerUIEvent.fromPlayerCollapsing();
        expect(event.getKind()).toEqual(1);
    }

    @Test
    public void createsEventFromPlayerCollapsed() {
        PlayerUIEvent event = PlayerUIEvent.fromPlayerCollapsed();
        expect(event.getKind()).toEqual(5);
    }

    @Test
    public void createEventForExpandPlayer() {
        PlayerUIEvent event = PlayerUIEvent.forExpandPlayer();
        expect(event.getKind()).toEqual(2);
    }

    @Test
    public void createsEventForClosePlayer() {
        PlayerUIEvent event = PlayerUIEvent.forCollapsePlayer();
        expect(event.getKind()).toEqual(3);
    }

    @Test
    public void createsEventForShowPlayer() {
        PlayerUIEvent event = PlayerUIEvent.forShowPlayer();
        expect(event.getKind()).toEqual(4);
    }

    @Test
    public void createsEventForUnskippablePlayer() {
        PlayerUIEvent event = PlayerUIEvent.forUnskippablePlayer();
        expect(event.getKind()).toEqual(6);
    }

}