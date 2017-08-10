package com.soundcloud.rx.eventbus;

import static org.assertj.core.api.Java6Assertions.assertThat;

import nl.jqno.equalsverifier.EqualsVerifier;
import org.junit.Ignore;
import org.junit.Test;

public class QueueTest {

    @Ignore
    @Test
    public void verifyEqualsAndHashCodeContract() {
        EqualsVerifier.forClass(Queue.class).verify();
    }

    @Test
    public void shouldBuildDefaultQueue() {
        final Queue<String> stringQueue = Queue.of(String.class).get();
        assertThat(stringQueue.name).isEqualTo("StringQueue");
        assertThat(stringQueue.replayLast).isFalse();
        assertThat(stringQueue.defaultEvent).isNull();
    }

    @Test
    public void shouldBuildNamedQueue() {
        final Queue<String> stringQueue = Queue.of(String.class).name("custom").get();
        assertThat(stringQueue.name).isEqualTo("custom");
        assertThat(stringQueue.replayLast).isFalse();
        assertThat(stringQueue.defaultEvent).isNull();
    }

    @Test
    public void shouldBuildReplayQueue() {
        final Queue<String> stringQueue = Queue.of(String.class).replay().get();
        assertThat(stringQueue.replayLast).isTrue();
    }

    @Test
    public void shouldBuildReplayQueueWithDefaultEvent() {
        final Queue<String> stringQueue = Queue.of(String.class).replay("def").get();
        assertThat(stringQueue.replayLast).isTrue();
        assertThat(stringQueue.defaultEvent).isEqualTo("def");
    }
}
