package com.soundcloud.android.presentation;

import static com.google.common.base.Preconditions.checkArgument;

import com.soundcloud.android.view.adapters.ItemAdapter;
import com.soundcloud.android.view.adapters.PagingItemAdapter;
import rx.Observable;
import rx.Observer;
import rx.Subscription;
import rx.android.NewPager;
import rx.android.NewPager.PagingFunction;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Func1;
import rx.internal.util.UtilityFunctions;
import rx.observables.ConnectableObservable;
import rx.subscriptions.CompositeSubscription;
import rx.subscriptions.Subscriptions;

import java.util.LinkedList;
import java.util.List;

public class NewListBinding<Item> {

    private final ConnectableObservable<? extends Iterable<Item>> listItems;
    private final List<Observer<? super Iterable<Item>>> observers = new LinkedList<>();
    private Subscription sourceSubscription = Subscriptions.empty();

    private final ItemAdapter<Item> adapter;

    public static <T, IT extends Iterable<T>> Builder<IT, T, IT> from(Observable<IT> source) {
        return from(source, UtilityFunctions.<IT>identity());
    }

    public static <S, T, IT extends Iterable<T>> Builder<S, T, IT> from(
            Observable<S> source, Func1<S, IT> transformer) {
        return new Builder<>(source, transformer);
    }

    NewListBinding(Observable<? extends Iterable<Item>> listItems, ItemAdapter<Item> adapter) {
        checkArgument(adapter != null, "adapter can't be null");
        this.listItems = listItems.observeOn(AndroidSchedulers.mainThread()).replay();
        this.adapter = adapter;
    }

    public void addViewObserver(Observer<? super Iterable<Item>> observer) {
        observers.add(observer);
    }

    public Subscription connect() {
        sourceSubscription = listItems.connect();
        return sourceSubscription;
    }

    public void disconnect() {
        sourceSubscription.unsubscribe();
        clearViewObservers();
    }

    public Observable<? extends Iterable<Item>> getListItems() {
        return listItems;
    }

    Subscription subscribeViewObservers() {
        final CompositeSubscription viewSubscriptions = new CompositeSubscription();
        for (Observer<? super Iterable<Item>> observer : observers) {
            viewSubscriptions.add(listItems.subscribe(observer));
        }
        return viewSubscriptions;
    }

    void clearViewObservers() {
        observers.clear();
    }

    public ItemAdapter<Item> getAdapter() {
        return adapter;
    }

    public static class Builder<S, T, IT extends Iterable<T>> {

        private final Observable<S> source;
        private final Func1<S, IT> transformer;
        private ItemAdapter<T> adapter;
        private PagingFunction<S> pagingFunction;

        Builder(Observable<S> source, Func1<S, IT> transformer) {
            this.source = source;
            this.transformer = transformer;
        }

        public Builder<S, T, IT> withPager(PagingFunction<S> pagingFunction) {
            this.pagingFunction = pagingFunction;
            return this;
        }

        public Builder<S, T, IT> withAdapter(ItemAdapter<T> adapter) {
            this.adapter = adapter;
            return this;
        }

        public NewListBinding<T> build() {
            if (pagingFunction != null) {
                checkArgument(adapter instanceof PagingItemAdapter,
                        "adapter in paged binding must be " + PagingItemAdapter.class);
                final NewPager<S, IT> pager = NewPager.create(pagingFunction, transformer);
                return new PagedListBinding<>(pager.page(source), (PagingItemAdapter<T>) adapter, pager);
            } else {
                return new NewListBinding<>(source.map(transformer), adapter);
            }
        }

    }
}
