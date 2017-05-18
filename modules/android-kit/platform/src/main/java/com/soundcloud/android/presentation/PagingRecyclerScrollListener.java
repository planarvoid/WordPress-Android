package com.soundcloud.android.presentation;

import com.soundcloud.rx.Pager;

import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.StaggeredGridLayoutManager;

class PagingRecyclerScrollListener extends RecyclerView.OnScrollListener {

    private final CollectionViewPresenter presenter;
    private final PagingAwareAdapter<?> adapter;

    private final RecyclerView.LayoutManager layoutManager;
    private final int numColumns;

    PagingRecyclerScrollListener(CollectionViewPresenter presenter, PagingAwareAdapter<?> adapter,
                                 RecyclerView.LayoutManager layoutManager, int numColumns) {
        this.presenter = presenter;
        this.adapter = adapter;
        this.layoutManager = layoutManager;
        this.numColumns = numColumns;
    }

    @Override
    public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
        onScroll(findFirstVisibleItemPosition(), layoutManager.getChildCount(), layoutManager.getItemCount());
    }

    private void onScroll(int firstVisibleItem, int visibleItemCount, int totalItemCount) {
        int lookAheadSize = visibleItemCount * 2;
        boolean lastItemReached = totalItemCount > 0 && (totalItemCount - lookAheadSize <= firstVisibleItem);

        final PagedCollectionBinding<?, ?, ?> pagedBinding = (PagedCollectionBinding<?, ?, ?>) presenter.getBinding();
        final Pager<?> pager = pagedBinding.pager();
        if (lastItemReached && adapter.isIdle() && pager.hasNext()) {
            adapter.setLoading();
            pager.next();
        }
    }

    private int findFirstVisibleItemPosition() {
        if (layoutManager instanceof StaggeredGridLayoutManager) {
            final int[] gridSpans = new int[numColumns];
            return ((StaggeredGridLayoutManager) layoutManager).findFirstVisibleItemPositions(gridSpans)[0];
        } else if (layoutManager instanceof LinearLayoutManager) {
            return ((LinearLayoutManager) layoutManager).findFirstVisibleItemPosition();
        } else {
            throw new IllegalArgumentException("Unknown LayoutManager type: " +
                    layoutManager.getClass().getSimpleName());
        }
    }
}
