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

public class NewListBinding<T> {

    private final ConnectableObservable<? extends Iterable<T>> source;
    private final List<Observer<? super Iterable<T>>> observers = new LinkedList<>();
    private Subscription sourceSubscription = Subscriptions.empty();

    private final ItemAdapter<T> adapter;
    private NewPager<?, ?> pager;

    public static <T> NewListBinding<T> create(Observable<? extends Iterable<T>> source, ItemAdapter<T> adapter) {
        return new NewListBinding<>(source, adapter);
    }

    public static <T, S, CollS extends Iterable<S>, CollT extends Iterable<T>> NewListBinding<T> paged(Observable<CollS> source, PagingItemAdapter<T> adapter,
                                                                                                       NewPager<CollS, CollT> pager) {
        final Observable<CollT> pagedSource = pager.page(source);
        return new NewListBinding<>(pagedSource, adapter, pager);
    }

    NewListBinding(Observable<? extends Iterable<T>> source, ItemAdapter<T> adapter) {
        this.source = source.observeOn(AndroidSchedulers.mainThread()).replay();
        this.adapter = adapter;
    }

    <CollT extends Iterable<T>> NewListBinding(Observable<CollT> source, PagingItemAdapter<T> adapter,
                                               NewPager<?, CollT> pager) {
        this(source, adapter);
        this.pager = pager;
    }

    public void addViewObserver(Observer<? super Iterable<T>> observer) {
        observers.add(observer);
    }

    public Subscription connect() {
        sourceSubscription = source.connect();
        return sourceSubscription;
    }

    public void disconnect() {
        sourceSubscription.unsubscribe();
        clearViewObservers();
    }

    public Observable<? extends Iterable<T>> getSource() {
        return source;
    }

    Subscription subscribeViewObservers() {
        final CompositeSubscription viewSubscriptions = new CompositeSubscription();
        for (Observer<? super Iterable<T>> observer : observers) {
            viewSubscriptions.add(source.subscribe(observer));
        }
        return viewSubscriptions;
    }

    void clearViewObservers() {
        observers.clear();
    }


    public ItemAdapter<T> getAdapter() {
        return adapter;
    }

    public NewPager<?, ?> getPager() {
        return pager;
    }

    boolean isPaged() {
        return pager != null;
    }

    NewListBinding<T> resetFromCurrentPage() {
        return new NewListBinding(pager.currentPage(), (PagingItemAdapter) adapter, pager);
    }
}
