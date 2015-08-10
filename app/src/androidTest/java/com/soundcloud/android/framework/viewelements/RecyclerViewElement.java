package com.soundcloud.android.framework.viewelements;

import com.soundcloud.android.framework.Han;

import android.support.v7.widget.RecyclerView;
import android.view.View;

public class RecyclerViewElement {

    protected final Han testDriver;
    private final RecyclerView recyclerView;

    public RecyclerViewElement(ViewElement element, Han testDriver) {
        this(element.getView(), testDriver);
    }

    public RecyclerViewElement(View view, Han testDriver) {
        if (!(view instanceof RecyclerView)){
            throw new IllegalArgumentException("Argument must be a valid recycler view");
        }

        this.testDriver = testDriver;
        recyclerView = (RecyclerView) view;
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

    public RecyclerView.Adapter getAdapter() {
        return recyclerView.getAdapter();
    }

    private RecyclerView.LayoutManager getLayoutManager() {
        return recyclerView.getLayoutManager();
    }
}
