package com.soundcloud.android.presentation;

import static com.google.common.base.Preconditions.checkArgument;

import com.soundcloud.android.view.adapters.PagingItemAdapter;
import com.soundcloud.android.view.adapters.ReactiveItemAdapter;
import rx.Observable;
import rx.Subscription;
import rx.android.Pager;
import rx.android.Pager.PagingFunction;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Func1;
import rx.internal.util.UtilityFunctions;
import rx.observables.ConnectableObservable;
import rx.subscriptions.Subscriptions;

public class ListBinding<Item> {

    private final ConnectableObservable<? extends Iterable<Item>> items;
    private final ReactiveItemAdapter<Item> adapter;
    private Subscription sourceSubscription = Subscriptions.empty();

    public static <Item, T extends Iterable<Item>> Builder<T, Item, T> from(Observable<T> source) {
        return from(source, UtilityFunctions.<T>identity());
    }

    public static <S, Item, T extends Iterable<Item>> Builder<S, Item, T> from(
            Observable<S> source, Func1<S, T> transformer) {
        return new Builder<>(source, transformer);
    }

    ListBinding(Observable<? extends Iterable<Item>> items, ReactiveItemAdapter<Item> adapter) {
        checkArgument(adapter != null, "adapter can't be null");
        this.items = items.observeOn(AndroidSchedulers.mainThread()).replay();
        this.adapter = adapter;
    }

    public Subscription connect() {
        sourceSubscription = items.connect();
        return sourceSubscription;
    }

    public void disconnect() {
        sourceSubscription.unsubscribe();
    }

    public Observable<? extends Iterable<Item>> items() {
        return items;
    }

    public ReactiveItemAdapter<Item> adapter() {
        return adapter;
    }

    public static class Builder<S, Item, T extends Iterable<Item>> {

        private final Observable<S> source;
        private final Func1<S, T> transformer;
        private ReactiveItemAdapter<Item> adapter;
        private PagingFunction<S> pagingFunction;

        Builder(Observable<S> source, Func1<S, T> transformer) {
            this.source = source;
            this.transformer = transformer;
        }

        public Builder<S, Item, T> withPager(PagingFunction<S> pagingFunction) {
            this.pagingFunction = pagingFunction;
            return this;
        }

        public Builder<S, Item, T> withAdapter(ReactiveItemAdapter<Item> adapter) {
            this.adapter = adapter;
            return this;
        }

        public ListBinding<Item> build() {
            if (pagingFunction != null) {
                checkArgument(adapter instanceof PagingItemAdapter,
                        "adapter in paged binding must be " + PagingItemAdapter.class);
                final Pager<S, T> pager = Pager.create(pagingFunction, transformer);
                return new PagedListBinding<>(pager.page(source), (PagingItemAdapter<Item>) adapter, pager);
            } else {
                return new ListBinding<>(source.map(transformer), adapter);
            }
        }

    }
}
