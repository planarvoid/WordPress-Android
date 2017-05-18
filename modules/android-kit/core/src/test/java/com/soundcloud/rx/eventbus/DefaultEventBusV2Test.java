package com.soundcloud.rx.eventbus;

import static org.assertj.core.api.Assertions.assertThat;

import io.reactivex.functions.Function;
import io.reactivex.observers.TestObserver;
import io.reactivex.schedulers.Schedulers;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class DefaultEventBusV2Test {

    private static final Queue<String> TEST_DEFAULT_QUEUE = Queue.of(String.class).get();
    private static final Queue<String> TEST_REPLAY_QUEUE = Queue.of(String.class).replay().get();
    private static final Queue<String> TEST_REPLAY_QUEUE_WITH_DEFAULT = Queue.of(String.class).replay("first!").get();

    private DefaultEventBusV2 eventBus = new DefaultEventBusV2(Schedulers.trampoline());

    private TestObserver<String> observer1;
    private TestObserver<String> observer2;

    @Before
    public void setUp() throws Exception {
        observer1 = new TestObserver<>();
        observer2 = new TestObserver<>();
    }

    @Test
    public void shouldLazilyCreateEventQueuesWhenFirstAccessingThem() {
        assertThat(eventBus.queue(TEST_DEFAULT_QUEUE)).isNotNull();
    }

    @Test
    public void shouldPublishEventsToSubscribers() {
        eventBus.subscribe(TEST_DEFAULT_QUEUE, observer1);
        eventBus.subscribe(TEST_DEFAULT_QUEUE, observer2);
        eventBus.publish(TEST_DEFAULT_QUEUE, "hello!");

        observer1.assertValue("hello!");
        observer2.assertValue("hello!");
    }

    @Test
    public void shouldNotPublishEventsToSubscribersAfterUnsubscribing() {
        eventBus.subscribe(TEST_DEFAULT_QUEUE, observer1);
        eventBus.subscribe(TEST_DEFAULT_QUEUE, observer2);

        eventBus.publish(TEST_DEFAULT_QUEUE, "hello!");
        observer1.dispose();

        eventBus.publish(TEST_DEFAULT_QUEUE, "world!");

        observer1.assertValue("hello!")
                 .assertValueCount(1);
        observer2.assertValues("hello!", "world!")
                 .assertValueCount(2);
    }

    @Test
    public void shouldAllowEventDataTransformation() {
        eventBus.subscribe(TEST_DEFAULT_QUEUE, observer1);
        TestObserver<Object> testObserver = eventBus.queue(TEST_DEFAULT_QUEUE).map(new Function<String, Object>() {
            @Override
            public Integer apply(String s) {
                return Integer.parseInt(s);
            }
        }).test();

        eventBus.publish(TEST_DEFAULT_QUEUE, "1");

        observer1.assertValue("1");
        testObserver.assertValue(1);
    }

    @Test
    public void shouldEmitDefaultEventOnConnectionToReplayQueue() {
        eventBus.subscribe(TEST_REPLAY_QUEUE_WITH_DEFAULT, observer1);
        observer1.assertValue("first!");
    }

    @Test
    public void shouldEmitLastValueOnConnectionToReplayQueue() throws Exception {
        eventBus.publish(TEST_REPLAY_QUEUE, "1");
        eventBus.publish(TEST_REPLAY_QUEUE, "2");
        eventBus.subscribe(TEST_REPLAY_QUEUE, observer1);
        observer1.assertValue("2");
    }

    @Test
    public void shouldEmitSubsequentValuesWhenConnectedToReplayQueue() throws Exception {
        eventBus.publish(TEST_REPLAY_QUEUE, "1");
        eventBus.subscribe(TEST_REPLAY_QUEUE, observer1);
        eventBus.publish(TEST_REPLAY_QUEUE, "2");
        observer1.assertValues("1", "2");
    }

    @Test
    public void shouldConstructPublishAction0() throws Exception {
        eventBus.subscribe(TEST_DEFAULT_QUEUE, observer1);
        eventBus.publishAction0(TEST_DEFAULT_QUEUE, "1").run();
        observer1.assertValue("1");
    }

    @Test
    public void shouldConstructPublishAction1() throws Exception {
        eventBus.subscribe(TEST_DEFAULT_QUEUE, observer1);
        eventBus.publishAction1(TEST_DEFAULT_QUEUE, "1").accept(new Object());
        observer1.assertValue("1");
    }
}
