package com.soundcloud.android.presentation;

import static com.soundcloud.android.Expect.expect;

import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.android.rx.TestPager;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import rx.Observable;
import rx.Subscription;
import rx.observers.TestSubscriber;

import java.util.Collections;
import java.util.List;

@RunWith(SoundCloudTestRunner.class)
public class CollectionBindingTest {

    @Mock private ItemAdapter<String> adapter;
    @Mock private PagingListItemAdapter<String> pagingAdapter;

    @Test
    public void shouldBuildUnpagedListBinding() {
        Observable<Iterable<String>> source = Observable.<Iterable<String>>just(Collections.singleton("item"));

        CollectionBinding<String> binding = CollectionBinding.from(source)
                .withAdapter(adapter)
                .build();

        expect(binding.adapter()).toBe(adapter);
        expect(binding.items()).not.toBeNull();
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldThrowIfNoAdapterSuppliedInBuilder() {
        Observable<Iterable<String>> source = Observable.<Iterable<String>>just(Collections.singleton("item"));

        CollectionBinding.from(source).build();
    }

    @Test
    public void shouldBuildPagedListBinding() {
        Observable<Iterable<String>> source = Observable.<Iterable<String>>just(Collections.singleton("item"));

        CollectionBinding binding = CollectionBinding.from(source)
                .withPager(TestPager.<Iterable<String>>singlePageFunction())
                .withAdapter(pagingAdapter)
                .build();

        expect(binding).toBeInstanceOf(PagedCollectionBinding.class);
        PagedCollectionBinding pagedListBinding = (PagedCollectionBinding) binding;
        expect(pagedListBinding.adapter()).toBe(pagingAdapter);
        expect(pagedListBinding.items()).not.toBeNull();
        expect(pagedListBinding.pager()).not.toBeNull();
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldThrowIfAdapterNotPagedAdapterWhenBuildingPagedBinding() {
        Observable<Iterable<String>> source = Observable.<Iterable<String>>just(Collections.singleton("item"));

        CollectionBinding.from(source)
                .withAdapter(adapter)
                .withPager(TestPager.<Iterable<String>>singlePageFunction())
                .build();
    }

    public void shouldReplaySourceSequence() {
        final List<String> listItems = Collections.singletonList("item");
        CollectionBinding<String> binding = new CollectionBinding<>(Observable.<Iterable<String>>just(listItems), adapter);
        binding.connect();

        TestSubscriber<Iterable<String>> observer = new TestSubscriber<>();
        binding.items().subscribe(observer);
        binding.items().subscribe(observer);

        expect(observer.getOnNextEvents()).toContainExactly(listItems, listItems);
    }

    @Test
    public void shouldDisconnectFromSourceSequence() {
        final List<String> listItems = Collections.singletonList("item");
        CollectionBinding<String> binding = new CollectionBinding<>(Observable.<Iterable<String>>just(listItems), adapter);
        final Subscription subscription = binding.connect();
        binding.disconnect();

        TestSubscriber<Iterable<String>> observer = new TestSubscriber<>();
        binding.items().subscribe(observer);

        expect(subscription.isUnsubscribed()).toBeTrue();
        expect(observer.getOnNextEvents()).toBeEmpty();
    }

}