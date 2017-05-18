package com.soundcloud.android.presentation;

import static org.assertj.core.api.Java6Assertions.assertThat;

import com.soundcloud.rx.Pager;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import rx.Observable;
import rx.Observer;
import rx.Subscription;
import rx.internal.util.UtilityFunctions;
import rx.observers.TestSubscriber;
import rx.schedulers.Schedulers;

import java.util.Collections;
import java.util.List;


@RunWith(MockitoJUnitRunner.class)
public class CollectionBindingTest {

    @Mock private ItemAdapter<String> adapter;
    @Mock private Observer<Iterable<String>> observer;
    @Mock(extraInterfaces = PagingAwareAdapter.class) ItemAdapter<String> pagingAdapter;

    private static <T> Pager.PagingFunction<T> singlePageFunction() {
        return new Pager.PagingFunction<T>() {
            @Override
            public Observable<T> call(T t) {
                return Pager.finish();
            }
        };
    }

    @Test
    public void shouldBuildUnpagedListBinding() {
        Observable<Iterable<String>> source = Observable.<Iterable<String>>just(Collections.singleton("item"));

        CollectionBinding<Iterable<String>, String> binding = CollectionBinding.from(source)
                .withAdapter(adapter)
                .build();

        assertThat(binding.adapter()).isSameAs(adapter);
        assertThat(binding.items()).isNotNull();
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
                                                     .withPager(CollectionBindingTest.<Iterable<String>>singlePageFunction())
                                                     .withAdapter(pagingAdapter)
                                                     .build();

        assertThat(binding).isInstanceOf(PagedCollectionBinding.class);
        PagedCollectionBinding pagedListBinding = (PagedCollectionBinding) binding;
        assertThat(pagedListBinding.adapter()).isSameAs(pagingAdapter);
        assertThat(pagedListBinding.items()).isNotNull();
        assertThat(pagedListBinding.pager()).isNotNull();
    }


    @Test
    public void shouldBuildPagedListBindingWithV2() {
        io.reactivex.Observable<Iterable<String>> source = io.reactivex.Observable.<Iterable<String>>just(Collections.singleton("item"));

        CollectionBinding binding = CollectionBinding.fromV2(source)
                                                     .withPager(CollectionBindingTest.<Iterable<String>>singlePageFunction())
                                                     .withAdapter(pagingAdapter)
                                                     .build();

        assertThat(binding).isInstanceOf(PagedCollectionBinding.class);
        PagedCollectionBinding pagedListBinding = (PagedCollectionBinding) binding;
        assertThat(pagedListBinding.adapter()).isSameAs(pagingAdapter);
        assertThat(pagedListBinding.items()).isNotNull();
        assertThat(pagedListBinding.pager()).isNotNull();
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldThrowIfAdapterNotPagedAdapterWhenBuildingPagedBinding() {
        Observable<Iterable<String>> source = Observable.<Iterable<String>>just(Collections.singleton("item"));

        CollectionBinding.from(source)
                .withAdapter(adapter)
                .withPager(CollectionBindingTest.<Iterable<String>>singlePageFunction())
                .build();
    }

    public void shouldReplaySourceSequence() {
        final List<String> listItems = Collections.singletonList("item");
        CollectionBinding<Iterable<String>, String> binding = getCollectionBinding(Observable.just(listItems));
        binding.connect();

        TestSubscriber<Iterable<String>> observer = new TestSubscriber<>();
        binding.items().subscribe(observer);
        binding.items().subscribe(observer);

        assertThat(observer.getOnNextEvents()).containsExactly(listItems, listItems);
    }

    @Test
    public void shouldDisconnectFromSourceSequence() {
        final List<String> listItems = Collections.singletonList("item");
        CollectionBinding<Iterable<String>, String> binding = getCollectionBinding(Observable.just(listItems));
        final Subscription subscription = binding.connect();
        binding.disconnect();

        TestSubscriber<Iterable<String>> observer = new TestSubscriber<>();
        binding.items().subscribe(observer);

        assertThat(subscription.isUnsubscribed()).isTrue();
        assertThat(observer.getOnNextEvents()).isEmpty();
    }

    @Test
    public void adapterShouldBeAnObserver() {
        final List<String> listItems = Collections.singletonList("item");
        CollectionBinding collectionBinding = CollectionBinding.from(Observable.just(listItems))
                .withAdapter(adapter)
                .addObserver(observer)
                .build();

        assertThat(collectionBinding.observers()).hasSize(2);
        assertThat(collectionBinding.observers()).contains(adapter, observer);
    }

    private CollectionBinding<Iterable<String>, String> getCollectionBinding(Observable<List<String>> source) {
        List<Observer<Iterable<String>>> observers = Collections.<Observer<Iterable<String>>>singletonList(adapter);
        return new CollectionBinding<>(source, UtilityFunctions.<Iterable<String>>identity(), adapter, Schedulers.immediate(), observers);
    }
}
