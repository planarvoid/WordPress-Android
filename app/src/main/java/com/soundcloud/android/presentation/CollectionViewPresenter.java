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

    private CollectionBinding<ItemT> collectionBinding;
    private CollectionBinding<ItemT> refreshBinding;
    private CompositeSubscription viewLifeCycle;

    protected CollectionViewPresenter(PullToRefreshWrapper pullToRefreshWrapper) {
        this.refreshWrapper = pullToRefreshWrapper;
    }

    protected abstract CollectionBinding<ItemT> onBuildBinding(Bundle fragmentArgs);

    protected CollectionBinding<ItemT> onRefreshBinding() {
        return onBuildBinding(fragmentArgs);
    }

    protected void onSubscribeBinding(CollectionBinding<ItemT> collectionBinding, CompositeSubscription viewLifeCycle) {
        // NOP by default
    }

    protected CollectionBinding<ItemT> getBinding() {
        return collectionBinding;
    }

    @Override
    public void onCreate(Fragment fragment, @Nullable Bundle bundle) {
        Log.d(TAG, "onCreate");
        super.onCreate(fragment, bundle);
        this.fragmentArgs = fragment.getArguments();
        rebuildBinding(fragmentArgs);
    }

    protected CollectionBinding<ItemT> rebuildBinding(Bundle fragmentArgs) {
        Log.d(TAG, "rebinding collection");
        resetBindingTo(onBuildBinding(fragmentArgs));
        return collectionBinding;
    }

    private void resetBindingTo(CollectionBinding<ItemT> collectionBinding) {
        this.collectionBinding = collectionBinding;
        this.collectionBinding.items().subscribe(collectionBinding.adapter());
    }

    @Override
    protected void onRetry() {
        retryWith(onBuildBinding(fragmentArgs));
    }

    protected void retryWith(CollectionBinding<ItemT> collectionBinding) {
        Log.d(TAG, "retrying collection");
        resetBindingTo(collectionBinding);
        subscribeBinding();
        collectionBinding.connect();
    }

    private void subscribeBinding() {
        Log.d(TAG, "subscribing view observers");
        if (viewLifeCycle != null) {
            viewLifeCycle.unsubscribe();
        }
        viewLifeCycle = new CompositeSubscription();
        viewLifeCycle.add(collectionBinding.items().subscribe(new EmptyViewSubscriber()));
        onSubscribeBinding(collectionBinding, viewLifeCycle);
    }

    @Override
    public void onViewCreated(Fragment fragment, View view, @Nullable Bundle savedInstanceState) {
        Log.d(TAG, "onViewCreated");
        super.onViewCreated(fragment, view, savedInstanceState);

        onCreateCollectionView(fragment, view, savedInstanceState);

        MultiSwipeRefreshLayout refreshLayout = (MultiSwipeRefreshLayout) view.findViewById(R.id.str_layout);
        refreshWrapper.attach(refreshLayout, new PullToRefreshListener());

        subscribeBinding();
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
        collectionBinding.disconnect();
        super.onDestroy(fragment);
    }

    private final class RefreshSubscriber extends DefaultSubscriber<Iterable<ItemT>> {

        @Override
        public void onNext(Iterable<ItemT> collection) {
            Log.d(TAG, "refresh complete");
            final ReactiveItemAdapter<ItemT> adapter = collectionBinding.adapter();
            adapter.clear();

            resetBindingTo(refreshBinding);
            subscribeBinding();

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
            refreshBinding = onRefreshBinding();
            refreshBinding.items().subscribe(new RefreshSubscriber());
            refreshBinding.connect();
        }
    }
}