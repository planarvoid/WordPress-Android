package com.soundcloud.rx.eventbus;

import io.reactivex.observers.TestObserver;
import io.reactivex.schedulers.Schedulers;
import org.junit.Test;
import rx.observers.TestSubscriber;

public class EventBusInteropTest {

    private static final Queue<String> TEST_DEFAULT_QUEUE = Queue.of(String.class).get();

    private final EventBus v1Bus = new DefaultEventBus(rx.schedulers.Schedulers.immediate());
    private final DefaultEventBusV2 v2Bus = new DefaultEventBusV2(Schedulers.trampoline(), v1Bus);

    private final TestObserver<String> testObserver = new TestObserver<>();
    private final TestSubscriber<String> testSubscriber = new TestSubscriber<>();

    @Test
    public void v2ReceivesEventsFromV1() throws Exception {
        v2Bus.subscribe(TEST_DEFAULT_QUEUE, testObserver);

        v1Bus.publish(TEST_DEFAULT_QUEUE, "Hello");
        v1Bus.publish(TEST_DEFAULT_QUEUE, "World");

        testObserver.assertValueCount(2)
                    .assertValues("Hello", "World");
    }

    @Test
    public void v1ReceivesEventsFromV2() throws Exception {
        v1Bus.subscribe(TEST_DEFAULT_QUEUE, testSubscriber);

        v2Bus.publish(TEST_DEFAULT_QUEUE, "Hello");
        v2Bus.publish(TEST_DEFAULT_QUEUE, "World");

        testSubscriber.assertValueCount(2);
        testSubscriber.assertValues("Hello", "World");
    }
}
