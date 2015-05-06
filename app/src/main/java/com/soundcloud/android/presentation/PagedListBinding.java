package com.soundcloud.android.presentation;

import com.soundcloud.android.view.adapters.PagingItemAdapter;
import rx.Observable;
import rx.android.NewPager;

public class PagedListBinding<ItemT, CollT extends Iterable<ItemT>> extends NewListBinding<ItemT> {

    private final PagingItemAdapter<ItemT> adapter;
    private final NewPager<?, CollT> pager;

    PagedListBinding(Observable<CollT> listItems, PagingItemAdapter<ItemT> adapter,
                     NewPager<?, CollT> pager) {
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

    PagedListBinding<ItemT, CollT> fromCurrentPage() {
        return new PagedListBinding<>(pager.currentPage(), adapter, pager);
    }
}
