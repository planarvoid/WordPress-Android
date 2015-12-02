package com.soundcloud.android.framework.viewelements;

import com.soundcloud.android.framework.Han;

import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;


public class RecyclerViewElement {

    private static final String TAG = "RecyclerView";
    protected final Han testDriver;
    private final RecyclerView recyclerView;

    public RecyclerViewElement(ViewElement element, Han testDriver) {
        this(element.getView(), testDriver);
    }

    public RecyclerViewElement(View view, Han testDriver) {
        if (!(view instanceof RecyclerView)) {
            throw new IllegalArgumentException("Argument must be a valid recycler view");
        }

        this.testDriver = testDriver;
        recyclerView = (RecyclerView) view;
    }

    public ViewElement getItemAt(int position) {
        int childCount = recyclerView.getChildCount();
        Log.i(TAG, "child count: " + childCount);
        for (int i = 0; i < childCount; i++) {
            View view = recyclerView.getChildAt(i);
            Log.i(TAG, "view at position " + i + ": " + view);
            if (recyclerView.getChildAdapterPosition(view) == position) {
                Log.i(TAG, "found item at position " + i);
                DefaultViewElement viewElement = new DefaultViewElement(view, testDriver);
                if (viewElement.isFullyVisible()) {
                    Log.i(TAG, "element was fully visible");
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
        Log.i(TAG, String.format("Has %d items", itemCount));
        for (int i = 0; i < itemCount; i++) {
            ViewElement listItem = scrollToItemAt(i);
            scrollViewToBeFullyVisible(i, itemCount);
            if (criteria.isSatisfied(listItem)) {
                Log.i(TAG, String.format("View matching criteria: %s found", criteria.description()));
                return listItem;
            }
            Log.i(TAG, String.format("View matching criteria: %s not found", criteria.description()));
        }
        return new EmptyViewElement("Couldn't find list element matching " + criteria.description());
    }

    private void scrollViewToBeFullyVisible(int position, int boundary) {
        while (!getItemAt(position).isFullyVisible() && position < boundary) {
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

        String description();
    }

    public int getItemCount() {
        return getAdapter().getItemCount();
    }

    public RecyclerView.Adapter getAdapter() {
        return recyclerView.getAdapter();
    }
}
