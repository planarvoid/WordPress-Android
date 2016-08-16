package com.soundcloud.android.events;

import static org.assertj.core.api.Assertions.assertThat;

import com.soundcloud.android.model.Urn;
import org.junit.Test;

public class PlayQueueEventTest {

    @Test
    public void shouldCreateEventForNewQueue() {
        PlayQueueEvent event = PlayQueueEvent.fromNewQueue(Urn.NOT_SET);
        assertThat(event.isNewQueue()).isTrue();
    }

    @Test
    public void shouldCreateEventForQueueUpdate() {
        PlayQueueEvent event = PlayQueueEvent.fromQueueUpdate(Urn.NOT_SET);
        assertThat(event.isQueueUpdate()).isTrue();
        assertThat(event.itemMoved()).isFalse();
        assertThat(event.adsRemoved()).isFalse();
    }

    @Test
    public void shouldCreateEventForAdsRemoved() {
        PlayQueueEvent event = PlayQueueEvent.fromAdsRemoved(Urn.NOT_SET);
        assertThat(event.adsRemoved()).isTrue();
    }

    @Test
    public void shouldCreateEventForUpdateAndMoved() {
        PlayQueueEvent playQueueEvent = PlayQueueEvent.fromQueueUpdateMoved(Urn.NOT_SET);
        assertThat(playQueueEvent.itemMoved()).isTrue();
        assertThat(playQueueEvent.isQueueUpdate()).isTrue();
        assertThat(playQueueEvent.itemRemoved()).isFalse();
    }

    @Test
    public void shouldCreateEventForUpdateAndReMoved() {
        PlayQueueEvent playQueueEvent = PlayQueueEvent.fromQueueUpdateRemoved(Urn.NOT_SET);
        assertThat(playQueueEvent.itemRemoved()).isTrue();
        assertThat(playQueueEvent.itemMoved()).isFalse();
        assertThat(playQueueEvent.isQueueUpdate()).isTrue();
    }

}
