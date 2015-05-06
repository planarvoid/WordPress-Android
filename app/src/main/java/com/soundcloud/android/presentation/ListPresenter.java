package com.soundcloud.android.presentation;

import com.soundcloud.android.R;
import com.soundcloud.android.image.ImageOperations;
import com.soundcloud.android.rx.observers.DefaultSubscriber;
import com.soundcloud.android.utils.Log;
import com.soundcloud.android.view.MultiSwipeRefreshLayout;
import com.soundcloud.android.view.adapters.ItemAdapter;
import com.soundcloud.android.view.adapters.PagingItemAdapter;
import org.jetbrains.annotations.Nullable;
import rx.subscriptions.CompositeSubscription;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.widget.SwipeRefreshLayout;
import android.view.View;
import android.widget.AbsListView;
import android.widget.ListView;

public abstract class ListPresenter<ItemT> extends EmptyViewPresenter {

    private static final String TAG = "ListPresenter";

    private final ImageOperations imageOperations;
    private final PullToRefreshWrapper refreshWrapper;

    private Bundle fragmentArgs;
    private AbsListView listView;
    private AbsListView.OnScrollListener scrollListener;
    @Nullable private ListHeaderPresenter headerPresenter;

    private ListBinding<ItemT> listBinding;
    private ListBinding<ItemT> refreshBinding;
    private CompositeSubscription viewLifeCycle;

    public ListPresenter(ImageOperations imageOperations, PullToRefreshWrapper pullToRefreshWrapper) {
        this.imageOperations = imageOperations;
        this.refreshWrapper = pullToRefreshWrapper;
    }

    protected void setHeaderPresenter(@Nullable ListHeaderPresenter headerPresenter) {
        this.headerPresenter = headerPresenter;
    }

    protected void setScrollListener(AbsListView.OnScrollListener scrollListener) {
        this.scrollListener = scrollListener;
    }

    protected abstract ListBinding<ItemT> onBuildListBinding(Bundle fragmentArgs);

    protected ListBinding<ItemT> onBuildRefreshBinding() {
        return onBuildListBinding(fragmentArgs);
    }

    protected abstract void onSubscribeListBinding(ListBinding<ItemT> listBinding);

    protected ListBinding<ItemT> getListBinding() {
        return listBinding;
    }

    protected AbsListView getListView() {
        return listView;
    }

    @Override
    public void onCreate(Fragment fragment, @Nullable Bundle bundle) {
        Log.d(TAG, "onCreate");
        super.onCreate(fragment, bundle);
        this.fragmentArgs = fragment.getArguments();
        rebuildListBinding(fragmentArgs);
    }

    protected ListBinding<ItemT> rebuildListBinding(Bundle fragmentArgs) {
        Log.d(TAG, "rebinding list");
        resetListBindingTo(onBuildListBinding(fragmentArgs));
        return listBinding;
    }

    private void resetListBindingTo(ListBinding<ItemT> listBinding) {
        this.listBinding = listBinding;
        this.listBinding.getListItems().subscribe(listBinding.getAdapter());
    }

    @Override
    protected void onRetry() {
        retryWith(onBuildListBinding(fragmentArgs));
    }

    private void retryWith(ListBinding<ItemT> listBinding) {
        Log.d(TAG, "retrying list");
        resetListBindingTo(listBinding);
        subscribeListViewObservers();
        listBinding.connect();
    }

    private void subscribeListViewObservers() {
        Log.d(TAG, "subscribing view observers");
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
        Log.d(TAG, "onViewCreated");
        super.onViewCreated(fragment, view, savedInstanceState);
        this.listView = (AbsListView) view.findViewById(android.R.id.list);
        if (this.listView == null) {
            throw new IllegalStateException("Expected to find ListView with ID android.R.id.list");
        }
        listView.setEmptyView(getEmptyView());
        configureScrollListener();

        if (headerPresenter != null) {
            headerPresenter.onViewCreated(view, (ListView) listView);
        }
        listView.setAdapter(getListBinding().getAdapter());

        MultiSwipeRefreshLayout refreshLayout = (MultiSwipeRefreshLayout) view.findViewById(R.id.str_layout);
        refreshWrapper.attach(refreshLayout, new PullToRefreshListener());

        subscribeListViewObservers();
    }

    @Override
    public void onDestroyView(Fragment fragment) {
        Log.d(TAG, "onDestroyView");
        viewLifeCycle.unsubscribe();
        listBinding.clearViewObservers();
        refreshWrapper.detach();
        listView.setAdapter(null);
        listView = null;
        super.onDestroyView(fragment);
    }

    @Override
    public void onDestroy(Fragment fragment) {
        Log.d(TAG, "onDestroy");
        listBinding.disconnect();
        super.onDestroy(fragment);
    }

    private void configureScrollListener() {
        if (scrollListener == null) {
            scrollListener = imageOperations.createScrollPauseListener(false, true);
        } else {
            scrollListener = imageOperations.createScrollPauseListener(false, true, scrollListener);
        }
        if (listBinding instanceof PagedListBinding) {
            configurePagedListAdapter((PagedListBinding<ItemT, ?>) listBinding);
        }

        listView.setOnScrollListener(scrollListener);
    }

    private void configurePagedListAdapter(final PagedListBinding<ItemT, ?> binding) {
        final PagingItemAdapter<ItemT> adapter = binding.getAdapter();
        scrollListener = new PagingScrollListener(this, adapter, scrollListener);
        adapter.setOnErrorRetryListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                adapter.setLoading();
                retryWith(binding.fromCurrentPage());
            }
        });
    }

    private final class ListRefreshSubscriber extends DefaultSubscriber<Iterable<ItemT>> {

        @Override
        public void onNext(Iterable<ItemT> collection) {
            Log.d(TAG, "refresh complete");
            final ItemAdapter<ItemT> adapter = listBinding.getAdapter();
            adapter.clear();

            resetListBindingTo(refreshBinding);
            subscribeListViewObservers();

            refreshWrapper.setRefreshing(false);
            unsubscribe();
        }

        @Override
        public void onError(Throwable error) {
            Log.d(TAG, "refresh failed");
            error.printStackTrace();
            refreshWrapper.setRefreshing(false);
            super.onError(error);
        }
    }

    private final class PullToRefreshListener implements SwipeRefreshLayout.OnRefreshListener {
        @Override
        public void onRefresh() {
            Log.d(TAG, "refreshing list");
            refreshBinding = onBuildRefreshBinding();
            refreshBinding.getListItems().subscribe(new ListRefreshSubscriber());
            refreshBinding.connect();
        }
    }
}