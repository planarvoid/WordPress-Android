package com.soundcloud.android.view;

import static android.widget.AbsListView.OnScrollListener;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import com.soundcloud.android.image.ImageOperations;
import com.soundcloud.android.main.DefaultFragmentLifeCycle;
import com.soundcloud.android.view.adapters.EndlessAdapter;
import org.jetbrains.annotations.Nullable;
import rx.Observable;
import rx.android.Pager;

import android.annotation.TargetApi;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.View;
import android.widget.AbsListView;
import android.widget.GridView;
import android.widget.ListAdapter;
import android.widget.ListView;

import javax.inject.Inject;

public class ListViewController extends DefaultFragmentLifeCycle<Fragment> {

    private final EmptyViewController emptyViewController;
    private final ImageOperations imageOperations;

    private AbsListView absListView;
    private ListAdapter adapter;
    private @Nullable OnScrollListener scrollListener;
    private @Nullable Pager<?> pager;

    @Inject
    public ListViewController(EmptyViewController emptyViewController, ImageOperations imageOperations) {
        this.emptyViewController = emptyViewController;
        this.imageOperations = imageOperations;
    }

    public void setAdapter(ListAdapter adapter) {
        this.adapter = adapter;
    }

    public <T> void setAdapter(EndlessAdapter<T> adapter, Pager<? extends Iterable<T>> pager) {
        this.adapter = adapter;
        this.pager = pager;
    }

    public void setScrollListener(@Nullable OnScrollListener scrollListener) {
        this.scrollListener = scrollListener;
    }

    public <O extends Observable<?>> void connect(final ReactiveListComponent<O> listComponent, O observable) {
        emptyViewController.connect(listComponent, observable);
        absListView.setOnItemClickListener(listComponent);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        Preconditions.checkNotNull(adapter, "You must set an adapter before calling onViewCreated");
        emptyViewController.onViewCreated(view, savedInstanceState);

        absListView = (AbsListView) view.findViewById(android.R.id.list);
        absListView.setEmptyView(emptyViewController.getEmptyView());

        if (scrollListener == null) {
            scrollListener = imageOperations.createScrollPauseListener(false, true);
        } else {
            scrollListener = imageOperations.createScrollPauseListener(false, true, scrollListener);
        }
        if (pager != null) {
            scrollListener = new PagingScrollListener(pager, (EndlessAdapter) adapter, scrollListener);
        }

        absListView.setOnScrollListener(scrollListener);

        compatSetAdapter(adapter);
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    private void compatSetAdapter(@Nullable ListAdapter adapter) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            absListView.setAdapter(adapter);
        } else if (absListView instanceof GridView) {
            final GridView gridView = (GridView) absListView;
            gridView.setAdapter(adapter);
        } else if (absListView instanceof ListView) {
            final ListView listView = (ListView) absListView;
            listView.setAdapter(adapter);
        }
    }

    @Override
    public void onDestroyView() {
        emptyViewController.onDestroyView();
        compatSetAdapter(null);
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

        private final Pager<?> pager;
        private final EndlessAdapter<?> adapter;
        private final OnScrollListener listenerDelegate;

        PagingScrollListener(Pager<?> pager, EndlessAdapter<?> adapter, OnScrollListener listenerDelegate) {
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
