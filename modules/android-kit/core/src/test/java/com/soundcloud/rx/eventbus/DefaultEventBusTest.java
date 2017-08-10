package com.soundcloud.rx.eventbus;

import static org.assertj.core.api.Java6Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import rx.Observer;
import rx.Subscription;
import rx.functions.Func1;
import rx.schedulers.Schedulers;

@RunWith(MockitoJUnitRunner.class)
public class DefaultEventBusTest {

    private static final Queue<String> TEST_DEFAULT_QUEUE = Queue.of(String.class).get();
    private static final Queue<String> TEST_REPLAY_QUEUE = Queue.of(String.class).replay().get();
    private static final Queue<String> TEST_REPLAY_QUEUE_WITH_DEFAULT = Queue.of(String.class).replay("first!").get();

    private DefaultEventBus eventBus = new DefaultEventBus(Schedulers.immediate());

    @Mock private Observer<String> observer1;
    @Mock private Observer<String> observer2;

    @Test
    public void shouldLazilyCreateEventQueuesWhenFirstAccessingThem() {
        assertThat(eventBus.queue(TEST_DEFAULT_QUEUE)).isNotNull();
    }

    @Test
    public void shouldPublishEventsToSubscribers() {
        eventBus.subscribe(TEST_DEFAULT_QUEUE, observer1);
        eventBus.subscribe(TEST_DEFAULT_QUEUE, observer2);
        eventBus.publish(TEST_DEFAULT_QUEUE, "hello!");

        verify(observer1).onNext("hello!");
        verify(observer2).onNext("hello!");
    }

    @Test
    public void shouldNotPublishEventsToSubscribersAfterUnsubscribing() {
        final Subscription subscription = eventBus.subscribe(TEST_DEFAULT_QUEUE, observer1);
        eventBus.subscribe(TEST_DEFAULT_QUEUE, observer2);

        eventBus.publish(TEST_DEFAULT_QUEUE, "hello!");
        subscription.unsubscribe();

        eventBus.publish(TEST_DEFAULT_QUEUE, "world!");

        verify(observer1).onNext("hello!");
        verifyNoMoreInteractions(observer1);
        verify(observer2).onNext("hello!");
        verify(observer2).onNext("world!");
    }

    @Test
    public void shouldAllowEventDataTransformation() {
        Observer<Integer> intObserver = mock(Observer.class);

        eventBus.subscribe(TEST_DEFAULT_QUEUE, observer1);
        eventBus.queue(TEST_DEFAULT_QUEUE).map(new Func1<String, Integer>() {
            @Override
            public Integer call(String s) {
                return Integer.parseInt(s);
            }
        }).subscribe(intObserver);

        eventBus.publish(TEST_DEFAULT_QUEUE, "1");

        verify(observer1).onNext("1");
        verify(intObserver).onNext(1);
    }

    @Test
    public void shouldEmitDefaultEventOnConnectionToReplayQueue() {
        eventBus.subscribe(TEST_REPLAY_QUEUE_WITH_DEFAULT, observer1);
        verify(observer1).onNext("first!");
        verifyNoMoreInteractions(observer1);
    }

    @Test
    public void shouldEmitLastValueOnConnectionToReplayQueue() throws Exception {
        eventBus.publish(TEST_REPLAY_QUEUE, "1");
        eventBus.publish(TEST_REPLAY_QUEUE, "2");
        eventBus.subscribe(TEST_REPLAY_QUEUE, observer1);
        verify(observer1).onNext("2");
        verifyNoMoreInteractions(observer1);
    }

    @Test
    public void shouldEmitSubsequentValuesWhenConnectedToReplayQueue() throws Exception {
        eventBus.publish(TEST_REPLAY_QUEUE, "1");
        eventBus.subscribe(TEST_REPLAY_QUEUE, observer1);
        eventBus.publish(TEST_REPLAY_QUEUE, "2");
        verify(observer1).onNext("2");
    }

    @Test
    public void shouldConstructPublishAction0() {
        eventBus.subscribe(TEST_DEFAULT_QUEUE, observer1);
        eventBus.publishAction0(TEST_DEFAULT_QUEUE, "1").call();
        verify(observer1).onNext("1");
    }

    @Test
    public void shouldConstructPublishAction1() {
        eventBus.subscribe(TEST_DEFAULT_QUEUE, observer1);
        eventBus.publishAction1(TEST_DEFAULT_QUEUE, "1").call(new Object());
        verify(observer1).onNext("1");
    }
}
