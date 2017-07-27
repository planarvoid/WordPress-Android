package com.soundcloud.android.presentation;

import com.soundcloud.android.view.EmptyView;
import com.soundcloud.android.view.MultiSwipeRefreshLayout;
import com.soundcloud.lightcycle.LightCycles;
import com.soundcloud.lightcycle.SupportFragmentLightCycleDispatcher;
import rx.Observer;
import rx.Subscriber;
import rx.Subscription;
import rx.subscriptions.CompositeSubscription;
import rx.subscriptions.Subscriptions;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.widget.SwipeRefreshLayout;
import android.util.Log;
import android.view.View;

abstract class CollectionViewPresenter<SourceT, ItemT>
        extends SupportFragmentLightCycleDispatcher<Fragment> {

    private static final String TAG = "CollectionViewPresenter";

    private final SwipeRefreshAttacher swipeRefreshAttacher;

    private Bundle fragmentArgs;
    private CollectionBinding<SourceT, ItemT> collectionBinding;
    private CollectionBinding<SourceT, ItemT> refreshBinding;
    private CompositeSubscription viewLifeCycle;
    private Subscription adapterSubscription = Subscriptions.unsubscribed();
    private Subscription refreshSubscription = Subscriptions.unsubscribed();

    private EmptyView emptyView;
    private EmptyView.Status emptyViewStatus = EmptyView.Status.WAITING;

    protected CollectionViewPresenter(SwipeRefreshAttacher swipeRefreshAttacher) {
        this.swipeRefreshAttacher = swipeRefreshAttacher;
    }

    protected abstract CollectionBinding<SourceT, ItemT> onBuildBinding(Bundle fragmentArgs);

    protected CollectionBinding<SourceT, ItemT> onRefreshBinding() {
        return onBuildBinding(fragmentArgs);
    }

    protected void onSubscribeBinding(CollectionBinding<SourceT, ItemT> collectionBinding,
                                      CompositeSubscription viewLifeCycle) {
        // NOP by default
    }

    public CollectionBinding<SourceT, ItemT> getBinding() {
        return collectionBinding;
    }

    public EmptyView getEmptyView() {
        return emptyView;
    }

    @Override
    public void onCreate(Fragment fragment, @Nullable Bundle bundle) {
        Log.d(TAG, "onCreate");
        LightCycles.bind(this);
        super.onCreate(fragment, bundle);
        this.fragmentArgs = fragment.getArguments();
        rebuildBinding(fragmentArgs);
    }

    protected CollectionBinding<SourceT, ItemT> rebuildBinding(@Nullable Bundle fragmentArgs) {
        Log.d(TAG, "rebinding collection");
        resetBindingTo(onBuildBinding(fragmentArgs));
        return collectionBinding;
    }

    private void resetBindingTo(CollectionBinding<SourceT, ItemT> collectionBinding) {
        this.collectionBinding = collectionBinding;
        adapterSubscription.unsubscribe();
        adapterSubscription = this.collectionBinding.items()
                                                    .subscribe(new CollectionViewSubscriber<>(collectionBinding));
    }

    protected void retryWith(CollectionBinding<SourceT, ItemT> collectionBinding) {
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
        if (emptyView == null) {
            throw new NullPointerException("android.id.empty not found in layout " + view.getClass().getCanonicalName());
        }
        emptyView.setStatus(emptyViewStatus);
    }

    private void updateEmptyViewStatus(EmptyView.Status status) {
        if (emptyView != null) {
            this.emptyViewStatus = status;
            emptyView.setStatus(status);
        }
    }

    protected abstract void onCreateCollectionView(Fragment fragment, View view, Bundle savedInstanceState);

    protected abstract EmptyView.Status handleError(Throwable error);

    protected void onItemClicked(View view, int position) {
        // no-op by default
    }

    @Override
    public void onDestroyView(Fragment fragment) {
        Log.d(TAG, "onDestroyView");
        viewLifeCycle.unsubscribe();
        detachSwipeToRefresh();
        emptyView = null;
        super.onDestroyView(fragment);
    }

    protected void attachSwipeToRefresh(MultiSwipeRefreshLayout refreshLayout, View... refreshableViews) {
        swipeRefreshAttacher.attach(new RefreshListener(), refreshLayout, refreshableViews);
        if (refreshBinding != null) {
            swipeRefreshAttacher.setRefreshing(true);
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

    private static class CollectionViewSubscriber<SourceT, ItemT> extends Subscriber<Iterable<ItemT>> {
        private final CollectionBinding<SourceT, ItemT> collectionBinding;

        public CollectionViewSubscriber(CollectionBinding<SourceT, ItemT> collectionBinding) {
            this.collectionBinding = collectionBinding;
        }

        @Override
        public void onCompleted() {
            for (Observer<Iterable<ItemT>> observer : collectionBinding.observers()) {
                observer.onCompleted();
            }
        }

        @Override
        public void onError(Throwable e) {
            for (Observer<Iterable<ItemT>> observer : collectionBinding.observers()) {
                observer.onError(e);
            }
        }

        @Override
        public void onNext(Iterable<ItemT> items) {
            for (Observer<Iterable<ItemT>> observer : collectionBinding.observers()) {
                observer.onNext(items);
            }
        }
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
            refreshBinding = null;
        }
    }

    private class RefreshListener implements SwipeRefreshLayout.OnRefreshListener {
        @Override
        public void onRefresh() {
            Log.d(TAG, "refreshing collection");
            refreshBinding = onRefreshBinding();
            refreshSubscription.unsubscribe();
            refreshSubscription = refreshBinding.items().subscribe(new RefreshSubscriber());
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
