package com.soundcloud.android.presentation;

import static com.soundcloud.android.Expect.expect;
import static java.util.Collections.singleton;

import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.android.view.adapters.ItemAdapter;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import rx.Observable;
import rx.Subscription;
import rx.observers.TestSubscriber;
import rx.subjects.PublishSubject;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@RunWith(SoundCloudTestRunner.class)
public class NewListBindingTest {

    @Mock private ItemAdapter<String> adapter;

    @Test
    public void shouldReplaySourceSequence() {
        final List<String> listItems = Collections.singletonList("item");
        NewListBinding<String> binding = NewListBinding.create(Observable.<Iterable<String>>just(listItems), adapter);
        binding.connect();

        TestSubscriber<Iterable<String>> observer = new TestSubscriber<>();
        binding.getListItems().subscribe(observer);
        binding.getListItems().subscribe(observer);

        expect(observer.getOnNextEvents()).toContainExactly(listItems, listItems);
    }

    @Test
    public void shouldSubscribeViewObserversThatWereAddedBefore() {
        final List<String> listItems = Collections.singletonList("item");
        NewListBinding<String> binding = NewListBinding.create(Observable.<Iterable<String>>just(listItems), adapter);
        binding.connect();

        TestSubscriber<Iterable<String>> observer = new TestSubscriber<>();
        binding.addViewObserver(observer);
        binding.subscribeViewObservers();

        expect(observer.getOnNextEvents()).toContainExactly(listItems);
    }

    @Test
    public void shouldAttachViewObserversToViewSubscription() {
        final PublishSubject<Iterable<String>> observable = PublishSubject.create();
        NewListBinding<String> binding = NewListBinding.create(observable, adapter);
        binding.connect();

        observable.onNext(singleton("event 1"));

        TestSubscriber<Iterable<String>> observer1 = new TestSubscriber<>();
        TestSubscriber<Iterable<String>> observer2 = new TestSubscriber<>();
        binding.addViewObserver(observer1);
        binding.addViewObserver(observer2);
        final Subscription subscription = binding.subscribeViewObservers();
        subscription.unsubscribe();

        observable.onNext(singleton("event 2"));

        expect(observer1.getOnNextEvents()).toContainExactly(singleton("event 1"));
        expect(observer2.getOnNextEvents()).toContainExactly(singleton("event 1"));
    }

    @Test
    public void shouldDisconnectFromSourceSequence() {
        final List<String> listItems = Collections.singletonList("item");
        NewListBinding<String> binding = NewListBinding.create(Observable.<Iterable<String>>just(listItems), adapter);
        final Subscription subscription = binding.connect();
        binding.disconnect();

        TestSubscriber<Iterable<String>> observer = new TestSubscriber<>();
        binding.addViewObserver(observer);
        binding.subscribeViewObservers();

        expect(subscription.isUnsubscribed()).toBeTrue();
        expect(observer.getOnNextEvents()).toBeEmpty();
    }

}