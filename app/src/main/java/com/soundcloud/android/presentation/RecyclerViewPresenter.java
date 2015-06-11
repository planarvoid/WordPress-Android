package com.soundcloud.android.presentation;


import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.RecyclerView.AdapterDataObserver;
import android.view.View;

import com.soundcloud.android.R;
import com.soundcloud.android.utils.ErrorUtils;

public abstract class RecyclerViewPresenter<ItemT> extends CollectionViewPresenter<ItemT> {

    private RecyclerView recyclerView;
    private LinearLayoutManager linearLayoutManager;
    private AdapterDataObserver emptyViewObserver;

    protected RecyclerViewPresenter(SwipeRefreshAttacher swipeRefreshAttacher) {
        super(swipeRefreshAttacher);
    }

    protected LinearLayoutManager getLinearLayoutManager() {
        return linearLayoutManager;
    }

    public RecyclerView getRecyclerView() {
        return recyclerView;
    }

    @Override
    protected void onCreateCollectionView(Fragment fragment, View view, Bundle savedInstanceState) {
        final CollectionBinding<ItemT> collectionBinding = getBinding();

        setupRecyclerView(fragment, view);

        setupDividers(view);

        final RecyclerItemAdapter adapter = setupAdapter(collectionBinding);

        setupEmptyView(adapter);
    }

    private void setupEmptyView(RecyclerItemAdapter adapter) {
        emptyViewObserver = createEmptyViewObserver();
        adapter.registerAdapterDataObserver(emptyViewObserver);
        updateEmptyViewVisibility();
    }

    private void updateEmptyViewVisibility() {
        final boolean empty = getBinding().adapter().isEmpty();
        getRecyclerView().setVisibility(empty ? View.GONE : View.VISIBLE);
        getEmptyView().setVisibility(empty ? View.VISIBLE : View.GONE);
    }

    private void setupRecyclerView(Fragment fragment, View view) {
        this.recyclerView = (RecyclerView) view.findViewById(R.id.recycler_view);
        if (this.recyclerView == null) {
            throw new IllegalStateException("Expected to find RecyclerView with ID R.id.recycler_view");
        }

        linearLayoutManager = new LinearLayoutManager(fragment.getActivity());
        recyclerView.setLayoutManager(linearLayoutManager);
    }

    private RecyclerItemAdapter setupAdapter(CollectionBinding<ItemT> collectionBinding) {
        final RecyclerItemAdapter adapter = (RecyclerItemAdapter) collectionBinding.adapter();
        if (!(collectionBinding.adapter() instanceof RecyclerItemAdapter)) {
            throw new IllegalArgumentException("Adapter must be an " + RecyclerItemAdapter.class);
        }
        recyclerView.setAdapter(adapter);
        adapter.setOnItemClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                final int adapterPosition = recyclerView.getChildAdapterPosition(view);
                if (adapterPosition >= 0 && adapterPosition < adapter.getItemCount()) {
                    onItemClicked(view, adapterPosition);
                } else {
                    ErrorUtils.handleSilentException(new IllegalArgumentException("Invalid recycler position in click handler " + adapterPosition));
                }
            }
        });
        if (collectionBinding instanceof PagedCollectionBinding) {
            configurePagedListAdapter((PagedCollectionBinding<ItemT, ?>) collectionBinding);
        }
        return adapter;
    }

    private void setupDividers(View view) {
        Drawable divider = view.getResources().getDrawable(R.drawable.divider_list_grey);
        int dividerHeight = view.getResources().getDimensionPixelSize(R.dimen.divider_horizontal_height);
        recyclerView.addItemDecoration(new DividerItemDecoration(divider, dividerHeight));
    }

    @Override
    public void onDestroyView(Fragment fragment) {
        recyclerView.clearOnScrollListeners();
        recyclerView.getAdapter().unregisterAdapterDataObserver(emptyViewObserver);
        recyclerView.setAdapter(null);
        recyclerView = null;
        super.onDestroyView(fragment);
    }

    private void configurePagedListAdapter(final PagedCollectionBinding<ItemT, ?> binding) {
        final PagingAwareAdapter<ItemT> adapter = binding.adapter();
        recyclerView.addOnScrollListener(new PagingRecyclerScrollListener(this, adapter, linearLayoutManager));
        adapter.setOnErrorRetryListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                adapter.setLoading();
                retryWith(binding.fromCurrentPage());
            }
        });
    }

    private AdapterDataObserver createEmptyViewObserver() {
        return new RecyclerView.AdapterDataObserver() {
            @Override
            public void onItemRangeInserted(int positionStart, int itemCount) {
                updateEmptyViewVisibility();
            }

            @Override
            public void onItemRangeRemoved(int positionStart, int itemCount) {
                updateEmptyViewVisibility();
            }

            @Override
            public void onChanged() {
                updateEmptyViewVisibility();
            }
        };
    }
}
