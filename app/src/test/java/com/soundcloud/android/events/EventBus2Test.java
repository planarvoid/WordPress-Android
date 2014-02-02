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

    private static final QueueDescriptor<String> TEST_QUEUE_1 = QueueDescriptor.create("test_queue_1");
    private static final QueueDescriptor<String> TEST_QUEUE_2 = QueueDescriptor.create("test_queue_2");

    private EventBus2 eventBus = new EventBus2();

    @Mock
    private Observer<String> observer1;
    @Mock
    private Observer<String> observer2;

    @Test
    public void shouldRegisterNewEventQueues() {
        eventBus.registerQueue(TEST_QUEUE_1);
        eventBus.registerQueue(TEST_QUEUE_2);
        expect(eventBus.queue(TEST_QUEUE_1)).not.toBeNull();
        expect(eventBus.queue(TEST_QUEUE_2)).not.toBeNull();
    }

    @Test
    public void shouldPublishEventsToSubscribers() {
        eventBus.registerQueue(TEST_QUEUE_1);

        eventBus.subscribe(TEST_QUEUE_1, observer1);
        eventBus.subscribe(TEST_QUEUE_1, observer2);
        eventBus.publish(TEST_QUEUE_1, "hello!");

        verify(observer1).onNext("hello!");
        verify(observer2).onNext("hello!");
    }

    @Test
    public void shouldNotPublishEventsToSubscribersAfterUnsubscribing() {
        eventBus.registerQueue(TEST_QUEUE_1);

        final Subscription subscription = eventBus.subscribe(TEST_QUEUE_1, observer1);
        eventBus.subscribe(TEST_QUEUE_1, observer2);

        eventBus.publish(TEST_QUEUE_1, "hello!");
        subscription.unsubscribe();

        eventBus.publish(TEST_QUEUE_1, "world!");

        verify(observer1).onNext("hello!");
        verifyNoMoreInteractions(observer1);
        verify(observer2).onNext("hello!");
        verify(observer2).onNext("world!");
    }

    @Test
    public void shouldAllowEventDataTransformation() {
        eventBus.registerQueue(TEST_QUEUE_1);
        Observer<Integer> intObserver = mock(Observer.class);

        eventBus.subscribe(TEST_QUEUE_1, observer1);
        eventBus.<String>queue(TEST_QUEUE_1).transform().map(new Func1<String, Integer>() {
            @Override
            public Integer call(String s) {
                return Integer.parseInt(s);
            }
        }).subscribe(intObserver);

        eventBus.publish(TEST_QUEUE_1, "1");

        verify(observer1).onNext("1");
        verify(intObserver).onNext(1);
    }
}
