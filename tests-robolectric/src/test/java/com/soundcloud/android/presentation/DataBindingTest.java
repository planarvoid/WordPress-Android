package com.soundcloud.android.presentation;

import static com.soundcloud.android.Expect.expect;

import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import org.junit.Test;
import org.junit.runner.RunWith;
import rx.Observable;
import rx.Subscription;
import rx.observers.TestSubscriber;
import rx.subjects.PublishSubject;

@RunWith(SoundCloudTestRunner.class)
public class DataBindingTest {

    @Test
    public void shouldReplaySourceSequence() {
        DataBinding<String, String> binding = DataBinding.create(Observable.just("event"));
        binding.connect();

        TestSubscriber<String> observer = new TestSubscriber<>();
        binding.getSource().subscribe(observer);
        binding.getSource().subscribe(observer);

        expect(observer.getOnNextEvents()).toContainExactly("event", "event");
    }

    @Test
    public void shouldSubscribeViewObserversThatWereAddedBefore() {
        DataBinding<String, String> binding = DataBinding.create(Observable.just("event"));
        binding.connect();

        TestSubscriber<String> observer = new TestSubscriber<>();
        binding.addViewObserver(observer);
        binding.subscribeViewObservers();

        expect(observer.getOnNextEvents()).toContainExactly("event");
    }

    @Test
    public void shouldAttachViewObserversToViewSubscription() {
        final PublishSubject<String> observable = PublishSubject.create();
        DataBinding<String, String> binding = DataBinding.create(observable);
        binding.connect();

        observable.onNext("event 1");

        TestSubscriber<String> observer1 = new TestSubscriber<>();
        TestSubscriber<String> observer2 = new TestSubscriber<>();
        binding.addViewObserver(observer1);
        binding.addViewObserver(observer2);
        final Subscription subscription = binding.subscribeViewObservers();
        subscription.unsubscribe();

        observable.onNext("event 2");

        expect(observer1.getOnNextEvents()).toContainExactly("event 1");
        expect(observer2.getOnNextEvents()).toContainExactly("event 1");
    }

    @Test
    public void shouldDisconnectFromSourceSequence() {
        DataBinding<String, String> binding = DataBinding.create(Observable.just("event"));
        final Subscription subscription = binding.connect();
        binding.disconnect();

        TestSubscriber<String> observer = new TestSubscriber<>();
        binding.addViewObserver(observer);
        binding.subscribeViewObservers();

        expect(subscription.isUnsubscribed()).toBeTrue();
        expect(observer.getOnNextEvents()).toBeEmpty();
    }

}