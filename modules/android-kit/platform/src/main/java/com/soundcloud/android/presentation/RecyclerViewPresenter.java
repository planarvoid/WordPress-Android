package com.soundcloud.android.presentation;

import com.soundcloud.androidkit.R;

import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.annotation.IntegerRes;
import android.support.v4.app.Fragment;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.RecyclerView.AdapterDataObserver;
import android.support.v7.widget.RecyclerView.LayoutManager;
import android.support.v7.widget.SimpleItemAnimator;
import android.support.v7.widget.StaggeredGridLayoutManager;
import android.view.View;

public abstract class RecyclerViewPresenter<SourceT, ItemT> extends CollectionViewPresenter<SourceT, ItemT> {

    private final Options options;
    private RecyclerView recyclerView;
    private AdapterDataObserver emptyViewObserver;

    protected RecyclerViewPresenter(SwipeRefreshAttacher swipeRefreshAttacher) {
        this(swipeRefreshAttacher, Options.list().build());
    }

    protected RecyclerViewPresenter(SwipeRefreshAttacher swipeRefreshAttacher, Options options) {
        super(swipeRefreshAttacher);
        this.options = options;
    }

    public RecyclerView getRecyclerView() {
        return recyclerView;
    }

    public void scrollToTop() {
        recyclerView.smoothScrollToPosition(0);
    }

    @Override
    protected void onCreateCollectionView(Fragment fragment, View view, Bundle savedInstanceState) {
        final CollectionBinding<SourceT, ItemT> collectionBinding = getBinding();

        setupRecyclerView(fragment, view);

        setupDividers(view);

        final RecyclerItemAdapter adapter = setupAdapter(collectionBinding);

        setupEmptyView(adapter);
    }

    private void setupEmptyView(RecyclerItemAdapter adapter) {
        emptyViewObserver = createEmptyViewObserver();
        adapter.registerAdapterDataObserver(emptyViewObserver);
        updateEmptyViewVisibility();
    }

    private void updateEmptyViewVisibility() {
        final boolean empty = getBinding().adapter().isEmpty();
        getRecyclerView().setVisibility(empty ? View.GONE : View.VISIBLE);
        getEmptyView().setVisibility(empty ? View.VISIBLE : View.GONE);
    }

    private void setupRecyclerView(Fragment fragment, View view) {
        this.recyclerView = view.findViewById(R.id.ak_recycler_view);
        if (this.recyclerView == null) {
            throw new IllegalStateException("Expected to find RecyclerView with ID R.id.recycler_view");
        }

        ((SimpleItemAnimator) recyclerView.getItemAnimator()).setSupportsChangeAnimations(options.useChangeAnimations);

        final Resources resources = view.getResources();
        if (options.layoutManagerClass == GridLayoutManager.class) {
            recyclerView.setLayoutManager(new GridLayoutManager(fragment.getActivity(),
                    resources.getInteger(options.numColumns)));
        } else if (options.layoutManagerClass == StaggeredGridLayoutManager.class) {
            recyclerView.setLayoutManager(new StaggeredGridLayoutManager(
                    resources.getInteger(options.numColumns), StaggeredGridLayoutManager.VERTICAL));
        } else {
            recyclerView.setLayoutManager(new LinearLayoutManager(fragment.getActivity()));
        }
    }

    private RecyclerItemAdapter setupAdapter(CollectionBinding<SourceT, ItemT> collectionBinding) {
        if (!(collectionBinding.adapter() instanceof RecyclerItemAdapter)) {
            throw new IllegalArgumentException("Adapter must be an " + RecyclerItemAdapter.class);
        }
        final RecyclerItemAdapter adapter = (RecyclerItemAdapter) collectionBinding.adapter();
        recyclerView.setAdapter(adapter);

        if (options.useItemClickListener) {
            setupDefaultClickListener(adapter);
        }

        if (collectionBinding instanceof PagedCollectionBinding) {
            configurePagedListAdapter((PagedCollectionBinding<SourceT, ItemT, ?>) collectionBinding);
        }
        return adapter;
    }

    private void setupDefaultClickListener(final RecyclerItemAdapter adapter) {
        adapter.setOnItemClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                final int adapterPosition = recyclerView.getChildAdapterPosition(view);
                if (adapterPosition >= 0 && adapterPosition < adapter.getItemCount()) {
                    onItemClicked(view, adapterPosition);
                } else {
                    new IllegalArgumentException("Invalid recycler position in click handler " + adapterPosition);
                }
            }
        });
    }

    private void setupDividers(View view) {
        switch (options.dividerMode) {
            case LIST:
                addListDividers(view);
                break;
            case GRID:
                addGridDividers(view);
                break;
            default:
                // No dividers
        }

    }

    private void addListDividers(View view) {
        Drawable divider = view.getResources().getDrawable(R.drawable.ak_list_divider_item);
        int dividerHeight = view.getResources().getDimensionPixelSize(R.dimen.ak_list_divider_horizontal_height);
        recyclerView.addItemDecoration(new DividerItemDecoration(divider, dividerHeight));
    }

    private void addGridDividers(View view) {
        int leftRightInset = view.getResources().getDimensionPixelSize(R.dimen.ak_grid_divider_left_right_inset);
        int topBottomInset = view.getResources().getDimensionPixelSize(R.dimen.ak_grid_divider_top_bottom_inset);
        recyclerView.addItemDecoration(new InsetDividerDecoration(leftRightInset, topBottomInset));
    }

    @Override
    public void onDestroyView(Fragment fragment) {
        recyclerView.clearOnScrollListeners();
        recyclerView.getAdapter().unregisterAdapterDataObserver(emptyViewObserver);
        recyclerView.setAdapter(null);
        recyclerView = null;
        super.onDestroyView(fragment);
    }

    private void configurePagedListAdapter(final PagedCollectionBinding<SourceT, ItemT, ?> binding) {
        final PagingAwareAdapter<ItemT> adapter = binding.adapter();
        final int numberOfColumns = recyclerView.getResources().getInteger(options.numColumns);

        recyclerView.addOnScrollListener(new PagingRecyclerScrollListener(
                this, adapter, recyclerView.getLayoutManager(), numberOfColumns));

        adapter.setOnErrorRetryListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                adapter.setLoading();
                retryWith(binding.fromCurrentPage());
            }
        });
    }

    private AdapterDataObserver createEmptyViewObserver() {
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

    public static class Options {

        public enum DividerMode {
            NONE, LIST, GRID
        }

        public static Options defaults() {
            return new Builder().build();
        }

        public static Builder custom() {
            return new Builder();
        }

        public static Options.Builder list() {
            return new Builder()
                    .withLayoutManager(LinearLayoutManager.class)
                    .useDividers(DividerMode.LIST)
                    .useItemClickListener();
        }

        public static Options.Builder grid(@IntegerRes int numColumns) {
            return new Builder()
                    .withLayoutManager(GridLayoutManager.class)
                    .columns(numColumns)
                    .useDividers(DividerMode.GRID)
                    .useItemClickListener();
        }

        public static Options.Builder staggeredGrid(@IntegerRes int numColumns) {
            return new Builder()
                    .withLayoutManager(StaggeredGridLayoutManager.class)
                    .columns(numColumns)
                    .useDividers(DividerMode.GRID)
                    .useItemClickListener();
        }

        private final DividerMode dividerMode;
        private final boolean useItemClickListener;
        private final boolean useChangeAnimations;
        private final Class<? extends LayoutManager> layoutManagerClass;
        private final int numColumns;

        private Options(DividerMode dividerMode,
                        boolean useItemClickListener,
                        boolean useChangeAnimations,
                        Class<? extends LayoutManager> layoutManagerClass,
                        @IntegerRes int numColumns) {
            this.dividerMode = dividerMode;
            this.useItemClickListener = useItemClickListener;
            this.useChangeAnimations = useChangeAnimations;
            this.layoutManagerClass = layoutManagerClass;
            this.numColumns = numColumns;
        }

        public static class Builder {

            private int numColumns = R.integer.ak_default_grid_columns;
            private DividerMode dividerMode = DividerMode.NONE;
            private boolean useItemClickListener;
            private boolean useChangeAnimations;
            private Class<? extends LayoutManager> layoutManagerClass;

            public Builder useDividers(DividerMode dividers) {
                dividerMode = dividers;
                return this;
            }

            public Builder useItemClickListener() {
                useItemClickListener = true;
                return this;
            }

            public Builder useChangeAnimations() {
                useChangeAnimations = true;
                return this;
            }

            // for internal use only
            private Builder withLayoutManager(Class<? extends LayoutManager> layoutManagerClass) {
                this.layoutManagerClass = layoutManagerClass;
                return this;
            }

            /**
             * An integer resource specifying how many columns of items should be rendered.
             * Currently this has only an effect when used in conjunction with {@link GridLayoutManager}
             *
             * @param numColumns the number of columns, specified as an integer resource
             */
            public Builder columns(@IntegerRes int numColumns) {
                this.numColumns = numColumns;
                return this;
            }

            public Options build() {
                return new Options(dividerMode, useItemClickListener, useChangeAnimations, layoutManagerClass, numColumns);
            }
        }
    }
}
