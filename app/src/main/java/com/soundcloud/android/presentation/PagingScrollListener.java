package com.soundcloud.android.presentation;

import com.soundcloud.android.view.adapters.PagingItemAdapter;
import rx.android.Pager;

import android.widget.AbsListView;


final class PagingScrollListener implements AbsListView.OnScrollListener {

    private final ListPresenter listPresenter;
    private final PagingItemAdapter<?> adapter;
    private final AbsListView.OnScrollListener listenerDelegate;

    PagingScrollListener(ListPresenter listPresenter, PagingItemAdapter<?> adapter, AbsListView.OnScrollListener listenerDelegate) {
        this.listPresenter = listPresenter;
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

        final PagedListBinding<?, ?> pagedBinding = (PagedListBinding<?, ?>) listPresenter.getListBinding();
        final Pager<?, ?> pager = pagedBinding.pager();
        if (lastItemReached && adapter.isIdle() && pager.hasNext()) {
            adapter.setLoading();
            pager.next();
        }
    }

}
