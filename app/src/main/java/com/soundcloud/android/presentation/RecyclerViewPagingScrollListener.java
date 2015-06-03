package com.soundcloud.android.presentation;

import com.soundcloud.android.rx.Pager;

import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;


final class RecyclerViewPagingScrollListener extends RecyclerView.OnScrollListener {

    private final CollectionViewPresenter presenter;
    private final PagingAwareAdapter<?> adapter;

    private final LinearLayoutManager layoutManager;

    RecyclerViewPagingScrollListener(CollectionViewPresenter presenter, PagingAwareAdapter<?> adapter,
                                     LinearLayoutManager layoutManager) {
        this.presenter = presenter;
        this.adapter = adapter;
        this.layoutManager = layoutManager;
    }

    @Override
    public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
        onScroll(layoutManager.findFirstVisibleItemPosition(), layoutManager.getChildCount(),
                layoutManager.getItemCount());

    }

    private void onScroll(int firstVisibleItem, int visibleItemCount, int totalItemCount) {
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
