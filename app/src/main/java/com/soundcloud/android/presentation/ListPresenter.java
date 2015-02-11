package com.soundcloud.android.presentation;

import com.soundcloud.android.R;
import com.soundcloud.android.image.ImageOperations;
import com.soundcloud.android.rx.observers.DefaultSubscriber;
import com.soundcloud.android.view.MultiSwipeRefreshLayout;
import com.soundcloud.android.view.adapters.EndlessAdapter;
import com.soundcloud.android.view.adapters.ItemAdapter;
import org.jetbrains.annotations.Nullable;
import rx.subscriptions.CompositeSubscription;

import android.annotation.TargetApi;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.widget.SwipeRefreshLayout;
import android.view.View;
import android.widget.AbsListView;
import android.widget.GridView;
import android.widget.ListAdapter;
import android.widget.ListView;

public abstract class ListPresenter<DataT, ItemT> extends EmptyViewPresenter {

    private final ImageOperations imageOperations;
    private final PullToRefreshWrapper refreshWrapper;

    private AbsListView listView;
    private AbsListView.OnScrollListener scrollListener;

    private ListBinding<DataT, ItemT> listBinding;
    private ListBinding<DataT, ItemT> refreshBinding;
    private CompositeSubscription viewLifeCycle;

    public ListPresenter(ImageOperations imageOperations, PullToRefreshWrapper pullToRefreshWrapper) {
        this.imageOperations = imageOperations;
        this.refreshWrapper = pullToRefreshWrapper;
    }

    public void setScrollListener(AbsListView.OnScrollListener scrollListener) {
        this.scrollListener = scrollListener;
    }

    protected abstract ListBinding<DataT, ItemT> onBuildListBinding();

    protected ListBinding<DataT, ItemT> onBuildRefreshBinding() {
        return onBuildListBinding();
    }

    protected abstract void onSubscribeListBinding(ListBinding<DataT, ItemT> listBinding);

    protected ListBinding<DataT, ItemT> getListBinding() {
        return listBinding;
    }

    protected AbsListView getListView() {
        return listView;
    }

    @Override
    public void onCreate(Fragment fragment, @Nullable Bundle bundle) {
        super.onCreate(fragment, bundle);
        rebuildListBinding();
    }

    protected ListBinding<DataT, ItemT> rebuildListBinding() {
        resetListBindingTo(onBuildListBinding());
        return listBinding;
    }

    private void resetListBindingTo(ListBinding<DataT, ItemT> listBinding) {
        this.listBinding = listBinding;
        this.listBinding.getSource().subscribe(listBinding.getAdapter());
    }

    @Override
    protected void onRetry() {
        retryWith(onBuildListBinding());
    }

    private void retryWith(ListBinding<DataT, ItemT> listBinding) {
        resetListBindingTo(listBinding);
        subscribeListViewObservers();
        listBinding.connect();
    }

    private void subscribeListViewObservers() {
        listBinding.clearViewObservers();
        onSubscribeListBinding(listBinding);
        listBinding.addViewObserver(new EmptyViewSubscriber());
        if (viewLifeCycle != null) {
            viewLifeCycle.unsubscribe();
        }
        viewLifeCycle = new CompositeSubscription(listBinding.subscribeViewObservers());
    }

    @Override
    public void onViewCreated(Fragment fragment, View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(fragment, view, savedInstanceState);
        this.listView = (AbsListView) view.findViewById(android.R.id.list);
        if (this.listView == null) {
            throw new IllegalStateException("Expected to find ListView with ID android.R.id.list");
        }
        listView.setEmptyView(getEmptyView());
        configureScrollListener();
        compatSetAdapter(getListBinding().getAdapter());

        MultiSwipeRefreshLayout refreshLayout = (MultiSwipeRefreshLayout) view.findViewById(R.id.str_layout);
        refreshWrapper.attach(refreshLayout, new PullToRefreshListener());

        subscribeListViewObservers();
    }

    @Override
    public void onDestroyView(Fragment fragment) {
        viewLifeCycle.unsubscribe();
        listBinding.clearViewObservers();
        refreshWrapper.detach();
        compatSetAdapter(null);
        listView = null;
        super.onDestroyView(fragment);
    }

    @Override
    public void onDestroy(Fragment fragment) {
        listBinding.disconnect();
        super.onDestroy(fragment);
    }

    private void configureScrollListener() {
        if (scrollListener == null) {
            scrollListener = imageOperations.createScrollPauseListener(false, true);
        } else {
            scrollListener = imageOperations.createScrollPauseListener(false, true, scrollListener);
        }
        if (getListBinding().isPaged()) {
            configurePagedListAdapter();
        }

        listView.setOnScrollListener(scrollListener);
    }

    private void configurePagedListAdapter() {
        final EndlessAdapter adapter = (EndlessAdapter) listBinding.getAdapter();
        scrollListener = new PagingScrollListener(this, adapter, scrollListener);
        adapter.setOnErrorRetryListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                adapter.setLoading();
                retryWith(listBinding.resetFromCurrentPage());
            }
        });
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    private void compatSetAdapter(@Nullable ListAdapter adapter) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            listView.setAdapter(adapter);
        } else if (listView instanceof GridView) {
            final GridView gridView = (GridView) listView;
            gridView.setAdapter(adapter);
        } else if (listView instanceof ListView) {
            final ListView listView = (ListView) this.listView;
            listView.setAdapter(adapter);
        }
    }

    private final class ListRefreshSubscriber extends DefaultSubscriber<Iterable<ItemT>> {

        @Override
        public void onNext(Iterable<ItemT> collection) {
            final ItemAdapter<ItemT> adapter = listBinding.getAdapter();
            adapter.clear();

            resetListBindingTo(refreshBinding);
            subscribeListViewObservers();

            refreshWrapper.setRefreshing(false);
            unsubscribe();
        }

        @Override
        public void onError(Throwable error) {
            error.printStackTrace();
            refreshWrapper.setRefreshing(false);
            super.onError(error);
        }
    }

    private final class PullToRefreshListener implements SwipeRefreshLayout.OnRefreshListener {
        @Override
        public void onRefresh() {
            refreshBinding = onBuildRefreshBinding();
            refreshBinding.getSource().subscribe(new ListRefreshSubscriber());
            refreshBinding.connect();
        }
    }
}