package com.soundcloud.android.presentation;


import static android.support.v7.widget.RecyclerView.OnScrollListener;

import com.google.common.base.Preconditions;
import com.soundcloud.android.R;
import com.soundcloud.android.image.PauseOnScrollListener;
import com.soundcloud.android.view.adapters.PagingAwareAdapter;
import com.soundcloud.android.view.adapters.RecyclerViewAdapter;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;

public abstract class RecyclerViewPresenter<ItemT> extends CollectionViewPresenter<ItemT> {

    private final PauseOnScrollListener pauseOnScrollListener;
    private RecyclerView recyclerView;
    private RecyclerView.OnScrollListener externalScrollListener;
    private LinearLayoutManager linearLayoutManager;
    private RecyclerView.AdapterDataObserver emptyViewObserver;

    protected RecyclerViewPresenter(PullToRefreshWrapper pullToRefreshWrapper, PauseOnScrollListener pauseOnScrollListener) {
        super(pullToRefreshWrapper);
        this.pauseOnScrollListener = pauseOnScrollListener;
    }

    public void setOnScrollListener(OnScrollListener scrollListener) {
        this.externalScrollListener = scrollListener;
    }

    @Override
    protected void onCreateCollectionView(Fragment fragment, View view, Bundle savedInstanceState) {
        final CollectionBinding<ItemT> collectionBinding = getBinding();
        Preconditions.checkState(collectionBinding.adapter() instanceof RecyclerViewAdapter, "Adapter must be an " + RecyclerViewAdapter.class);

        this.recyclerView = (RecyclerView) view.findViewById(R.id.recycler_view);
        if (this.recyclerView == null) {
            throw new IllegalStateException("Expected to find RecyclerView with ID R.id.recycler_view");
        }

        linearLayoutManager = new LinearLayoutManager(fragment.getActivity());
        recyclerView.setLayoutManager(linearLayoutManager);

        configureScrollListener();

        final RecyclerViewAdapter adapter = (RecyclerViewAdapter) collectionBinding.adapter();
        recyclerView.setAdapter(adapter);
        adapter.setOnItemClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onItemClicked(view, recyclerView.getChildAdapterPosition(view));
            }
        });

        emptyViewObserver = createEmptyViewObserver(adapter);
        adapter.registerAdapterDataObserver(emptyViewObserver);
    }

    @Override
    public void onDestroyView(Fragment fragment) {
        recyclerView.getAdapter().unregisterAdapterDataObserver(emptyViewObserver);
        recyclerView.setAdapter(null);
        recyclerView = null;
        super.onDestroyView(fragment);
    }

    private void configureScrollListener() {
        recyclerView.addOnScrollListener(pauseOnScrollListener);
        if (externalScrollListener != null) {
            recyclerView.addOnScrollListener(externalScrollListener);
        }

        final CollectionBinding<ItemT> collectionBinding = getBinding();
        if (collectionBinding instanceof PagedCollectionBinding) {
            configurePagedListAdapter((PagedCollectionBinding<ItemT, ?>) collectionBinding);
        }
    }

    private void configurePagedListAdapter(final PagedCollectionBinding<ItemT, ?> binding) {
        final PagingAwareAdapter<ItemT> adapter = binding.adapter();
        recyclerView.addOnScrollListener(new RecyclerViewPagingScrollListener(this, adapter, linearLayoutManager));
        adapter.setOnErrorRetryListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                adapter.setLoading();
                retryWith(binding.fromCurrentPage());
            }
        });
    }

    private RecyclerView.AdapterDataObserver createEmptyViewObserver(final RecyclerView.Adapter adapter) {
        return new RecyclerView.AdapterDataObserver() {
            @Override
            public void onItemRangeInserted(int positionStart, int itemCount) {
                configureEmptyView();
            }

            @Override
            public void onItemRangeRemoved(int positionStart, int itemCount) {
                configureEmptyView();
            }

            @Override
            public void onChanged() {
                configureEmptyView();
            }

            private void configureEmptyView() {
                getEmptyView().setVisibility(adapter.getItemCount() > 0 ? View.GONE : View.VISIBLE);
            }
        };
    }

    protected abstract void onItemClicked(View view, int position);
}
