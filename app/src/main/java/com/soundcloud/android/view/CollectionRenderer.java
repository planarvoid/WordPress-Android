package com.soundcloud.android.view;

import static com.soundcloud.android.view.ViewError.CONNECTION_ERROR;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;
import com.soundcloud.android.R;
import com.soundcloud.android.presentation.DividerItemDecoration;
import com.soundcloud.android.presentation.RecyclerItemAdapter;
import com.soundcloud.android.view.adapters.CollectionViewState;
import com.soundcloud.java.checks.Preconditions;
import com.soundcloud.java.optional.Optional;
import rx.functions.Action1;
import rx.functions.Func2;
import rx.subjects.PublishSubject;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.support.annotation.NonNull;
import android.support.v7.util.DiffUtil;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;

import java.util.List;

public class CollectionRenderer<ItemT, VH extends RecyclerView.ViewHolder> {

    @BindView(R.id.ak_recycler_view) protected RecyclerView recyclerView;
    @BindView(android.R.id.empty) protected EmptyView emptyView;
    @BindView(R.id.str_layout) protected MultiSwipeRefreshLayout swipeRefreshLayout;

    private Unbinder unbinder;

    private EmptyView.Status emptyViewStatus = EmptyView.Status.WAITING;
    private RecyclerView.AdapterDataObserver emptyViewObserver;

    private final PublishSubject<Void> onRefresh = PublishSubject.create();

    private final RecyclerItemAdapter<ItemT, VH> adapter;
    private final Func2<ItemT, ItemT, Boolean> areItemsTheSame;
    private final Func2<ItemT, ItemT, Boolean> areContentsTheSame;

    public CollectionRenderer(RecyclerItemAdapter<ItemT, VH> adapter, Func2<ItemT, ItemT, Boolean> areItemsTheSame, Func2<ItemT, ItemT, Boolean> areContentsTheSame) {
        this.adapter = adapter;
        this.areItemsTheSame = areItemsTheSame;
        this.areContentsTheSame = areContentsTheSame;
    }

    public void attach(View view) {
        Preconditions.checkArgument(recyclerView == null, "Recycler View already atteched. Did you forget to detach?");

        unbinder = ButterKnife.bind(this, view);

        configureRecyclerView(view.getContext());
        recyclerView.setAdapter(adapter);

        emptyViewObserver = createEmptyViewObserver();
        adapter.registerAdapterDataObserver(emptyViewObserver);
        emptyView.setStatus(emptyViewStatus);

        // handle swipe to refresh
        swipeRefreshLayout.setSwipeableChildren(recyclerView, emptyView);
        swipeRefreshLayout.setOnRefreshListener(() -> onRefresh.onNext(null));
    }

    public PublishSubject<Void> onRefresh(){
        return onRefresh;
    }

    public void detach() {
        adapter.unregisterAdapterDataObserver(emptyViewObserver);
        emptyView = null;
        recyclerView.setAdapter(null);
        recyclerView = null;
        unbinder.unbind();
    }

    public void render(CollectionViewState<ItemT> state) {
        swipeRefreshLayout.setRefreshing(state.isRefreshing());
        onNewItems(state.items());
        updateEmptyView();
    }

    private void configureRecyclerView(Context context) {
        // TODO : Grid Support
        recyclerView.setLayoutManager(new LinearLayoutManager(context));
        addListDividers(recyclerView);
    }

    private void addListDividers(RecyclerView recyclerView) {
        Drawable divider = recyclerView.getResources().getDrawable(com.soundcloud.androidkit.R.drawable.ak_list_divider_item);
        int dividerHeight = recyclerView.getResources().getDimensionPixelSize(com.soundcloud.androidkit.R.dimen.ak_list_divider_horizontal_height);
        recyclerView.addItemDecoration(new DividerItemDecoration(divider, dividerHeight));
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

    private void onNewItems(List<ItemT> newItems){
        final List<ItemT> oldItems = adapter().getItems();
        final DiffUtil.DiffResult diffResult = DiffUtil.calculateDiff(new AdapterDiffCallback(oldItems, newItems), true);
        populateAdapter(newItems);
        diffResult.dispatchUpdatesTo(adapter());
    }

    protected void populateAdapter(List<ItemT> newItems) {
        adapter.clear();
        for (ItemT item : newItems) {
            adapter.addItem(item);
        }
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
