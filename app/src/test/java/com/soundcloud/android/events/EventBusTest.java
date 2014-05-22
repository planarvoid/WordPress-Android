package com.soundcloud.android.events;

import static com.soundcloud.android.Expect.expect;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import rx.Observer;
import rx.Subscription;
import rx.functions.Func1;

@RunWith(SoundCloudTestRunner.class)
public class EventBusTest {

    private static final EventBus.Queue<String> TEST_QUEUE = EventBus.Queue.create("test_queue_1", String.class);
    private static final EventBus.Queue<String> TEST_BEHAVIOR_QUEUE =
            EventBus.Queue.create("test_behavior_queue_1", String.class, "default-item");

    private EventBus eventBus = new EventBus();

    @Mock
    private Observer<String> observer1;
    @Mock
    private Observer<String> observer2;

    @Test
    public void shouldCreateUniqueQueueDescriptorsBasedOnQueueName() {
        EventBus.Queue qd1 = EventBus.Queue.create("one", String.class);
        EventBus.Queue qd2 = EventBus.Queue.create("one", String.class);
        EventBus.Queue qd3 = EventBus.Queue.create("two", String.class);

        expect(qd1).toEqual(qd2);
        expect(qd1.hashCode()).toEqual(qd2.hashCode());
        expect(qd1.hashCode()).not.toEqual(qd3.hashCode());
        expect(qd1).not.toEqual(qd3);
    }

    @Test
    public void shouldDeriveQueueNameWhenEventTypeIsCustomType() {
        EventBus.Queue qd = EventBus.Queue.create(Object.class);
        expect(qd.name).toEqual("Object");
    }

    @Test
    public void shouldLazilyCreateEventQueuesWhenFirstAccessingThem() {
        expect(eventBus.queue(TEST_QUEUE)).not.toBeNull();
    }

    @Test
    public void shouldPublishEventsToSubscribers() {
        eventBus.subscribe(TEST_QUEUE, observer1);
        eventBus.subscribe(TEST_QUEUE, observer2);
        eventBus.publish(TEST_QUEUE, "hello!");

        verify(observer1).onNext("hello!");
        verify(observer2).onNext("hello!");
    }

    @Test
    public void shouldNotPublishEventsToSubscribersAfterUnsubscribing() {
        final Subscription subscription = eventBus.subscribe(TEST_QUEUE, observer1);
        eventBus.subscribe(TEST_QUEUE, observer2);

        eventBus.publish(TEST_QUEUE, "hello!");
        subscription.unsubscribe();

        eventBus.publish(TEST_QUEUE, "world!");

        verify(observer1).onNext("hello!");
        verifyNoMoreInteractions(observer1);
        verify(observer2).onNext("hello!");
        verify(observer2).onNext("world!");
    }

    @Test
    public void shouldAllowEventDataTransformation() {
        Observer<Integer> intObserver = mock(Observer.class);

        eventBus.subscribe(TEST_QUEUE, observer1);
        eventBus.queue(TEST_QUEUE).map(new Func1<String, Integer>() {
            @Override
            public Integer call(String s) {
                return Integer.parseInt(s);
            }
        }).subscribe(intObserver);

        eventBus.publish(TEST_QUEUE, "1");

        verify(observer1).onNext("1");
        verify(intObserver).onNext(1);
    }

    @Test
    public void shouldPublishDefaultValueOnConnectionToBehaviorQueueWithNoEvents() throws Exception {
        eventBus.subscribe(TEST_BEHAVIOR_QUEUE, observer1);
        verify(observer1).onNext("default-item");
    }

    @Test
    public void shouldEmitLastValueOnConnectionToBehaviorQueue() throws Exception {
        eventBus.publish(TEST_BEHAVIOR_QUEUE, "1");
        eventBus.publish(TEST_BEHAVIOR_QUEUE, "2");
        eventBus.subscribe(TEST_BEHAVIOR_QUEUE, observer1);
        verify(observer1).onNext("2");
        verifyNoMoreInteractions(observer1);
    }

    @Test
    public void shouldEmitSubsequeneValuesWhenConnectedToBehaviorQueue() throws Exception {
        eventBus.publish(TEST_BEHAVIOR_QUEUE, "1");
        eventBus.subscribe(TEST_BEHAVIOR_QUEUE, observer1);
        eventBus.publish(TEST_BEHAVIOR_QUEUE, "2");
        verify(observer1).onNext("2");
    }
}
