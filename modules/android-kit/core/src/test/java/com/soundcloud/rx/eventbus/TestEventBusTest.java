package com.soundcloud.rx.eventbus;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import rx.Observer;

@RunWith(MockitoJUnitRunner.class)
public class TestEventBusTest {

    private static final Queue<String> STRING_QUEUE = Queue.of(String.class).get();
    private static final Queue<Integer> INT_QUEUE = Queue.of(Integer.class).get();
    private static final Queue<Number> NUMBER_QUEUE = Queue.of(Number.class).get();

    private TestEventBus eventBus = new TestEventBus();

    @Mock private Observer observer;

    @Test
    public void shouldRecordQueueEventsViaPublish() {
        eventBus.publish(STRING_QUEUE, "one");
        eventBus.publish(STRING_QUEUE, "two");
        eventBus.publish(INT_QUEUE, 1);
        eventBus.publish(Queue.of(Integer.class).get(), 1);

        assertThat(eventBus.eventsOn(STRING_QUEUE)).containsExactly("one", "two");
        assertThat(eventBus.eventsOn(INT_QUEUE)).containsExactly(1);
    }

    @Test
    public void shouldRecordQueueEventsViaQueue() {
        eventBus.queue(STRING_QUEUE).onNext("one");
        eventBus.queue(STRING_QUEUE).onNext("two");
        eventBus.queue(INT_QUEUE).onNext(1);
        eventBus.queue(Queue.of(Integer.class).get()).onNext(1);

        assertThat(eventBus.eventsOn(STRING_QUEUE)).containsExactly("one", "two");
        assertThat(eventBus.eventsOn(INT_QUEUE)).containsExactly(1);
    }

    @Test(expected = AssertionError.class)
    public void shouldFailWhenTryingToAccessFirstEventOnQueueButNeverFired() {
        eventBus.firstEventOn(STRING_QUEUE);
    }

    @Test(expected = AssertionError.class)
    public void shouldFailWhenTryingToAccessLastEventOnQueueButNeverFired() {
        eventBus.lastEventOn(STRING_QUEUE);
    }

    @Test
    public void shouldObtainFirstEventOfGivenType() {
        eventBus.publish(NUMBER_QUEUE, 1);
        eventBus.publish(NUMBER_QUEUE, 2);

        assertThat(eventBus.firstEventOn(NUMBER_QUEUE, Integer.class)).isEqualTo(1);
    }

    @Test(expected = AssertionError.class)
    public void shouldFailWhenObtainingFirstEventOfGivenTypeIsWrongType() {
        eventBus.publish(NUMBER_QUEUE, 1F);

        eventBus.firstEventOn(NUMBER_QUEUE, Integer.class);
    }

    @Test
    public void shouldObtainLastEventOfGivenType() {
        eventBus.publish(NUMBER_QUEUE, 1);
        eventBus.publish(NUMBER_QUEUE, 2);

        assertThat(eventBus.lastEventOn(NUMBER_QUEUE, Integer.class)).isEqualTo(2);
    }

    @Test(expected = AssertionError.class)
    public void shouldFailWhenObtainingLastEventOfGivenTypeIsWrongType() {
        eventBus.publish(NUMBER_QUEUE, 1F);

        eventBus.lastEventOn(NUMBER_QUEUE, Integer.class);
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
