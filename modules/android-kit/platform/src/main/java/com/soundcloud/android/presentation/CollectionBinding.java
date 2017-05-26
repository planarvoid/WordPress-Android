package com.soundcloud.android.presentation;

import com.soundcloud.rx.Pager;
import hu.akarnokd.rxjava.interop.RxJavaInterop;
import io.reactivex.BackpressureStrategy;
import io.reactivex.Single;
import io.reactivex.functions.Function;
import rx.Observable;
import rx.Observer;
import rx.Scheduler;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Func1;
import rx.internal.util.UtilityFunctions;
import rx.observables.ConnectableObservable;
import rx.subscriptions.Subscriptions;

import java.util.ArrayList;
import java.util.List;

public class CollectionBinding<SourceT, ItemT> {

    private final ConnectableObservable<? extends SourceT> source;
    private final ItemAdapter<ItemT> adapter;
    protected final Func1<SourceT, ? extends Iterable<ItemT>> transformer;
    private List<Observer<Iterable<ItemT>>> observers = new ArrayList<>();

    private Subscription sourceSubscription = Subscriptions.empty();

    public static <ItemT, T extends Iterable<ItemT>> Builder<T, ItemT, T> fromV2(io.reactivex.Observable<T> source) {
        return fromV2(source, new Function<T, Iterable<ItemT>>() {
            @Override
            public Iterable<ItemT> apply(T itemTS) throws Exception {
                return itemTS;
            }
        });
    }

    public static <ItemT, T extends Iterable<ItemT>> Builder<T, ItemT, T> fromV2(Single<T> source) {
        return fromV2(source, new Function<T, Iterable<ItemT>>() {
            @Override
            public Iterable<ItemT> apply(T itemTS) throws Exception {
                return itemTS;
            }
        });
    }

    public static <SourceT, ItemT, T extends Iterable<ItemT>> Builder<SourceT, ItemT, T> fromV2(
            io.reactivex.Observable<SourceT> source, final Function<SourceT, ? extends Iterable<ItemT>> transformer) {
        return new Builder<>(RxJavaInterop.toV1Observable(source, BackpressureStrategy.ERROR), new Func1<SourceT, Iterable<ItemT>>() {
            @Override
            public Iterable<ItemT> call(SourceT sourceT) {
                try {
                    return transformer.apply(sourceT);
                } catch (Exception e) {
                    throw new IllegalStateException(e);
                }
            }
        });
    }

    public static <SourceT, ItemT, T extends Iterable<ItemT>> Builder<SourceT, ItemT, T> fromV2(
            Single<SourceT> source, final Function<SourceT, ? extends Iterable<ItemT>> transformer) {
        return new Builder<>(RxJavaInterop.toV1Observable(source.toObservable(), BackpressureStrategy.ERROR), new Func1<SourceT, Iterable<ItemT>>() {
            @Override
            public Iterable<ItemT> call(SourceT sourceT) {
                try {
                    return transformer.apply(sourceT);
                } catch (Exception e) {
                    throw new IllegalStateException(e);
                }
            }
        });
    }

    public static <ItemT, T extends Iterable<ItemT>> Builder<T, ItemT, T> from(Observable<T> source) {
        return from(source, UtilityFunctions.<T>identity());
    }

    public static <SourceT, ItemT, T extends Iterable<ItemT>> Builder<SourceT, ItemT, T> from(
            Observable<SourceT> source, Func1<SourceT, ? extends Iterable<ItemT>> transformer) {
        return new Builder<>(source, transformer);
    }

    CollectionBinding(Observable<? extends SourceT> source,
                      Func1<SourceT, ? extends Iterable<ItemT>> transformer,
                      ItemAdapter<ItemT> adapter,
                      List<Observer<Iterable<ItemT>>> observers) {
        this(source, transformer, adapter, AndroidSchedulers.mainThread(), observers);
    }

    CollectionBinding(Observable<? extends SourceT> source,
                      Func1<SourceT, ? extends Iterable<ItemT>> transformer,
                      ItemAdapter<ItemT> adapter, Scheduler observeOn,
                      List<Observer<Iterable<ItemT>>> observers) {
        this.observers = observers;
        this.source = source.observeOn(observeOn).replay();
        this.transformer = transformer;
        this.adapter = adapter;
    }

    public Subscription connect() {
        sourceSubscription = source.connect();
        return sourceSubscription;
    }

    public void disconnect() {
        sourceSubscription.unsubscribe();
    }

    public List<Observer<Iterable<ItemT>>> observers() {
        return observers;
    }

    public Observable<? extends Iterable<ItemT>> items() {
        return source.map(transformer);
    }

    public ItemAdapter<ItemT> adapter() {
        return adapter;
    }

    public ConnectableObservable<? extends SourceT> source() {
        return source;
    }

    public static class Builder<SourceT, ItemT, T extends Iterable<ItemT>> {

        private final Observable<SourceT> source;
        private final Func1<SourceT, ? extends Iterable<ItemT>> transformer;
        private ItemAdapter<ItemT> adapter;
        private Pager.PagingFunction<SourceT> pagingFunction;
        private List<Observer<Iterable<ItemT>>> observers = new ArrayList<>();

        public Builder(Observable<SourceT> source, Func1<SourceT, ? extends Iterable<ItemT>> transformer) {
            this.source = source;
            this.transformer = transformer;
        }

        public Builder<SourceT, ItemT, T> withPager(Pager.PagingFunction<SourceT> pagingFunction) {
            this.pagingFunction = pagingFunction;
            return this;
        }

        public Builder<SourceT, ItemT, T> withAdapter(ItemAdapter<ItemT> adapter) {
            this.adapter = adapter;
            this.observers.add(adapter);
            return this;
        }

        public Builder<SourceT, ItemT, T> addObserver(Observer<Iterable<ItemT>> subscriber) {
            this.observers.add(subscriber);
            return this;
        }

        public CollectionBinding<SourceT, ItemT> build() {
            if (adapter == null) {
                throw new IllegalArgumentException("Adapter can't be null");
            }
            if (pagingFunction != null) {
                if (!(adapter instanceof PagingAwareAdapter)) {
                    throw new IllegalArgumentException("Adapter must implement " + PagingAwareAdapter.class
                            + " when used in a paged binding");
                }
                final Pager<SourceT> pager = Pager.create(pagingFunction);
                return new PagedCollectionBinding<>(
                        pager.page(source), transformer, (PagingAwareAdapter<ItemT>) adapter, pager, observers);
            } else {
                return new CollectionBinding<>(source, transformer, adapter, observers);
            }
        }

    }
}
