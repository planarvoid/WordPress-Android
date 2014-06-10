package com.soundcloud.android.robolectric;

import static com.pivotallabs.greatexpectations.Expect.expect;
import static org.junit.Assert.fail;

import com.soundcloud.android.events.Queue;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import rx.Observer;

@RunWith(SoundCloudTestRunner.class)
public class TestEventBusTest {

    private static final Queue<String> STRING_QUEUE = Queue.create("str-queue", String.class);
    private static final Queue<Integer> INT_QUEUE = Queue.create("int-queue", Integer.class);

    private TestEventBus eventBus = new TestEventBus();

    @Mock
    private Observer observer;

    @Test
    public void shouldRecordQueueEventsViaPublish() {
        eventBus.publish(STRING_QUEUE, "one");
        eventBus.publish(STRING_QUEUE, "two");
        eventBus.publish(INT_QUEUE, 1);
        eventBus.publish(Queue.create(Integer.class), 1);

        expect(eventBus.eventsOn(STRING_QUEUE)).toContainExactly("one", "two");
        expect(eventBus.eventsOn(INT_QUEUE)).toContainExactly(1);
    }

    @Test
    public void shouldRecordQueueEventsViaQueue() {
        eventBus.queue(STRING_QUEUE).onNext("one");
        eventBus.queue(STRING_QUEUE).onNext("two");
        eventBus.queue(INT_QUEUE).onNext(1);
        eventBus.queue(Queue.create(Integer.class)).onNext(1);

        expect(eventBus.eventsOn(STRING_QUEUE)).toContainExactly("one", "two");
        expect(eventBus.eventsOn(INT_QUEUE)).toContainExactly(1);
    }

    @Test(expected = AssertionError.class)
    public void shouldFailWhenNotExpectingEventOnQueueButEventFired() {
        eventBus.publish(STRING_QUEUE, "one");
        eventBus.verifyNoEventsOn(STRING_QUEUE);
    }

    @Test
    public void shouldPassWhenNotExpectingEventOnQueueAndDidNotFire() {
        try {
            eventBus.verifyNoEventsOn(STRING_QUEUE);
        } catch (AssertionError e) {
            fail();
        }
    }

    @Test(expected = AssertionError.class)
    public void shouldFailWhenExpectingToBeUnsubscribedFromQueueWasNeverSubscribed() {
        eventBus.verifyUnsubscribed(STRING_QUEUE);
    }

    @Test(expected = AssertionError.class)
    public void shouldFailWhenExpectingToBeUnsubscribedFromQueueButIsNot() {
        eventBus.subscribe(STRING_QUEUE, observer);
        eventBus.verifyUnsubscribed(STRING_QUEUE);
    }

    @Test
    public void shouldPassWhenExpectingToBeUnsubscribedFromQueueAndIsYesIndeed() {
        eventBus.subscribe(STRING_QUEUE, observer).unsubscribe();
        try {
            eventBus.verifyUnsubscribed(STRING_QUEUE);
        } catch (AssertionError e) {
            fail();
        }
    }

    @Test(expected = AssertionError.class)
    public void shouldFailWhenExpectingToBeUnsubscribedFromAllQueuesButWasNeverSubscribed() {
        eventBus.verifyUnsubscribed();
    }

    @Test(expected = AssertionError.class)
    public void shouldFailWhenExpectingToBeUnsubscribedFromAllQueuesButIsNot() {
        eventBus.subscribe(STRING_QUEUE, observer).unsubscribe();
        eventBus.subscribe(INT_QUEUE, observer);
        eventBus.verifyUnsubscribed();
    }

    @Test
    public void shouldPassWhenExpectingToBeUnsubscribedFromAllQueuesAndIsYesIndeedYep() {
        eventBus.subscribe(STRING_QUEUE, observer).unsubscribe();
        eventBus.subscribe(INT_QUEUE, observer).unsubscribe();
        try {
            eventBus.verifyUnsubscribed(STRING_QUEUE);
        } catch (AssertionError e) {
            fail();
        }
    }
}