package com.soundcloud.android.events;

import static com.soundcloud.android.Expect.expect;
import static com.soundcloud.android.events.EventBus2.QueueDescriptor;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import rx.Observer;
import rx.Subscription;
import rx.util.functions.Func1;

@RunWith(SoundCloudTestRunner.class)
public class EventBus2Test {

    private static final QueueDescriptor<String> TEST_QUEUE = QueueDescriptor.create("test_queue_1", String.class);

    private EventBus2 eventBus = new EventBus2();

    @Mock
    private Observer<String> observer1;
    @Mock
    private Observer<String> observer2;

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
        eventBus.<String>queue(TEST_QUEUE).transform().map(new Func1<String, Integer>() {
            @Override
            public Integer call(String s) {
                return Integer.parseInt(s);
            }
        }).subscribe(intObserver);

        eventBus.publish(TEST_QUEUE, "1");

        verify(observer1).onNext("1");
        verify(intObserver).onNext(1);
    }
}
