package com.soundcloud.android.view;

import static android.widget.AbsListView.OnScrollListener;
import static rx.android.schedulers.AndroidSchedulers.mainThread;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import com.soundcloud.android.image.ImageOperations;
import com.soundcloud.android.presentation.PagingListItemAdapter;
import com.soundcloud.lightcycle.DefaultSupportFragmentLightCycle;
import com.soundcloud.android.presentation.ListItem;
import org.jetbrains.annotations.Nullable;
import rx.Observable;
import rx.android.LegacyPager;
import rx.functions.Func1;
import rx.internal.util.UtilityFunctions;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.View;
import android.widget.AbsListView;
import android.widget.ListAdapter;

import javax.inject.Inject;

@Deprecated // use ListPresenter
public class ListViewController extends DefaultSupportFragmentLightCycle {

    private final EmptyViewController emptyViewController;
    private final ImageOperations imageOperations;

    private AbsListView absListView;
    private ListAdapter adapter;

    @Nullable private OnScrollListener scrollListener;
    @Nullable private LegacyPager<?> pager;

    @Inject
    public ListViewController(EmptyViewController emptyViewController, ImageOperations imageOperations) {
        this.emptyViewController = emptyViewController;
        this.imageOperations = imageOperations;
    }

    /**
     * Use this method to connect an adapter on the managed ListView if the content is not paged but a static list.
     */
    public void setAdapter(ListAdapter adapter) {
        this.adapter = adapter;
    }

    /**
     * Use this method to connect an adapter and pager object to get paged list views. The given item mapper can
     * apply an optional transformation of items before adding them to the adapter, e.g. when mapping to a view model.
     */
    public <T, R extends ListItem, CollT extends Iterable<T>>
    void setAdapter(final PagingListItemAdapter<R> adapter, final LegacyPager<CollT> pager, final Func1<CollT, ? extends Iterable<R>> itemMapper) {
        this.adapter = adapter;
        this.pager = pager;
        adapter.setOnErrorRetryListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                adapter.setLoading();
                pager.currentPage().map(itemMapper).observeOn(mainThread()).subscribe(adapter);
            }
        });
    }

    /**
     * Like {@link #setAdapter(PagingListItemAdapter, LegacyPager)}, but does
     * not perform any item mapping.
     */
    public <T extends ListItem, CollT extends Iterable<T>> void setAdapter(final PagingListItemAdapter<T> adapter, final LegacyPager<CollT> pager) {
        setAdapter(adapter, pager, UtilityFunctions.<CollT>identity());
    }

    public void setScrollListener(@Nullable OnScrollListener scrollListener) {
        this.scrollListener = scrollListener;
    }

    public <O extends Observable<?>> void connect(final ReactiveListComponent<O> listComponent, O observable) {
        emptyViewController.connect(listComponent, observable);
        absListView.setOnItemClickListener(listComponent);
    }

    @Override
    public void onViewCreated(Fragment fragment, View view, @Nullable Bundle savedInstanceState) {
        Preconditions.checkNotNull(adapter, "You must set an adapter before calling onViewCreated");
        emptyViewController.onViewCreated(fragment, view, savedInstanceState);

        absListView = (AbsListView) view.findViewById(android.R.id.list);
        absListView.setEmptyView(emptyViewController.getEmptyView());

        if (scrollListener == null) {
            scrollListener = imageOperations.createScrollPauseListener(false, true);
        } else {
            scrollListener = imageOperations.createScrollPauseListener(false, true, scrollListener);
        }
        if (pager != null) {
            scrollListener = new PagingScrollListener(pager, (PagingListItemAdapter) adapter, scrollListener);
        }

        absListView.setOnScrollListener(scrollListener);
        absListView.setAdapter(adapter);
    }

    @Override
    public void onDestroyView(Fragment fragment) {
        emptyViewController.onDestroyView(fragment);
        absListView.setAdapter(null);
        absListView = null;
    }

    public EmptyView getEmptyView() {
        return emptyViewController.getEmptyView();
    }

    @VisibleForTesting
    AbsListView getListView() {
        return absListView;
    }

    private static class PagingScrollListener implements AbsListView.OnScrollListener {

        private final LegacyPager<?> pager;
        private final PagingListItemAdapter<?> adapter;
        private final OnScrollListener listenerDelegate;

        PagingScrollListener(LegacyPager<?> pager, PagingListItemAdapter<?> adapter, OnScrollListener listenerDelegate) {
            this.pager = pager;
            this.adapter = adapter;
            this.listenerDelegate = listenerDelegate;
        }

        @Override
        public void onScrollStateChanged(AbsListView view, int scrollState) {
            listenerDelegate.onScrollStateChanged(view, scrollState);
        }

        @Override
        public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
            listenerDelegate.onScroll(view, firstVisibleItem, visibleItemCount, totalItemCount);

            int lookAheadSize = visibleItemCount * 2;
            boolean lastItemReached = totalItemCount > 0 && (totalItemCount - lookAheadSize <= firstVisibleItem);

            if (lastItemReached && adapter.isIdle() && pager.hasNext()) {
                adapter.setLoading();
                pager.next();
            }
        }
    }
}
