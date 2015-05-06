package com.soundcloud.android.presentation;

import com.soundcloud.android.view.adapters.PagingItemAdapter;
import rx.Observable;
import rx.android.NewPager;

public class PagedListBinding<ItemT, Items extends Iterable<ItemT>> extends NewListBinding<ItemT> {

    private final PagingItemAdapter<ItemT> adapter;
    private final NewPager<?, Items> pager;

    PagedListBinding(Observable<Items> listItems, PagingItemAdapter<ItemT> adapter, NewPager<?, Items> pager) {
        super(listItems, adapter);
        this.adapter = adapter;
        this.pager = pager;
    }

    @Override
    public PagingItemAdapter<ItemT> getAdapter() {
        return adapter;
    }

    public NewPager<?, ? extends Iterable<ItemT>> getPager() {
        return pager;
    }

    PagedListBinding<ItemT, Items> fromCurrentPage() {
        return new PagedListBinding<>(pager.currentPage(), adapter, pager);
    }
}
