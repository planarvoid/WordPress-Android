package com.soundcloud.android.events;

import static org.assertj.core.api.Assertions.assertThat;

import com.soundcloud.android.model.Urn;
import org.junit.Test;

public class PlayQueueEventTest {

    @Test
    public void shouldCreateEventForNewQueue() {
        PlayQueueEvent event = PlayQueueEvent.fromNewQueue(Urn.NOT_SET);
        assertThat(event.getKind()).isEqualTo(0);
    }

    @Test
    public void shouldCreateEventForQueueUpdate() {
        PlayQueueEvent event = PlayQueueEvent.fromQueueUpdate(Urn.NOT_SET);
        assertThat(event.getKind()).isEqualTo(1);
    }

    @Test
    public void shouldCreateEventForAudioAdRemoved() {
        PlayQueueEvent event = PlayQueueEvent.fromAudioAdRemoved(Urn.NOT_SET);
        assertThat(event.getKind()).isEqualTo(2);
    }

}
