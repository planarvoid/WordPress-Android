package com.soundcloud.android.presentation;

import static android.widget.AbsListView.OnScrollListener;

import com.soundcloud.android.rx.Pager;
import com.soundcloud.android.view.adapters.PagingAwareAdapter;

import android.widget.AbsListView;


final class PagingScrollListener implements OnScrollListener {

    private final CollectionViewPresenter presenter;
    private final PagingAwareAdapter<?> adapter;

    private final OnScrollListener listenerDelegate;

    PagingScrollListener(CollectionViewPresenter presenter, PagingAwareAdapter<?> adapter, OnScrollListener listenerDelegate) {
        this.presenter = presenter;
        this.adapter = adapter;
        this.listenerDelegate = listenerDelegate;
    }

    @Override
    public void onScrollStateChanged(AbsListView view, int scrollState) {
        listenerDelegate.onScrollStateChanged(view, scrollState);
    }

    @Override
    public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
        listenerDelegate.onScroll(view, firstVisibleItem, visibleItemCount, totalItemCount);
        int lookAheadSize = visibleItemCount * 2;
        boolean lastItemReached = totalItemCount > 0 && (totalItemCount - lookAheadSize <= firstVisibleItem);

        final PagedCollectionBinding<?, ?> pagedBinding = (PagedCollectionBinding<?, ?>) presenter.getBinding();
        final Pager<?, ?> pager = pagedBinding.pager();
        if (lastItemReached && adapter.isIdle() && pager.hasNext()) {
            adapter.setLoading();
            pager.next();
        }
    }
}
