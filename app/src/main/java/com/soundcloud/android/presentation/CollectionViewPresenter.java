package com.soundcloud.android.presentation;

import com.soundcloud.android.R;
import com.soundcloud.android.rx.observers.DefaultSubscriber;
import com.soundcloud.android.utils.Log;
import com.soundcloud.android.view.MultiSwipeRefreshLayout;
import com.soundcloud.android.view.adapters.ReactiveItemAdapter;
import org.jetbrains.annotations.Nullable;
import rx.subscriptions.CompositeSubscription;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.widget.SwipeRefreshLayout;
import android.view.View;

abstract class CollectionViewPresenter<ItemT> extends EmptyViewPresenter {

    private static final String TAG = "CollectionViewPresenter";

    private final PullToRefreshWrapper refreshWrapper;

    private Bundle fragmentArgs;

    private ListBinding<ItemT> listBinding;
    private ListBinding<ItemT> refreshBinding;
    private CompositeSubscription viewLifeCycle;

    protected CollectionViewPresenter(PullToRefreshWrapper pullToRefreshWrapper) {
        this.refreshWrapper = pullToRefreshWrapper;
    }

    protected abstract ListBinding<ItemT> onBuildListBinding(Bundle fragmentArgs);

    protected ListBinding<ItemT> onBuildRefreshBinding() {
        return onBuildListBinding(fragmentArgs);
    }

    protected void onSubscribeListBinding(ListBinding<ItemT> listBinding, CompositeSubscription viewLifeCycle) {
        // NOP by default
    }

    protected ListBinding<ItemT> getListBinding() {
        return listBinding;
    }

    @Override
    public void onCreate(Fragment fragment, @Nullable Bundle bundle) {
        Log.d(TAG, "onCreate");
        super.onCreate(fragment, bundle);
        this.fragmentArgs = fragment.getArguments();
        rebuildListBinding(fragmentArgs);
    }

    protected ListBinding<ItemT> rebuildListBinding(Bundle fragmentArgs) {
        Log.d(TAG, "rebinding collection");
        resetListBindingTo(onBuildListBinding(fragmentArgs));
        return listBinding;
    }

    private void resetListBindingTo(ListBinding<ItemT> listBinding) {
        this.listBinding = listBinding;
        this.listBinding.items().subscribe(listBinding.adapter());
    }

    @Override
    protected void onRetry() {
        retryWith(onBuildListBinding(fragmentArgs));
    }

    protected void retryWith(ListBinding<ItemT> listBinding) {
        Log.d(TAG, "retrying collection");
        resetListBindingTo(listBinding);
        subscribeListBinding();
        listBinding.connect();
    }

    private void subscribeListBinding() {
        Log.d(TAG, "subscribing view observers");
        if (viewLifeCycle != null) {
            viewLifeCycle.unsubscribe();
        }
        viewLifeCycle = new CompositeSubscription();
        viewLifeCycle.add(listBinding.items().subscribe(new EmptyViewSubscriber()));
        onSubscribeListBinding(listBinding, viewLifeCycle);
    }

    @Override
    public void onViewCreated(Fragment fragment, View view, @Nullable Bundle savedInstanceState) {
        Log.d(TAG, "onViewCreated");
        super.onViewCreated(fragment, view, savedInstanceState);

        onCreateCollectionView(fragment, view, savedInstanceState);

        MultiSwipeRefreshLayout refreshLayout = (MultiSwipeRefreshLayout) view.findViewById(R.id.str_layout);
        refreshWrapper.attach(refreshLayout, new PullToRefreshListener());

        subscribeListBinding();
    }

    protected abstract void onCreateCollectionView(Fragment fragment, View view, Bundle savedInstanceState);

    @Override
    public void onDestroyView(Fragment fragment) {
        Log.d(TAG, "onDestroyView");
        viewLifeCycle.unsubscribe();
        refreshWrapper.detach();
        super.onDestroyView(fragment);
    }

    @Override
    public void onDestroy(Fragment fragment) {
        Log.d(TAG, "onDestroy");
        listBinding.disconnect();
        super.onDestroy(fragment);
    }

    private final class ListRefreshSubscriber extends DefaultSubscriber<Iterable<ItemT>> {

        @Override
        public void onNext(Iterable<ItemT> collection) {
            Log.d(TAG, "refresh complete");
            final ReactiveItemAdapter<ItemT> adapter = listBinding.adapter();
            adapter.clear();

            resetListBindingTo(refreshBinding);
            subscribeListBinding();

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
            Log.d(TAG, "refreshing collection");
            refreshBinding = onBuildRefreshBinding();
            refreshBinding.items().subscribe(new ListRefreshSubscriber());
            refreshBinding.connect();
        }
    }
}