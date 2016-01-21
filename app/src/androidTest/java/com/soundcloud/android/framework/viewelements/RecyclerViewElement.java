package com.soundcloud.android.framework.viewelements;

import com.soundcloud.android.framework.Han;
import com.soundcloud.android.framework.with.With;

import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;


public class RecyclerViewElement extends DefaultViewElement {

    private final RecyclerView recyclerView;

    public RecyclerViewElement(View view, Han driver) {
        super(view, driver);
        if (!(view instanceof RecyclerView)) {
            throw new IllegalArgumentException("Argument must be a valid recycler view");
        }

        recyclerView = (RecyclerView) view;
    }

    public RecyclerViewElement scrollToBottom() {
        getTestDriver().scrollToPosition(recyclerView, getItemCount() - 1);
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
