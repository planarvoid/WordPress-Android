package com.soundcloud.android.events;

import static com.soundcloud.android.Expect.expect;
import static junit.framework.Assert.assertEquals;

import org.junit.Test;

public class PlayerLifeCycleEventTest {

    @Test
    public void shouldCreateEventforStopped() throws Exception {
        PlayerLifeCycleEvent event = PlayerLifeCycleEvent.forStopped();
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
    public void serviceIsRunningForCreatedEvent() throws Exception {
        expect(PlayerLifeCycleEvent.forCreated().isServiceRunning()).toBeTrue();
    }

    @Test
    public void serviceIsRunningForStartedEvent() throws Exception {
        expect(PlayerLifeCycleEvent.forCreated().isServiceRunning()).toBeTrue();
    }

    @Test
    public void serviceIsNotRunningForStoppedEvent() throws Exception {
        expect(PlayerLifeCycleEvent.forStopped().isServiceRunning()).toBeFalse();
    }

    @Test
    public void serviceIsNotRunningForDestroyedEvent() throws Exception {
        expect(PlayerLifeCycleEvent.forDestroyed().isServiceRunning()).toBeFalse();
    }

}
