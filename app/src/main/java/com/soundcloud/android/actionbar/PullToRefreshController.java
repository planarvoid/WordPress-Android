package com.soundcloud.android.actionbar;

import com.google.common.base.Preconditions;
import com.soundcloud.android.R;
import com.soundcloud.android.presentation.SwipeRefreshAttacher;
import com.soundcloud.android.rx.RxUtils;
import com.soundcloud.android.rx.observers.DefaultSubscriber;
import com.soundcloud.android.view.MultiSwipeRefreshLayout;
import com.soundcloud.android.view.RefreshableListComponent;
import com.soundcloud.android.view.adapters.ReactiveAdapter;
import com.soundcloud.lightcycle.DefaultSupportFragmentLightCycle;
import org.jetbrains.annotations.Nullable;
import rx.Observable;
import rx.Subscription;
import rx.subscriptions.Subscriptions;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v4.widget.SwipeRefreshLayout.OnRefreshListener;
import android.view.View;

import javax.inject.Inject;

@Deprecated // use ListPresenter now, or use PullToRefreshWrapper directly
public class PullToRefreshController extends DefaultSupportFragmentLightCycle {

    private final SwipeRefreshAttacher wrapper;

    private Subscription refreshSubscription = RxUtils.invalidSubscription();
    private boolean wasRefreshing;

    private OnRefreshListener refreshListener;

    @Inject
    public PullToRefreshController(SwipeRefreshAttacher wrapper) {
        this.wrapper = wrapper;
    }

    public <T extends Iterable<?>, OT extends Observable<? extends T>>
    void setRefreshListener(final RefreshableListComponent<OT> component, final ReactiveAdapter<T> adapter) {
        refreshListener = new OnRefreshListener() {
            @Override
            public void onRefresh() {
                OT refreshObservable = component.refreshObservable();
                refreshSubscription = refreshObservable.subscribe(new RefreshSubscriber<>(adapter));
                component.connectObservable(refreshObservable);
            }
        };
    }

    public void setRefreshListener(OnRefreshListener refreshListener) {
        this.refreshListener = refreshListener;
    }

    @Override
    public void onViewCreated(Fragment fragment, View view, @Nullable Bundle savedInstanceState) {
        Preconditions.checkNotNull(refreshListener, "You must set a refresh listener before calling onViewCreated");
        MultiSwipeRefreshLayout swipeRefreshLayout;
        if (view instanceof SwipeRefreshLayout) {
            // this is only used for ScListFragment right now
            swipeRefreshLayout = (MultiSwipeRefreshLayout) view;
        } else {
            swipeRefreshLayout = (MultiSwipeRefreshLayout) view.findViewById(R.id.str_layout);
        }

        wrapper.attach(refreshListener, swipeRefreshLayout,
                view.findViewById(android.R.id.list),
                view.findViewById(android.R.id.empty));
        wrapper.setRefreshing(wasRefreshing);
    }

    /**
     * Use this overload for paged list fragments, as it will take care of managing all PTR state.
     */
    public <T extends Iterable<?>> void connect(Observable<? extends T> activeObservable, ReactiveAdapter<T> adapter) {
        if (wasRefreshing) {
            refreshSubscription = activeObservable.subscribe(new RefreshSubscriber<>(adapter));
        }
    }

    @Override
    public void onDestroyView(Fragment fragment) {
        this.wasRefreshing = isRefreshing();
        refreshSubscription.unsubscribe();
        wrapper.detach();
    }

    public boolean isAttached() {
        return wrapper.isAttached();
    }

    public boolean isRefreshing() {
        return wrapper.isRefreshing();
    }

    public void startRefreshing() {
        if (isAttached()) {
            wrapper.setRefreshing(true);
        }
    }

    public void stopRefreshing() {
        if (isAttached()) {
            wrapper.setRefreshing(false);
        }
    }

    private final class RefreshSubscriber<CollT extends Iterable<?>> extends DefaultSubscriber<CollT> {

        private final ReactiveAdapter<CollT> adapter;

        public RefreshSubscriber(ReactiveAdapter<CollT> adapter) {
            this.adapter = adapter;
        }

        @Override
        public void onNext(CollT collection) {
            adapter.clear();
            adapter.onNext(collection);
            stopRefreshing();
            unsubscribe();
        }

        @Override
        public void onError(Throwable error) {
            stopRefreshing();
            super.onError(error);
        }
    }
}
