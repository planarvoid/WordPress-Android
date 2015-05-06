package com.soundcloud.android.presentation;

import static com.soundcloud.android.Expect.expect;
import static java.util.Collections.singleton;

import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.android.rx.RxTestHelper;
import com.soundcloud.android.view.adapters.ItemAdapter;
import com.soundcloud.android.view.adapters.PagingItemAdapter;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import rx.Observable;
import rx.Subscription;
import rx.internal.util.UtilityFunctions;
import rx.observers.TestSubscriber;
import rx.subjects.PublishSubject;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@RunWith(SoundCloudTestRunner.class)
public class NewListBindingTest {

    @Mock private ItemAdapter<String> adapter;
    @Mock private PagingItemAdapter<String> pagingAdapter;

    @Test
    public void shouldBuildUnpagedListBinding() {
        Observable<Iterable<String>> source = Observable.<Iterable<String>>just(Collections.singleton("item"));

        NewListBinding<String> binding = NewListBinding.from(source)
                .withAdapter(adapter)
                .build();

        expect(binding.getAdapter()).toBe(adapter);
        expect(binding.getListItems()).not.toBeNull();
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldThrowIfNoAdapterSuppliedInBuilder() {
        Observable<Iterable<String>> source = Observable.<Iterable<String>>just(Collections.singleton("item"));

        NewListBinding.from(source).build();
    }

    @Test
    public void shouldBuildPagedListBinding() {
        Observable<Iterable<String>> source = Observable.<Iterable<String>>just(Collections.singleton("item"));

        NewListBinding binding = NewListBinding.from(source)
                .withPager(RxTestHelper.<Iterable<String>>singlePageFunction())
                .withAdapter(pagingAdapter)
                .build();

        expect(binding).toBeInstanceOf(PagedListBinding.class);
        PagedListBinding pagedListBinding = (PagedListBinding) binding;
        expect(pagedListBinding.getAdapter()).toBe(pagingAdapter);
        expect(pagedListBinding.getListItems()).not.toBeNull();
        expect(pagedListBinding.getPager()).not.toBeNull();
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldThrowIfAdapterNotPagedAdapterWhenBuildingPagedBinding() {
        Observable<Iterable<String>> source = Observable.<Iterable<String>>just(Collections.singleton("item"));

        NewListBinding.from(source)
                .withAdapter(adapter)
                .withPager(RxTestHelper.<Iterable<String>>singlePageFunction())
                .build();
    }

    public void shouldReplaySourceSequence() {
        final List<String> listItems = Collections.singletonList("item");
        NewListBinding<String> binding = new NewListBinding<>(Observable.<Iterable<String>>just(listItems), adapter);
        binding.connect();

        TestSubscriber<Iterable<String>> observer = new TestSubscriber<>();
        binding.getListItems().subscribe(observer);
        binding.getListItems().subscribe(observer);

        expect(observer.getOnNextEvents()).toContainExactly(listItems, listItems);
    }

    @Test
    public void shouldSubscribeViewObserversThatWereAddedBefore() {
        final List<String> listItems = Collections.singletonList("item");
        NewListBinding<String> binding = new NewListBinding<>(Observable.<Iterable<String>>just(listItems), adapter);
        binding.connect();

        TestSubscriber<Iterable<String>> observer = new TestSubscriber<>();
        binding.addViewObserver(observer);
        binding.subscribeViewObservers();

        expect(observer.getOnNextEvents()).toContainExactly(listItems);
    }

    @Test
    public void shouldAttachViewObserversToViewSubscription() {
        final PublishSubject<Iterable<String>> observable = PublishSubject.create();
        NewListBinding<String> binding = new NewListBinding<>(observable, adapter);
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
        NewListBinding<String> binding = new NewListBinding<>(Observable.<Iterable<String>>just(listItems), adapter);
        final Subscription subscription = binding.connect();
        binding.disconnect();

        TestSubscriber<Iterable<String>> observer = new TestSubscriber<>();
        binding.addViewObserver(observer);
        binding.subscribeViewObservers();

        expect(subscription.isUnsubscribed()).toBeTrue();
        expect(observer.getOnNextEvents()).toBeEmpty();
    }

}