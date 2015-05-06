package com.soundcloud.android.presentation;

import com.soundcloud.android.R;
import com.soundcloud.android.image.ImageOperations;
import com.soundcloud.android.rx.observers.DefaultSubscriber;
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

public abstract class NewListPresenter<ItemT> extends EmptyViewPresenter {

    private final ImageOperations imageOperations;
    private final PullToRefreshWrapper refreshWrapper;

    private Bundle fragmentArgs;
    private AbsListView listView;
    private AbsListView.OnScrollListener scrollListener;
    @Nullable private ListHeaderPresenter headerPresenter;

    private NewListBinding<ItemT> listBinding;
    private NewListBinding<ItemT> refreshBinding;
    private CompositeSubscription viewLifeCycle;

    public NewListPresenter(ImageOperations imageOperations, PullToRefreshWrapper pullToRefreshWrapper) {
        this.imageOperations = imageOperations;
        this.refreshWrapper = pullToRefreshWrapper;
    }

    protected void setHeaderPresenter(@Nullable ListHeaderPresenter headerPresenter) {
        this.headerPresenter = headerPresenter;
    }

    protected void setScrollListener(AbsListView.OnScrollListener scrollListener) {
        this.scrollListener = scrollListener;
    }

    protected abstract NewListBinding<ItemT> onBuildListBinding(Bundle fragmentArgs);

    protected NewListBinding<ItemT> onBuildRefreshBinding() {
        return onBuildListBinding(fragmentArgs);
    }

    protected abstract void onSubscribeListBinding(NewListBinding<ItemT> listBinding);

    protected NewListBinding<ItemT> getListBinding() {
        return listBinding;
    }

    protected AbsListView getListView() {
        return listView;
    }

    @Override
    public void onCreate(Fragment fragment, @Nullable Bundle bundle) {
        super.onCreate(fragment, bundle);
        this.fragmentArgs = fragment.getArguments();
        rebuildListBinding(fragmentArgs);
    }

    protected NewListBinding<ItemT> rebuildListBinding(Bundle fragmentArgs) {
        resetListBindingTo(onBuildListBinding(fragmentArgs));
        return listBinding;
    }

    private void resetListBindingTo(NewListBinding<ItemT> listBinding) {
        this.listBinding = listBinding;
        this.listBinding.getSource().subscribe(listBinding.getAdapter());
    }

    @Override
    protected void onRetry() {
        retryWith(onBuildListBinding(fragmentArgs));
    }

    private void retryWith(NewListBinding<ItemT> listBinding) {
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
        viewLifeCycle.unsubscribe();
        listBinding.clearViewObservers();
        refreshWrapper.detach();
        listView.setAdapter(null);
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
        final PagingItemAdapter adapter = (PagingItemAdapter) listBinding.getAdapter();
        scrollListener = new NewPagingScrollListener(this, adapter, scrollListener);
        adapter.setOnErrorRetryListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                adapter.setLoading();
                retryWith(listBinding.resetFromCurrentPage());
            }
        });
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