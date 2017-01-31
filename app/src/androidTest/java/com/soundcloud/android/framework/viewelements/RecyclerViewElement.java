package com.soundcloud.android.framework.viewelements;

import com.soundcloud.android.framework.Han;
import com.soundcloud.android.screens.elements.AdapterElement;

import android.support.v7.widget.RecyclerView;
import android.view.View;

public class RecyclerViewElement extends AdapterElement {

    private final RecyclerView recyclerView;

    public RecyclerViewElement(View view, Han driver) {
        super(view, driver);
        if (!(view instanceof RecyclerView)) {
            throw new IllegalArgumentException("Argument must be a valid recycler view");
        }

        recyclerView = (RecyclerView) view;
    }

    public AdapterElement scrollToBottom() {
        getTestDriver().scrollToPosition(recyclerView, getItemCount() - 1);
        return this;
    }

    public AdapterElement scrollToTop() {
        getTestDriver().scrollToPosition(recyclerView, 0);
        return this;
    }

    public int getItemCount() {
        return getAdapter().getItemCount();
    }

    public RecyclerView.Adapter getAdapter() {
        waiter.waitForContentAndRetryIfLoadingFailed();
        return recyclerView.getAdapter();
    }
}
