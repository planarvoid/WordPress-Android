package com.soundcloud.android.presentation;

import static com.soundcloud.java.checks.Preconditions.checkState;

import com.soundcloud.android.image.ImageOperations;
import org.jetbrains.annotations.Nullable;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.View;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.ListAdapter;
import android.widget.ListView;

public abstract class ListPresenter<ItemT> extends CollectionViewPresenter<ItemT> {

    private final ImageOperations imageOperations;

    private AbsListView listView;
    private AbsListView.OnScrollListener scrollListener;
    @Nullable private ListHeaderPresenter headerPresenter;

    public ListPresenter(ImageOperations imageOperations, SwipeRefreshAttacher swipeRefreshAttacher) {
        super(swipeRefreshAttacher);
        this.imageOperations = imageOperations;
    }

    protected void setHeaderPresenter(@Nullable ListHeaderPresenter headerPresenter) {
        this.headerPresenter = headerPresenter;
    }

    protected void setScrollListener(AbsListView.OnScrollListener scrollListener) {
        this.scrollListener = scrollListener;
    }

    public AbsListView getListView() {
        return listView;
    }

    @Override
    protected void onCreateCollectionView(Fragment fragment, View view, @Nullable Bundle savedInstanceState) {
        final CollectionBinding<ItemT> collectionBinding = getBinding();
        checkState(collectionBinding.adapter() instanceof ListAdapter, "Adapter must be an " + ListAdapter.class);

        this.listView = (AbsListView) view.findViewById(android.R.id.list);
        if (this.listView == null) {
            throw new IllegalStateException("Expected to find ListView with ID android.R.id.list");
        }
        listView.setEmptyView(getEmptyView());
        configureScrollListener();

        if (headerPresenter != null) {
            headerPresenter.onViewCreated(view, (ListView) listView);
        }
        listView.setAdapter((ListAdapter) collectionBinding.adapter());
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
                onItemClicked(view, position);
            }
        });
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
        final CollectionBinding<ItemT> collectionBinding = getBinding();
        if (collectionBinding instanceof PagedCollectionBinding) {
            configurePagedListAdapter((PagedCollectionBinding<ItemT, ?>) collectionBinding);
        }

        listView.setOnScrollListener(scrollListener);
    }

    private void configurePagedListAdapter(final PagedCollectionBinding<ItemT, ?> binding) {
        final PagingAwareAdapter<ItemT> adapter = binding.adapter();
        scrollListener = new PagingListScrollListener(this, adapter, scrollListener);
        adapter.setOnErrorRetryListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                adapter.setLoading();
                retryWith(binding.fromCurrentPage());
            }
        });
    }
}
