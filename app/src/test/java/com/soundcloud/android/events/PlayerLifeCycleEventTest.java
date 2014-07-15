package com.soundcloud.android.events;

import static com.soundcloud.android.Expect.expect;
import static junit.framework.Assert.assertEquals;

import org.junit.Test;

public class PlayerLifeCycleEventTest {

    @Test
    public void shouldCreateEventforIdle() throws Exception {
        PlayerLifeCycleEvent event = PlayerLifeCycleEvent.forIdle();
        assertEquals(event.getKind(), 0);
    }

    @Test
    public void shouldCreateEventforDestroyed() throws Exception {
        PlayerLifeCycleEvent event = PlayerLifeCycleEvent.forDestroyed();
        assertEquals(event.getKind(), 1);
    }

    @Test
    public void shouldCreateEventforCreated() throws Exception {
        PlayerLifeCycleEvent event = PlayerLifeCycleEvent.forCreated();
        assertEquals(event.getKind(), 2);
    }

    @Test
    public void serviceIsAliveForIdleEvent() throws Exception {
        expect(PlayerLifeCycleEvent.forIdle().isServiceAlive()).toBeTrue();
    }

    @Test
    public void serviceIsAliveForCreatedEvent() throws Exception {
        expect(PlayerLifeCycleEvent.forCreated().isServiceAlive()).toBeTrue();
    }

    @Test
    public void serviceIsNotAliveForDestroyedEvent() throws Exception {
        expect(PlayerLifeCycleEvent.forDestroyed().isServiceAlive()).toBeFalse();
    }

}
