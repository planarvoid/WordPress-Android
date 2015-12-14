package com.soundcloud.android.events;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Test;

public class PlayerUIEventTest {

    @Test
    public void createsEventFromPlayerExpanded() {
        PlayerUIEvent event = PlayerUIEvent.fromPlayerExpanded();
        assertThat(event.getKind()).isEqualTo(0);
    }

    @Test
    public void createsEventFromPlayerCollapsed() {
        PlayerUIEvent event = PlayerUIEvent.fromPlayerCollapsed();
        assertThat(event.getKind()).isEqualTo(1);
    }

}
