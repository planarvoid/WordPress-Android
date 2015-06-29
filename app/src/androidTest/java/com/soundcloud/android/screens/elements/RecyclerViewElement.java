package com.soundcloud.android.screens.elements;

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

    public RecyclerViewElement scrollDown() {
        // scrolls partially down the screen, usually about 5 items
        testDriver.scrollDown();
        return this;
    }

    public RecyclerViewElement scrollToBottomOfPage() {
        testDriver.scrollToPosition(recyclerView, getItemCount()-1);
        return this;
    }

    private RecyclerView.Adapter getAdapter() {
        return recyclerView.getAdapter();
    }

    private RecyclerView.LayoutManager getLayoutManager() {
        return recyclerView.getLayoutManager();
    }
}
