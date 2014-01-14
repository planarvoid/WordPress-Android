package com.soundcloud.android.events;

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

}
