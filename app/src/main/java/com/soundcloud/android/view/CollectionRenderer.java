package com.soundcloud.android.view;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;
import com.soundcloud.android.R;
import com.soundcloud.android.presentation.DividerItemDecoration;
import com.soundcloud.android.presentation.RecyclerItemAdapter;
import com.soundcloud.android.view.adapters.CollectionViewState;
import com.soundcloud.java.checks.Preconditions;
import com.soundcloud.java.optional.Optional;
import rx.functions.Func2;
import rx.subjects.PublishSubject;

import android.graphics.drawable.Drawable;
import android.support.annotation.NonNull;
import android.support.v7.util.DiffUtil;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SimpleItemAnimator;
import android.view.View;

import java.util.List;

public class CollectionRenderer<ItemT, VH extends RecyclerView.ViewHolder> {

    private final EmptyStateProvider emptyStateProvider;
    @BindView(R.id.ak_recycler_view) protected RecyclerView recyclerView;
    @BindView(R.id.str_layout) protected MultiSwipeRefreshLayout swipeRefreshLayout;

    private Unbinder unbinder;
    private RecyclerView.AdapterDataObserver emptyViewObserver;
    private final PublishSubject<Void> onRefresh = PublishSubject.create();

    private final RecyclerItemAdapter<ItemT, VH> adapter;
    private final Func2<ItemT, ItemT, Boolean> areItemsTheSame;
    private final Func2<ItemT, ItemT, Boolean> areContentsTheSame;
    private EmptyAdapter emptyAdapter;
    private boolean animateLayoutChangesInItems;

    public CollectionRenderer(RecyclerItemAdapter<ItemT, VH> adapter,
                              Func2<ItemT, ItemT, Boolean> areItemsTheSame,
                              Func2<ItemT, ItemT, Boolean> areContentsTheSame,
                              EmptyStateProvider emptyStateProvider,
                              boolean animateLayoutChangesInItems) {
        this.adapter = adapter;
        this.areItemsTheSame = areItemsTheSame;
        this.areContentsTheSame = areContentsTheSame;
        this.emptyStateProvider = emptyStateProvider;
        this.animateLayoutChangesInItems = animateLayoutChangesInItems;

    }

    public void attach(View view, boolean renderEmptyAtTop, RecyclerView.LayoutManager layoutmanager) {
        Preconditions.checkArgument(recyclerView == null, "Recycler View already atteched. Did you forget to detach?");

        unbinder = ButterKnife.bind(this, view);

        configureRecyclerView(layoutmanager);
        this.emptyAdapter = new EmptyAdapter(emptyStateProvider, renderEmptyAtTop);
        recyclerView.setAdapter(emptyAdapter);

        // handle swipe to refresh
        swipeRefreshLayout.setSwipeableChildren(recyclerView);
        swipeRefreshLayout.setOnRefreshListener(() -> onRefresh.onNext(null));
    }

    public PublishSubject<Void> onRefresh() {
        return onRefresh;
    }

    public void detach() {
        if (emptyViewObserver != null) {
            adapter.unregisterAdapterDataObserver(emptyViewObserver);
        }

        emptyViewObserver = null;
        recyclerView.setAdapter(null);
        recyclerView = null;
        unbinder.unbind();
    }

    public void render(CollectionViewState<ItemT> state) {
        swipeRefreshLayout.setRefreshing(state.isRefreshing());
        if (state.items().isEmpty()) {
            if (recyclerView.getAdapter() != emptyAdapter) {
                recyclerView.setAdapter(emptyAdapter);
                setAnimateItemChanges(true);
            }
            updateEmptyView(state);

        } else {
            if (recyclerView.getAdapter() != adapter) {
                recyclerView.setAdapter(adapter);
                setAnimateItemChanges(animateLayoutChangesInItems);
                populateAdapter(state.items());
                adapter.notifyDataSetChanged();
            } else {
                onNewItems(state.items());
            }

        }
    }

    private void setAnimateItemChanges(boolean supportsChangeAnimations) {
        ((SimpleItemAnimator) recyclerView.getItemAnimator()).setSupportsChangeAnimations(supportsChangeAnimations);
    }

    private void configureRecyclerView(RecyclerView.LayoutManager layoutManager) {
        recyclerView.setLayoutManager(layoutManager);
        addListDividers(recyclerView);
    }

    private void addListDividers(RecyclerView recyclerView) {
        Drawable divider = recyclerView.getResources().getDrawable(com.soundcloud.androidkit.R.drawable.ak_list_divider_item);
        int dividerHeight = recyclerView.getResources().getDimensionPixelSize(com.soundcloud.androidkit.R.dimen.ak_list_divider_horizontal_height);
        recyclerView.addItemDecoration(new DividerItemDecoration(divider, dividerHeight));
    }

    @NonNull
    private void updateEmptyView(CollectionViewState<ItemT> state) {
        Optional<ViewError> viewErrorOptional = state.nextPageError();
        emptyAdapter.setEmptyStatus(EmptyStatus.fromErrorAndLoading(viewErrorOptional, state.isLoadingNextPage()));
        emptyAdapter.notifyDataSetChanged();
    }

    public RecyclerItemAdapter<ItemT, VH> adapter() {
        return adapter;
    }

    private void onNewItems(List<ItemT> newItems) {
        final List<ItemT> oldItems = adapter().getItems();
        final DiffUtil.DiffResult diffResult = DiffUtil.calculateDiff(new AdapterDiffCallback(oldItems, newItems), true);
        populateAdapter(newItems);
        diffResult.dispatchUpdatesTo(adapter());
    }

    private void populateAdapter(List<ItemT> newItems) {
        adapter.clear();
        for (ItemT item : newItems) {
            adapter.addItem(item);
        }
    }

    public interface EmptyStateProvider {

        int waitingView();

        int connectionErrorView();

        int serverErrorView();

        int emptyView();
    }

    private class AdapterDiffCallback extends DiffUtil.Callback {

        private final List<ItemT> oldItems;
        private final List<ItemT> newItems;

        public AdapterDiffCallback(List<ItemT> oldItems, List<ItemT> newItems) {
            this.oldItems = oldItems;
            this.newItems = newItems;
        }

        @Override
        public int getOldListSize() {
            return oldItems.size();
        }

        @Override
        public int getNewListSize() {
            return newItems.size();
        }

        @Override
        public boolean areItemsTheSame(int oldItemPosition, int newItemPosition) {
            final ItemT oldItem = oldItems.get(oldItemPosition);
            final ItemT newItem = newItems.get(newItemPosition);
            return areItemsTheSame.call(oldItem, newItem);
        }

        @Override
        public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
            final ItemT oldItem = oldItems.get(oldItemPosition);
            final ItemT newItem = newItems.get(newItemPosition);
            return areContentsTheSame.call(oldItem, newItem);
        }
    }
}

