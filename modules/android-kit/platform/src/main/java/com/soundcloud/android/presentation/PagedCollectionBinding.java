package com.soundcloud.android.presentation;

import com.soundcloud.rx.Pager;
import rx.Observable;
import rx.Observer;
import rx.Scheduler;
import rx.functions.Func1;

import java.util.List;

public class PagedCollectionBinding<SourceT, ItemT, Items extends Iterable<ItemT>>
        extends CollectionBinding<SourceT, ItemT> {

    private final PagingAwareAdapter<ItemT> adapter;
    private final Pager<SourceT> pager;
    private final List<Observer<Iterable<ItemT>>> observers;

    PagedCollectionBinding(Observable<SourceT> source,
                           Func1<SourceT, ? extends Iterable<ItemT>> transformer,
                           PagingAwareAdapter<ItemT> adapter,
                           Pager<SourceT> pager,
                           List<Observer<Iterable<ItemT>>> observers) {
        super(source, transformer, adapter, observers);
        this.adapter = adapter;
        this.pager = pager;
        this.observers = observers;
    }

    PagedCollectionBinding(Observable<SourceT> source,
                           Func1<SourceT, Iterable<ItemT>> transformer,
                           PagingAwareAdapter<ItemT> adapter,
                           Pager<SourceT> pager, Scheduler observeOn,
                           List<Observer<Iterable<ItemT>>> observers) {
        super(source, transformer, adapter, observeOn, observers);
        this.adapter = adapter;
        this.pager = pager;
        this.observers = observers;
    }

    @Override
    public PagingAwareAdapter<ItemT> adapter() {
        return adapter;
    }

    public Pager<SourceT> pager() {
        return pager;
    }

    PagedCollectionBinding<SourceT, ItemT, Items> fromCurrentPage() {
        return new PagedCollectionBinding<>(pager.currentPage(), transformer, adapter, pager, observers);
    }
}
