package com.soundcloud.android.presentation;

import static com.soundcloud.android.Expect.expect;
import static java.util.Collections.singleton;

import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.android.rx.TestPager;
import com.soundcloud.android.view.adapters.ItemAdapter;
import com.soundcloud.android.view.adapters.PagingItemAdapter;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import rx.Observable;
import rx.Subscription;
import rx.observers.TestSubscriber;
import rx.subjects.PublishSubject;

import java.util.Collections;
import java.util.List;

@RunWith(SoundCloudTestRunner.class)
public class ListBindingTest {

    @Mock private ItemAdapter<String> adapter;
    @Mock private PagingItemAdapter<String> pagingAdapter;

    @Test
    public void shouldBuildUnpagedListBinding() {
        Observable<Iterable<String>> source = Observable.<Iterable<String>>just(Collections.singleton("item"));

        ListBinding<String> binding = ListBinding.from(source)
                .withAdapter(adapter)
                .build();

        expect(binding.adapter()).toBe(adapter);
        expect(binding.items()).not.toBeNull();
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldThrowIfNoAdapterSuppliedInBuilder() {
        Observable<Iterable<String>> source = Observable.<Iterable<String>>just(Collections.singleton("item"));

        ListBinding.from(source).build();
    }

    @Test
    public void shouldBuildPagedListBinding() {
        Observable<Iterable<String>> source = Observable.<Iterable<String>>just(Collections.singleton("item"));

        ListBinding binding = ListBinding.from(source)
                .withPager(TestPager.<Iterable<String>>singlePageFunction())
                .withAdapter(pagingAdapter)
                .build();

        expect(binding).toBeInstanceOf(PagedListBinding.class);
        PagedListBinding pagedListBinding = (PagedListBinding) binding;
        expect(pagedListBinding.adapter()).toBe(pagingAdapter);
        expect(pagedListBinding.items()).not.toBeNull();
        expect(pagedListBinding.pager()).not.toBeNull();
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldThrowIfAdapterNotPagedAdapterWhenBuildingPagedBinding() {
        Observable<Iterable<String>> source = Observable.<Iterable<String>>just(Collections.singleton("item"));

        ListBinding.from(source)
                .withAdapter(adapter)
                .withPager(TestPager.<Iterable<String>>singlePageFunction())
                .build();
    }

    public void shouldReplaySourceSequence() {
        final List<String> listItems = Collections.singletonList("item");
        ListBinding<String> binding = new ListBinding<>(Observable.<Iterable<String>>just(listItems), adapter);
        binding.connect();

        TestSubscriber<Iterable<String>> observer = new TestSubscriber<>();
        binding.items().subscribe(observer);
        binding.items().subscribe(observer);

        expect(observer.getOnNextEvents()).toContainExactly(listItems, listItems);
    }

    @Test
    public void shouldSubscribeViewObserversThatWereAddedBefore() {
        final List<String> listItems = Collections.singletonList("item");
        ListBinding<String> binding = new ListBinding<>(Observable.<Iterable<String>>just(listItems), adapter);
        binding.connect();

        TestSubscriber<Iterable<String>> observer = new TestSubscriber<>();
        binding.addViewObserver(observer);
        binding.subscribeViewObservers();

        expect(observer.getOnNextEvents()).toContainExactly(listItems);
    }

    @Test
    public void shouldAttachViewObserversToViewSubscription() {
        final PublishSubject<Iterable<String>> observable = PublishSubject.create();
        ListBinding<String> binding = new ListBinding<>(observable, adapter);
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
        ListBinding<String> binding = new ListBinding<>(Observable.<Iterable<String>>just(listItems), adapter);
        final Subscription subscription = binding.connect();
        binding.disconnect();

        TestSubscriber<Iterable<String>> observer = new TestSubscriber<>();
        binding.addViewObserver(observer);
        binding.subscribeViewObservers();

        expect(subscription.isUnsubscribed()).toBeTrue();
        expect(observer.getOnNextEvents()).toBeEmpty();
    }

}