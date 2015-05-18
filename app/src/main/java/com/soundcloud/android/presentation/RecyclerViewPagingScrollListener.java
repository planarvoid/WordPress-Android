package com.soundcloud.android.presentation;

import com.soundcloud.android.view.adapters.PagingAwareAdapter;
import rx.android.Pager;

import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;


final class RecyclerViewPagingScrollListener extends RecyclerView.OnScrollListener {

    private final CollectionViewPresenter presenter;
    private final PagingAwareAdapter<?> adapter;

    private final LinearLayoutManager layoutManager;
    private final RecyclerView.OnScrollListener recyclerViewDelegate;

    RecyclerViewPagingScrollListener(CollectionViewPresenter presenter, PagingAwareAdapter<?> adapter,
                                     LinearLayoutManager layoutManager, RecyclerView.OnScrollListener listenerDelegate) {
        this.presenter = presenter;
        this.adapter = adapter;
        this.layoutManager = layoutManager;
        this.recyclerViewDelegate = listenerDelegate;
    }

    @Override
    public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
        recyclerViewDelegate.onScrollStateChanged(recyclerView, newState);
    }

    @Override
    public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
        recyclerViewDelegate.onScrolled(recyclerView, dx, dy);
        onScroll(layoutManager.findFirstVisibleItemPosition(), layoutManager.getChildCount(),
                layoutManager.getItemCount());

    }

    void onScroll(int firstVisibleItem, int visibleItemCount, int totalItemCount) {
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
