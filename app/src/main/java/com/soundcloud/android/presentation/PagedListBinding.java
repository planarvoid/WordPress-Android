package com.soundcloud.android.presentation;

import com.soundcloud.android.view.adapters.PagingItemAdapter;
import rx.Observable;
import rx.android.Pager;

public class PagedListBinding<ItemT, Items extends Iterable<ItemT>> extends ListBinding<ItemT> {

    private final PagingItemAdapter<ItemT> adapter;
    private final Pager<?, Items> pager;

    PagedListBinding(Observable<Items> listItems, PagingItemAdapter<ItemT> adapter, Pager<?, Items> pager) {
        super(listItems, adapter);
        this.adapter = adapter;
        this.pager = pager;
    }

    @Override
    public PagingItemAdapter<ItemT> adapter() {
        return adapter;
    }

    public Pager<?, ? extends Iterable<ItemT>> pager() {
        return pager;
    }

    PagedListBinding<ItemT, Items> fromCurrentPage() {
        return new PagedListBinding<>(pager.currentPage(), adapter, pager);
    }
}
