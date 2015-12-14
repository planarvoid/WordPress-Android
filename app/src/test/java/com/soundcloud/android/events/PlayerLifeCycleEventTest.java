package com.soundcloud.android.events;

import static junit.framework.Assert.assertEquals;
import static org.assertj.core.api.Assertions.assertThat;

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
        assertThat(PlayerLifeCycleEvent.forCreated().isServiceRunning()).isTrue();
    }

    @Test
    public void serviceIsRunningForStartedEvent() throws Exception {
        assertThat(PlayerLifeCycleEvent.forCreated().isServiceRunning()).isTrue();
    }

    @Test
    public void serviceIsNotRunningForStoppedEvent() throws Exception {
        assertThat(PlayerLifeCycleEvent.forStopped().isServiceRunning()).isFalse();
    }

    @Test
    public void serviceIsNotRunningForDestroyedEvent() throws Exception {
        assertThat(PlayerLifeCycleEvent.forDestroyed().isServiceRunning()).isFalse();
    }

}
