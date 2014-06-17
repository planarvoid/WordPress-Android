package com.soundcloud.android.rx.eventbus;

import static com.soundcloud.android.Expect.expect;

import com.soundcloud.android.rx.eventbus.Queue;
import nl.jqno.equalsverifier.EqualsVerifier;
import org.junit.Test;

public class QueueTest {

    @Test
    public void verifyEqualsAndHashCodeContract() {
        EqualsVerifier.forClass(Queue.class).verify();
    }

    @Test
    public void shouldBuildDefaultQueue() {
        final Queue<String> stringQueue = Queue.of(String.class).get();
        expect(stringQueue.name).toEqual("StringQueue");
        expect(stringQueue.replayLast).toBeFalse();
        expect(stringQueue.defaultEvent).toBeNull();
    }

    @Test
    public void shouldBuildNamedQueue() {
        final Queue<String> stringQueue = Queue.of(String.class).name("custom").get();
        expect(stringQueue.name).toEqual("custom");
        expect(stringQueue.replayLast).toBeFalse();
        expect(stringQueue.defaultEvent).toBeNull();
    }

    @Test
    public void shouldBuildReplayQueue() {
        final Queue<String> stringQueue = Queue.of(String.class).replay().get();
        expect(stringQueue.replayLast).toBeTrue();
    }

    @Test
    public void shouldBuildReplayQueueWithDefaultEvent() {
        final Queue<String> stringQueue = Queue.of(String.class).replay("def").get();
        expect(stringQueue.replayLast).toBeTrue();
        expect(stringQueue.defaultEvent).toEqual("def");
    }
}