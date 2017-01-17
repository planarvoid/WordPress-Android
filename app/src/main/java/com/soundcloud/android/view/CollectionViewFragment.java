package com.soundcloud.android.view;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;
import com.soundcloud.android.R;
import com.soundcloud.android.presentation.RecyclerItemAdapter;
import com.soundcloud.android.rx.observers.DefaultSubscriber;
import com.soundcloud.lightcycle.LightCycleSupportFragment;
import rx.Observable;
import rx.android.schedulers.AndroidSchedulers;
import rx.subscriptions.CompositeSubscription;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;

// TODO: EmptyView logic (?)
public abstract class CollectionViewFragment<ItemT>
        extends LightCycleSupportFragment<Fragment> {

    @BindView(R.id.ak_recycler_view) protected RecyclerView recyclerView;
    @BindView(android.R.id.empty) protected EmptyView emptyView;
    @BindView(R.id.str_layout) protected MultiSwipeRefreshLayout swipeRefreshLayout;

    private RecyclerItemAdapter<ItemT, RecyclerView.ViewHolder> adapter;
    private Unbinder unbinder;

    private EmptyView.Status emptyViewStatus = EmptyView.Status.WAITING;
    private RecyclerView.AdapterDataObserver emptyViewObserver;
    private CompositeSubscription subscription;

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        unbinder = ButterKnife.bind(this, view);

        adapter = createAdapter();
        recyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
        recyclerView.setAdapter(adapter);

        emptyViewObserver = createEmptyViewObserver();
        adapter.registerAdapterDataObserver(emptyViewObserver);
        emptyView.setStatus(emptyViewStatus);

        // handle swipe to refresh
        swipeRefreshLayout.setSwipeableChildren(recyclerView, emptyView);
        swipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                CollectionViewFragment.this.onRefresh();
            }
        });
        // TODO : Logic to trigger STR

        subscription = new CompositeSubscription(
                isRefreshing()
                        .observeOn(AndroidSchedulers.mainThread())
                        .doOnNext(aBoolean -> swipeRefreshLayout.setRefreshing(aBoolean))
                        .subscribe(new CrashOnTerminateSubscriber<>()),

                items().observeOn(AndroidSchedulers.mainThread())
                       .doOnNext(this::replaceItemsInAdapter)
                       .subscribe(new CrashOnTerminateSubscriber<>())
        );
    }

    protected abstract void onRefresh();

    private void replaceItemsInAdapter(Iterable<ItemT> itemTs) {
        adapter.clear();
        for (ItemT item : itemTs) {
            adapter.addItem(item);
        }
        adapter.notifyDataSetChanged();
    }

    protected abstract RecyclerItemAdapter<ItemT, RecyclerView.ViewHolder> createAdapter();

    protected abstract Observable<Iterable<ItemT>> items();

    protected abstract Observable<Boolean> isRefreshing();

    @Override
    public void onDestroyView() {
        subscription.unsubscribe();
        adapter.unregisterAdapterDataObserver(emptyViewObserver);

        emptyView = null;
        adapter = null;
        recyclerView.setAdapter(null);
        recyclerView = null;
        unbinder.unbind();

        super.onDestroyView();
    }

    private RecyclerView.AdapterDataObserver createEmptyViewObserver() {
        return new RecyclerView.AdapterDataObserver() {
            @Override
            public void onItemRangeInserted(int positionStart, int itemCount) {
                updateEmptyViewVisibility();
            }

            @Override
            public void onItemRangeRemoved(int positionStart, int itemCount) {
                updateEmptyViewVisibility();
            }

            @Override
            public void onChanged() {
                updateEmptyViewVisibility();
            }
        };
    }

    private void updateEmptyViewVisibility() {
        final boolean empty = adapter.isEmpty();
        recyclerView.setVisibility(empty ? View.GONE : View.VISIBLE);
        emptyView.setVisibility(empty ? View.VISIBLE : View.GONE);
    }

    private class CrashOnTerminateSubscriber<T> extends DefaultSubscriber<T> {

        @Override
        public void onError(Throwable e) {
            super.onError(new IllegalStateException(e));
        }

        @Override
        public void onCompleted() {
            throw new IllegalStateException("Subscription should not terminate");
        }
    }
}
