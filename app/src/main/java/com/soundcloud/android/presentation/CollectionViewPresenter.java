package com.soundcloud.android.presentation;

import com.soundcloud.android.view.EmptyView;
import com.soundcloud.android.view.MultiSwipeRefreshLayout;
import com.soundcloud.lightcycle.LightCycleBinder;
import com.soundcloud.lightcycle.SupportFragmentLightCycleDispatcher;
import rx.Subscriber;
import rx.subscriptions.CompositeSubscription;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.widget.SwipeRefreshLayout;
import android.util.Log;
import android.view.View;

abstract class CollectionViewPresenter<ItemT>
        extends SupportFragmentLightCycleDispatcher<Fragment> {

    private static final String TAG = "CollectionViewPresenter";

    private final SwipeRefreshAttacher swipeRefreshAttacher;

    private Bundle fragmentArgs;
    private CollectionBinding<ItemT> collectionBinding;
    private CollectionBinding<ItemT> refreshBinding;
    private CompositeSubscription viewLifeCycle;

    private EmptyView emptyView;
    private EmptyView.Status emptyViewStatus = EmptyView.Status.WAITING;

    protected CollectionViewPresenter(SwipeRefreshAttacher swipeRefreshAttacher) {
        this.swipeRefreshAttacher = swipeRefreshAttacher;
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

    public EmptyView getEmptyView() {
        return emptyView;
    }

    @Override
    public void onCreate(Fragment fragment, @Nullable Bundle bundle) {
        Log.d(TAG, "onCreate");
        LightCycleBinder.bind(this);
        super.onCreate(fragment, bundle);
        this.fragmentArgs = fragment.getArguments();
        rebuildBinding(fragmentArgs);
    }

    protected CollectionBinding<ItemT> rebuildBinding(@Nullable Bundle fragmentArgs) {
        Log.d(TAG, "rebinding collection");
        resetBindingTo(onBuildBinding(fragmentArgs));
        return collectionBinding;
    }

    private void resetBindingTo(CollectionBinding<ItemT> collectionBinding) {
        this.collectionBinding = collectionBinding;
        this.collectionBinding.items().subscribe(collectionBinding.adapter());
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
    public void onViewCreated(Fragment fragment, View view, Bundle savedInstanceState) {
        Log.d(TAG, "onViewCreated");
        super.onViewCreated(fragment, view, savedInstanceState);

        setupEmptyView(view);
        onCreateCollectionView(fragment, view, savedInstanceState);

        if (fragment instanceof RefreshableScreen) {
            RefreshableScreen refreshableScreen = ((RefreshableScreen) fragment);
            attachSwipeToRefresh(refreshableScreen.getRefreshLayout(), refreshableScreen.getRefreshableViews());
        }

        subscribeBinding();
    }

    private void setupEmptyView(View view) {
        emptyView = (EmptyView) view.findViewById(android.R.id.empty);
        emptyView.setStatus(emptyViewStatus);
        emptyView.setOnRetryListener(new EmptyView.RetryListener() {
            @Override
            public void onEmptyViewRetry() {
                updateEmptyViewStatus(EmptyView.Status.WAITING);
                retryWith(onBuildBinding(fragmentArgs));
            }
        });
    }

    private void updateEmptyViewStatus(EmptyView.Status status) {
        this.emptyViewStatus = status;
        emptyView.setStatus(status);
    }

    protected abstract void onCreateCollectionView(Fragment fragment, View view, Bundle savedInstanceState);

    protected abstract void onItemClicked(View view, int position);

    protected abstract EmptyView.Status handleError(Throwable error);

    @Override
    public void onDestroyView(Fragment fragment) {
        Log.d(TAG, "onDestroyView");
        viewLifeCycle.unsubscribe();
        detachSwipeToRefresh();
        super.onDestroyView(fragment);
    }

    protected void attachSwipeToRefresh(MultiSwipeRefreshLayout refreshLayout, View... refreshableViews) {
        swipeRefreshAttacher.attach(new RefreshListener(), refreshLayout, refreshableViews);
        if (refreshBinding != null) {
            refreshLayout.setRefreshing(true);
        }
    }

    protected void detachSwipeToRefresh() {
        swipeRefreshAttacher.setRefreshing(false);
        swipeRefreshAttacher.detach();
    }

    @Override
    public void onDestroy(Fragment fragment) {
        Log.d(TAG, "onDestroy");
        collectionBinding.disconnect();
        super.onDestroy(fragment);
    }

    private final class RefreshSubscriber extends Subscriber<Iterable<ItemT>> {

        @Override
        public void onNext(Iterable<ItemT> collection) {
            Log.d(TAG, "refresh complete");
            final ItemAdapter<ItemT> adapter = collectionBinding.adapter();
            adapter.clear();

            resetBindingTo(refreshBinding);
            refreshBinding = null;
            subscribeBinding();

            swipeRefreshAttacher.setRefreshing(false);
            unsubscribe();
        }

        @Override
        public void onCompleted() {
            // no op.
        }

        @Override
        public void onError(Throwable error) {
            Log.d(TAG, "refresh failed");
            error.printStackTrace();
            swipeRefreshAttacher.setRefreshing(false);
        }
    }

    private final class RefreshListener implements SwipeRefreshLayout.OnRefreshListener {
        @Override
        public void onRefresh() {
            Log.d(TAG, "refreshing collection");
            refreshBinding = onRefreshBinding();
            refreshBinding.items().subscribe(new RefreshSubscriber());
            refreshBinding.connect();
        }
    }

    private final class EmptyViewSubscriber extends Subscriber<Object> {

        @Override
        public void onCompleted() {
            updateEmptyViewStatus(EmptyView.Status.OK);
        }

        @Override
        public void onNext(Object unused) {
            updateEmptyViewStatus(EmptyView.Status.OK);
        }

        @Override
        public void onError(Throwable error) {
            error.printStackTrace();
            updateEmptyViewStatus(handleError(error));
        }
    }
}