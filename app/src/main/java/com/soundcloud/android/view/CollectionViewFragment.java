package com.soundcloud.android.view;

import static com.soundcloud.android.view.ViewError.CONNECTION_ERROR;
import static java.util.Collections.emptyList;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;
import com.soundcloud.android.R;
import com.soundcloud.android.presentation.RecyclerItemAdapter;
import com.soundcloud.android.rx.observers.DefaultSubscriber;
import com.soundcloud.java.optional.Optional;
import com.soundcloud.lightcycle.LightCycleSupportFragment;
import rx.Observable;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;
import rx.functions.Func1;
import rx.subjects.PublishSubject;
import rx.subscriptions.CompositeSubscription;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;

import java.util.List;

public abstract class CollectionViewFragment<ViewModelT, ItemT, VH extends RecyclerView.ViewHolder>
        extends LightCycleSupportFragment<Fragment> {

    @BindView(R.id.ak_recycler_view) protected RecyclerView recyclerView;
    @BindView(android.R.id.empty) protected EmptyView emptyView;
    @BindView(R.id.str_layout) protected MultiSwipeRefreshLayout swipeRefreshLayout;

    private RecyclerItemAdapter<ItemT, VH> adapter;
    private Unbinder unbinder;

    private EmptyView.Status emptyViewStatus = EmptyView.Status.WAITING;
    private RecyclerView.AdapterDataObserver emptyViewObserver;
    private CompositeSubscription subscription;

    protected final PublishSubject<Void> onRefresh = PublishSubject.create();

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
        swipeRefreshLayout.setOnRefreshListener(() -> onRefresh.onNext(null));

        subscription = new CompositeSubscription(

                modelUpdates().map(AsyncViewModel::isRefreshing)
                              .observeOn(AndroidSchedulers.mainThread())
                              .doOnNext(aBoolean -> swipeRefreshLayout.setRefreshing(aBoolean))
                              .subscribe(new CrashOnTerminateSubscriber<>()),

                modelUpdates().map(extractItems())
                              .observeOn(AndroidSchedulers.mainThread())
                              .doOnNext(data -> onNewItems(data))
                              .subscribe(new CrashOnTerminateSubscriber<>()),

                modelUpdates().doOnNext(updateEmptyView())
                              .subscribe(new CrashOnTerminateSubscriber<>())
        );
    }



    @NonNull
    private Func1<AsyncViewModel<ViewModelT>, List<ItemT>> extractItems() {
        return viewModelTAsyncViewModel -> viewModelTAsyncViewModel.data().isPresent()
                                           ? viewModelToItems().call(viewModelTAsyncViewModel.data().get())
                                           : emptyList();
    }

    @NonNull
    private Action1<AsyncViewModel<ViewModelT>> updateEmptyView() {
        return asyncViewModel -> {
            Optional<ViewError> viewErrorOptional = asyncViewModel.error();
            if (viewErrorOptional.isPresent()) {
                if (viewErrorOptional.get() == CONNECTION_ERROR) {
                    emptyView.setStatus(EmptyView.Status.CONNECTION_ERROR);
                } else {
                    emptyView.setStatus(EmptyView.Status.SERVER_ERROR);
                }
            } else if (asyncViewModel.data().isPresent()) {
                emptyView.setStatus(EmptyView.Status.OK);
            } else {
                emptyView.setStatus(EmptyView.Status.WAITING);
            }
        };
    }

    public RecyclerItemAdapter<ItemT, VH> adapter() {
        return adapter;
    }

    protected abstract void onNewItems(List<ItemT> newItems);

    protected void populateAdapter(List<ItemT> newItems) {
        adapter.clear();
        for (ItemT item : newItems) {
            adapter.addItem(item);
        }
    }

    protected abstract RecyclerItemAdapter<ItemT, VH> createAdapter();

    protected abstract Observable<AsyncViewModel<ViewModelT>> modelUpdates();

    protected abstract Func1<ViewModelT, List<ItemT>> viewModelToItems();

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
