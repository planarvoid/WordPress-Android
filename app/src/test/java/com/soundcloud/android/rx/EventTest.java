package com.soundcloud.android.rx;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import com.soundcloud.android.events.Event;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import rx.Observer;
import rx.Subscription;

@RunWith(SoundCloudTestRunner.class)
public class EventTest {

    @Mock
    private Observer<String> observer;

    @Test
    public void shouldPublishEventToRegisteredObserver() {
        Event.TEST_EVENT.subscribe(observer);

        Event.TEST_EVENT.publish("one");
        Event.TEST_EVENT.publish("two");

        verify(observer).onNext("one");
        verify(observer).onNext("two");
    }

    @Test
    public void shouldPublishVoidEventToRegisteredObserver() {
        Event.TEST_VOID_EVENT.subscribe(observer);

        Event.TEST_VOID_EVENT.publish();

        verify(observer).onNext(null);
    }

    @Test
    public void shouldNotPublishEventIfObserverHasUnsubscribed() {
        Subscription subscription = Event.TEST_EVENT.subscribe(observer);

        Event.TEST_EVENT.publish("one");
        subscription.unsubscribe();

        Event.TEST_EVENT.publish("two");

        verify(observer).onNext("one");
        verifyNoMoreInteractions(observer);
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldRaiseExceptionWhenTryingToPublishUnsupportedEventData() {
        Event.TEST_EVENT.subscribe(observer);
        Event.TEST_EVENT.publish(1);
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldRaiseExceptionWhenTryingToPublishVoidToEventWithDataType() {
        Event.TEST_EVENT.subscribe(observer);
        Event.TEST_EVENT.publish();
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldRaiseExceptionWhenTryingToPublishWithDataToVoidEvent() {
        Event.TEST_VOID_EVENT.subscribe(observer);
        Event.TEST_VOID_EVENT.publish(1);
    }
}
