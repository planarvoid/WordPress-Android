package com.soundcloud.android.events;

import static com.soundcloud.android.Expect.expect;

import nl.jqno.equalsverifier.EqualsVerifier;
import org.junit.Test;

public class QueueTest {

    @Test
    public void verifyEqualsAndHashCodeContract() {
        EqualsVerifier.forClass(Queue.class).verify();
    }

    @Test
    public void shouldDeriveQueueNameWhenEventTypeIsCustomType() {
        Queue queue = Queue.create(Object.class);
        expect(queue.name).toEqual("ObjectQueue");
    }

    @Test
    public void shouldCreateQueueWithDefaultEvent() {
        Queue queue = Queue.create(Integer.class, Integer.MAX_VALUE);
        expect(queue.defaultEvent).toEqual(Integer.MAX_VALUE);
    }
}