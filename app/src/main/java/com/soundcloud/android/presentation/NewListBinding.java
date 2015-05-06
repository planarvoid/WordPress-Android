package com.soundcloud.android.presentation;

import com.soundcloud.android.view.adapters.ItemAdapter;
import com.soundcloud.android.view.adapters.PagingItemAdapter;
import rx.Observable;
import rx.Observer;
import rx.Subscription;
import rx.android.NewPager;
import rx.android.schedulers.AndroidSchedulers;
import rx.observables.ConnectableObservable;
import rx.subscriptions.CompositeSubscription;
import rx.subscriptions.Subscriptions;

import java.util.LinkedList;
import java.util.List;

public class NewListBinding<ItemT> {

    private final ConnectableObservable<? extends Iterable<ItemT>> listItems;
    private final List<Observer<? super Iterable<ItemT>>> observers = new LinkedList<>();
    private Subscription sourceSubscription = Subscriptions.empty();

    private final ItemAdapter<ItemT> adapter;

    public static <T> NewListBinding<T> create(Observable<? extends Iterable<T>> source, ItemAdapter<T> adapter) {
        return new NewListBinding<>(source, adapter);
    }

    public static <T, S, CollS extends Iterable<S>, CollT extends Iterable<T>> PagedListBinding<T, CollT> paged(
            Observable<CollS> source, PagingItemAdapter<T> adapter, NewPager<CollS, CollT> pager) {
        final Observable<CollT> pagedSource = pager.page(source);
        return new PagedListBinding<>(pagedSource, adapter, pager);
    }

    NewListBinding(Observable<? extends Iterable<ItemT>> listItems, ItemAdapter<ItemT> adapter) {
        this.listItems = listItems.observeOn(AndroidSchedulers.mainThread()).replay();
        this.adapter = adapter;
    }

    public void addViewObserver(Observer<? super Iterable<ItemT>> observer) {
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

    public Observable<? extends Iterable<ItemT>> getListItems() {
        return listItems;
    }

    Subscription subscribeViewObservers() {
        final CompositeSubscription viewSubscriptions = new CompositeSubscription();
        for (Observer<? super Iterable<ItemT>> observer : observers) {
            viewSubscriptions.add(listItems.subscribe(observer));
        }
        return viewSubscriptions;
    }

    void clearViewObservers() {
        observers.clear();
    }

    public ItemAdapter<ItemT> getAdapter() {
        return adapter;
    }
}
