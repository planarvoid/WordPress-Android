package com.soundcloud.android.events;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Test;

public class PlayerUICommandTest {

    @Test
    public void createEventForShowPlayer() {
        PlayerUICommand event = PlayerUICommand.showPlayer();
        assertThat(event.isShow()).isTrue();
    }

    @Test
    public void createEventForExpandPlayer() {
        PlayerUICommand event = PlayerUICommand.expandPlayer();
        assertThat(event.isExpand()).isTrue();
    }

    @Test
    public void createsEventForClosePlayer() {
        PlayerUICommand event = PlayerUICommand.collapsePlayer();
        assertThat(event.isCollapse()).isTrue();
    }

}
