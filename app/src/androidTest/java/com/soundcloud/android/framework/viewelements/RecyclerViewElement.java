package com.soundcloud.android.framework.viewelements;

import com.soundcloud.android.framework.Han;
import com.soundcloud.android.screens.elements.StreamCardElement;
import com.soundcloud.android.utils.Log;

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

    public ViewElement getItemAt(int position) {
        int childCount = recyclerView.getChildCount();
        for(int i = 0; i < childCount; i++) {
            View view = recyclerView.getChildAt(i);
            if(recyclerView.getChildAdapterPosition(view) == position) {
                DefaultViewElement viewElement = new DefaultViewElement(view, testDriver);
                if(viewElement.isFullyVisible()) {
                    return viewElement;
                }
            }
        }
        return new EmptyViewElement("RecyclerView Item");
    }

    public ViewElement scrollToItemAt(int position) {
        testDriver.scrollToPosition(recyclerView, position);
        return getItemAt(position);
    }

    public ViewElement scrollToItem(Criteria criteria) {
        int itemCount = getItemCount();
        for(int i = 0; i < itemCount; i++) {
            scrollToItemAt(i);
            scrollViewToBeFullyVisible(i, itemCount);
            if(criteria.isSatisfied(getItemAt(i))) {
                return getItemAt(i);
            }
        }
        return new EmptyViewElement("Item With Criteria not found");
    }

    private void scrollViewToBeFullyVisible(int position, int boundary) {
        while(!getItemAt(position).isFullyVisible() && position < boundary) {
            scrollToItemAt(position++);
        }
    }

    public RecyclerViewElement scrollToTop() {
        testDriver.scrollToPosition(recyclerView, 0);
        return this;
    }

    public RecyclerViewElement scrollToBottom() {
        testDriver.scrollToPosition(recyclerView, getItemCount() - 1);
        return this;
    }

    public interface Criteria {
        boolean isSatisfied(ViewElement viewElement);
    }

    public int getItemCount() {
        return getAdapter().getItemCount();
    }

    private RecyclerView.Adapter getAdapter() {
        return recyclerView.getAdapter();
    }
}
