package com.soundcloud.android.presentation;

import com.google.common.base.Preconditions;
import com.soundcloud.android.image.ImageOperations;
import com.soundcloud.android.view.adapters.PagingItemAdapter;
import org.jetbrains.annotations.Nullable;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.View;
import android.widget.AbsListView;
import android.widget.ListAdapter;
import android.widget.ListView;

public abstract class ListPresenter<ItemT> extends CollectionViewPresenter<ItemT> {

    private final ImageOperations imageOperations;

    private AbsListView listView;
    private AbsListView.OnScrollListener scrollListener;
    @Nullable private ListHeaderPresenter headerPresenter;

    public ListPresenter(ImageOperations imageOperations, PullToRefreshWrapper pullToRefreshWrapper) {
        super(pullToRefreshWrapper);
        this.imageOperations = imageOperations;
    }

    protected void setHeaderPresenter(@Nullable ListHeaderPresenter headerPresenter) {
        this.headerPresenter = headerPresenter;
    }

    protected void setScrollListener(AbsListView.OnScrollListener scrollListener) {
        this.scrollListener = scrollListener;
    }

    protected AbsListView getListView() {
        return listView;
    }

    @Override
    protected void onCreateCollectionView(Fragment fragment, View view, @Nullable Bundle savedInstanceState) {
        final ListBinding<ItemT> listBinding = getListBinding();
        Preconditions.checkState(listBinding.adapter() instanceof ListAdapter, "Adapter must be an " + ListAdapter.class);

        this.listView = (AbsListView) view.findViewById(android.R.id.list);
        if (this.listView == null) {
            throw new IllegalStateException("Expected to find ListView with ID android.R.id.list");
        }
        listView.setEmptyView(getEmptyView());
        configureScrollListener();

        if (headerPresenter != null) {
            headerPresenter.onViewCreated(view, (ListView) listView);
        }
        listView.setAdapter((ListAdapter) listBinding.adapter());
    }

    @Override
    public void onDestroyView(Fragment fragment) {
        listView.setAdapter(null);
        listView = null;
        super.onDestroyView(fragment);
    }

    private void configureScrollListener() {
        if (scrollListener == null) {
            scrollListener = imageOperations.createScrollPauseListener(false, true);
        } else {
            scrollListener = imageOperations.createScrollPauseListener(false, true, scrollListener);
        }
        final ListBinding<ItemT> listBinding = getListBinding();
        if (listBinding instanceof PagedListBinding) {
            configurePagedListAdapter((PagedListBinding<ItemT, ?>) listBinding);
        }

        listView.setOnScrollListener(scrollListener);
    }

    private void configurePagedListAdapter(final PagedListBinding<ItemT, ?> binding) {
        final PagingItemAdapter<ItemT> adapter = binding.adapter();
        scrollListener = new PagingScrollListener(this, adapter, scrollListener);
        adapter.setOnErrorRetryListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                adapter.setLoading();
                retryWith(binding.fromCurrentPage());
            }
        });
    }
}
