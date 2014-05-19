package com.soundcloud.android.view;

import com.google.common.annotations.VisibleForTesting;
import com.soundcloud.android.image.ImageOperations;
import org.jetbrains.annotations.Nullable;
import rx.observables.ConnectableObservable;

import android.annotation.TargetApi;
import android.os.Build;
import android.view.View;
import android.widget.AbsListView;
import android.widget.GridView;
import android.widget.ListAdapter;
import android.widget.ListView;

import javax.inject.Inject;

public class ListViewController {

    private final EmptyViewController emptyViewController;
    private final ImageOperations imageOperations;

    private AbsListView absListView;

    @Inject
    public ListViewController(EmptyViewController emptyViewController, ImageOperations imageOperations) {
        this.emptyViewController = emptyViewController;
        this.imageOperations = imageOperations;
    }

    public <OT extends ConnectableObservable<?>> void onViewCreated(
            final ReactiveListComponent<OT> listComponent, OT observable, View view, ListAdapter adapter,
            @Nullable AbsListView.OnScrollListener scrollListener) {
        emptyViewController.onViewCreated(listComponent, observable, view);

        absListView = (AbsListView) view.findViewById(android.R.id.list);
        absListView.setOnItemClickListener(listComponent);
        absListView.setEmptyView(emptyViewController.getEmptyView());
        compatSetAdapter(adapter);
        if (scrollListener != null) {
            absListView.setOnScrollListener(imageOperations.createScrollPauseListener(false, true, scrollListener));
        } else {
            absListView.setOnScrollListener(imageOperations.createScrollPauseListener(false, true));
        }
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

    public void onDestroyView() {
        compatSetAdapter(null);
        absListView = null;
    }

    public EmptyListView getEmptyView() {
        return emptyViewController.getEmptyView();
    }

    @VisibleForTesting
    AbsListView getListView() {
        return absListView;
    }
}
