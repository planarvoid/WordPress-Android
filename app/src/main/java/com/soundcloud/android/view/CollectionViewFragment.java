package com.soundcloud.android.view;

import static com.soundcloud.android.view.ViewError.CONNECTION_ERROR;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;
import com.soundcloud.android.R;
import com.soundcloud.android.presentation.RecyclerItemAdapter;
import com.soundcloud.android.rx.CrashOnTerminateSubscriber;
import com.soundcloud.android.view.adapters.CollectionViewState;
import com.soundcloud.java.optional.Optional;
import com.soundcloud.lightcycle.LightCycleSupportFragment;
import rx.Observable;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;
import rx.subjects.PublishSubject;
import rx.subscriptions.CompositeSubscription;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;

import java.util.List;

public abstract class CollectionViewFragment<ItemT, VH extends RecyclerView.ViewHolder>
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

                collectionView().map(CollectionViewState::isRefreshing)
                                .observeOn(AndroidSchedulers.mainThread())
                                .doOnNext(aBoolean -> swipeRefreshLayout.setRefreshing(aBoolean))
                                .subscribe(new CrashOnTerminateSubscriber<>()),

                collectionView().map(CollectionViewState::items)
                                .observeOn(AndroidSchedulers.mainThread())
                                .doOnNext(this::onNewItems)
                                .subscribe(new CrashOnTerminateSubscriber<>()),

                collectionView().doOnNext(updateEmptyView())
                                .subscribe(new CrashOnTerminateSubscriber<>())
        );
    }



    @NonNull
    private Action1<CollectionViewState<ItemT>> updateEmptyView() {
        return viewModel -> {
            Optional<ViewError> viewErrorOptional = viewModel.nextPageError();
            if (viewErrorOptional.isPresent()) {
                if (viewErrorOptional.get() == CONNECTION_ERROR) {
                    emptyView.setStatus(EmptyView.Status.CONNECTION_ERROR);
                } else {
                    emptyView.setStatus(EmptyView.Status.SERVER_ERROR);
                }
            } else if (viewModel.isLoadingNextPage()){
                emptyView.setStatus(EmptyView.Status.WAITING);
            } else {
                emptyView.setStatus(EmptyView.Status.OK);
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

    protected abstract Observable<CollectionViewState<ItemT>> collectionView();

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

}
