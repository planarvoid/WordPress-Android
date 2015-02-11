package com.soundcloud.android.presentation;

import com.soundcloud.android.view.adapters.EndlessAdapter;
import com.soundcloud.android.view.adapters.ItemAdapter;
import rx.Observable;
import rx.Observer;
import rx.Subscription;
import rx.android.Pager;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Func1;
import rx.internal.util.UtilityFunctions;
import rx.observables.ConnectableObservable;
import rx.subscriptions.CompositeSubscription;
import rx.subscriptions.Subscriptions;

import java.util.LinkedList;
import java.util.List;

public class DataBinding<DataT, ViewT> {

    private final ConnectableObservable<ViewT> source;
    private final List<Observer<? super ViewT>> observers = new LinkedList<>();
    private Subscription sourceSubscription = Subscriptions.empty();

    static <ViewT> DataBinding<ViewT, ViewT> create(Observable<ViewT> source) {
        return new DataBinding<>(source, UtilityFunctions.<ViewT>identity());
    }

    public static <DataT, ViewT> ListBinding<DataT, ViewT> pagedList(
            Observable<List<DataT>> source, EndlessAdapter<ViewT> adapter, Pager<List<DataT>> pager,
            Func1<List<DataT>, List<ViewT>> itemTransformer) {
        return new ListBinding<>(source, adapter, pager, itemTransformer);
    }

    static <ViewT> ListBinding<ViewT, ViewT> list(Observable<List<ViewT>> source, ItemAdapter<ViewT> adapter) {
        return new ListBinding<>(source, adapter, UtilityFunctions.<List<ViewT>>identity());
    }

    DataBinding(Observable<DataT> source, Func1<DataT, ViewT> transformer) {
        this.source = source.map(transformer).observeOn(AndroidSchedulers.mainThread()).replay();
    }

    public DataBinding<DataT, ViewT> addViewObserver(Observer<? super ViewT> observer) {
        observers.add(observer);
        return this;
    }

    public Subscription connect() {
        sourceSubscription = source.connect();
        return sourceSubscription;
    }

    public void disconnect() {
        sourceSubscription.unsubscribe();
        clearViewObservers();
    }

    public Observable<ViewT> getSource() {
        return source;
    }

    Subscription subscribeViewObservers() {
        final CompositeSubscription viewSubscriptions = new CompositeSubscription();
        for (Observer<? super ViewT> observer : observers) {
            viewSubscriptions.add(source.subscribe(observer));
        }
        return viewSubscriptions;
    }

    void clearViewObservers() {
        observers.clear();
    }

    @Override
    public String toString() {
        return "DataBinding; observers = " + observers.size() + "; unsubscribed=" + sourceSubscription.isUnsubscribed();
    }

}
