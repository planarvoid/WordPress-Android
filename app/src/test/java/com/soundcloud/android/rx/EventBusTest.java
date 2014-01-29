package com.soundcloud.android.rx;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import com.soundcloud.android.events.EventBus;
import com.soundcloud.android.events.UIEvent;
import com.soundcloud.android.model.Track;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import rx.Observer;
import rx.Subscription;

@RunWith(SoundCloudTestRunner.class)
public class EventBusTest {

    @Mock
    private Observer<String> observer;

    private Subscription eventSubscription;

    @After
    public void tearDown() throws Exception {
        eventSubscription.unsubscribe();
    }

    @Test
    public void shouldPublishEventToRegisteredObserver() {
        eventSubscription = EventBus.SCREEN_ENTERED.subscribe(observer);

        EventBus.SCREEN_ENTERED.publish("one");
        EventBus.SCREEN_ENTERED.publish("two");

        verify(observer).onNext("one");
        verify(observer).onNext("two");
    }

    @Test
    public void shouldNotPublishEventIfObserverHasUnsubscribed() {
        eventSubscription = EventBus.SCREEN_ENTERED.subscribe(observer);

        EventBus.SCREEN_ENTERED.publish("one");
        eventSubscription.unsubscribe();

        EventBus.SCREEN_ENTERED.publish("two");

        verify(observer).onNext("one");
        verifyNoMoreInteractions(observer);
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldRaiseExceptionWhenTryingToPublishEventDataOfIncompatibleType() {
        eventSubscription = EventBus.SCREEN_ENTERED.subscribe(observer);
        EventBus.SCREEN_ENTERED.publish(1);
    }

    @Test
    public void shouldNotRaiseExceptionWhenTryingToPublishSubtypesOfDeclaredEventType() {
        eventSubscription = EventBus.UI.subscribe(observer);
        UIEvent event = UIEvent.fromToggleLike(true, "screen", new Track());
        EventBus.UI.publish(event);
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldRaiseExceptionWhenTryingToPublishVoidToEventWithDataType() {
        eventSubscription = EventBus.SCREEN_ENTERED.subscribe(observer);
        EventBus.SCREEN_ENTERED.publish();
    }
}
