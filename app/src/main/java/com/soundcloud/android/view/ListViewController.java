package com.soundcloud.android.view;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import com.soundcloud.android.image.ImageOperations;
import com.soundcloud.android.main.DefaultFragmentLifeCycle;
import org.jetbrains.annotations.Nullable;
import rx.Observable;

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
    private @Nullable AbsListView.OnScrollListener scrollListener;

    @Inject
    public ListViewController(EmptyViewController emptyViewController, ImageOperations imageOperations) {
        this.emptyViewController = emptyViewController;
        this.imageOperations = imageOperations;
    }

    public void setAdapter(ListAdapter adapter) {
        this.adapter = adapter;
    }

    public void setScrollListener(@Nullable AbsListView.OnScrollListener scrollListener) {
        this.scrollListener = scrollListener;
    }

    public <O extends Observable<?>> void connect(final ReactiveListComponent<O> listComponent, O observable) {
        emptyViewController.connect(listComponent, observable);
        absListView.setOnItemClickListener(listComponent);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        Preconditions.checkState(adapter != null, "You must set an adapter before calling onViewCreated");
        emptyViewController.onViewCreated(view, savedInstanceState);

        absListView = (AbsListView) view.findViewById(android.R.id.list);
        absListView.setEmptyView(emptyViewController.getEmptyView());

        if (scrollListener == null) {
            absListView.setOnScrollListener(imageOperations.createScrollPauseListener(false, true));
        } else {
            absListView.setOnScrollListener(imageOperations.createScrollPauseListener(false, true, scrollListener));
        }

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
}
