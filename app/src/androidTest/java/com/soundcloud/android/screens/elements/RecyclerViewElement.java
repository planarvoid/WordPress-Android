package com.soundcloud.android.screens.elements;

import com.soundcloud.android.Consts;
import com.soundcloud.android.framework.Han;

import android.support.v7.widget.RecyclerView;
import android.view.View;

public class RecyclerViewElement {

    protected final Han testDriver;
    private final RecyclerView recyclerView;

    public RecyclerViewElement(View element, Han testDriver) {
        this.testDriver = testDriver;
        recyclerView = (RecyclerView) element;
    }

    public int getItemCount() {
        // Returns the total number of items in the data set hold by the adapter.
        return getAdapter().getItemCount();
    }

    public int getBoundItemCount() {
        // Returns the number of items in the adapter bound to the parent RecyclerView.
        return getLayoutManager().getChildCount();
    }

    public void scrollToBottom() {
        testDriver.scrollToBottom();
    }

    public void scrollToNextPage() {
        int numberOfScreensBeforePagingOccurs = Consts.LIST_PAGE_SIZE/getBoundItemCount();
        // since we start on the first page
        for (int i=1; i<numberOfScreensBeforePagingOccurs; i++) {
            testDriver.scrollToBottom();
        }
    }

    private RecyclerView.Adapter getAdapter() {
        return recyclerView.getAdapter();
    }

    private RecyclerView.LayoutManager getLayoutManager() {
        return recyclerView.getLayoutManager();
    }
}
