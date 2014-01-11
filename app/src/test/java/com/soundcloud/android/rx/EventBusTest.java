package com.soundcloud.android.rx;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import com.soundcloud.android.events.EventBus;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import rx.Observer;
import rx.Subscription;

@RunWith(SoundCloudTestRunner.class)
public class EventBusTest {

    @Mock
    private Observer<String> observer;

    @Test
    public void shouldPublishEventToRegisteredObserver() {
        EventBus.SCREEN_ENTERED.subscribe(observer);

        EventBus.SCREEN_ENTERED.publish("one");
        EventBus.SCREEN_ENTERED.publish("two");

        verify(observer).onNext("one");
        verify(observer).onNext("two");
    }

    @Test
    public void shouldNotPublishEventIfObserverHasUnsubscribed() {
        Subscription subscription = EventBus.SCREEN_ENTERED.subscribe(observer);

        EventBus.SCREEN_ENTERED.publish("one");
        subscription.unsubscribe();

        EventBus.SCREEN_ENTERED.publish("two");

        verify(observer).onNext("one");
        verifyNoMoreInteractions(observer);
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldRaiseExceptionWhenTryingToPublishUnsupportedEventData() {
        EventBus.SCREEN_ENTERED.subscribe(observer);
        EventBus.SCREEN_ENTERED.publish(1);
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldRaiseExceptionWhenTryingToPublishVoidToEventWithDataType() {
        EventBus.SCREEN_ENTERED.subscribe(observer);
        EventBus.SCREEN_ENTERED.publish();
    }
}
