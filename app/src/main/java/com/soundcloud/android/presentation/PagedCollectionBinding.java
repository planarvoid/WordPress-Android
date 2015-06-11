package com.soundcloud.android.presentation;

import com.soundcloud.android.rx.Pager;
import rx.Observable;

public class PagedCollectionBinding<ItemT, Items extends Iterable<ItemT>> extends CollectionBinding<ItemT> {

    private final PagingAwareAdapter<ItemT> adapter;
    private final Pager<?, Items> pager;

    PagedCollectionBinding(Observable<Items> listItems, PagingAwareAdapter<ItemT> adapter, Pager<?, Items> pager) {
        super(listItems, adapter);
        this.adapter = adapter;
        this.pager = pager;
    }

    @Override
    public PagingAwareAdapter<ItemT> adapter() {
        return adapter;
    }

    public Pager<?, ? extends Iterable<ItemT>> pager() {
        return pager;
    }

    PagedCollectionBinding<ItemT, Items> fromCurrentPage() {
        return new PagedCollectionBinding<>(pager.currentPage(), adapter, pager);
    }
}
