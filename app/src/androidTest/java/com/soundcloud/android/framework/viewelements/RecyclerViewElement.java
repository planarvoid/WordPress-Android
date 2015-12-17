package com.soundcloud.android.framework.viewelements;

import com.soundcloud.android.framework.Han;
import com.soundcloud.android.framework.with.With;

import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;


public class RecyclerViewElement extends DefaultViewElement {

    private class RecyclerViewWrapper {
        protected final RecyclerView recyclerView;

        public RecyclerViewWrapper(RecyclerView recyclerView) {
            this.recyclerView = recyclerView;
        }

        public int getChildCount() {
            waiter.waitForContentAndRetryIfLoadingFailed();
            return recyclerView.getChildCount();
        }

        public View getChildAt(int index) {
            waiter.waitForContentAndRetryIfLoadingFailed();
            return recyclerView.getChildAt(index);
        }

        public int getChildAdapterPosition(View view) {
            waiter.waitForContentAndRetryIfLoadingFailed();
            return recyclerView.getChildAdapterPosition(view);
        }

        public RecyclerView.Adapter getAdapter() {
            waiter.waitForContentAndRetryIfLoadingFailed();
            return recyclerView.getAdapter();
        }
    }

    private static final String TAG = "RecyclerView";
    private static final int MAX_SCROLL_ATTEMPTS = 10;
    private final RecyclerViewWrapper recyclerView;

    public RecyclerViewElement(ViewElement element, Han testDriver) {
        this(element.getView(), testDriver);
    }

    public RecyclerViewElement(View view, Han driver) {
        super(view, driver);
        if (!(view instanceof RecyclerView)) {
            throw new IllegalArgumentException("Argument must be a valid recycler view");
        }

        recyclerView = new RecyclerViewWrapper((RecyclerView) view);
    }

    public ViewElement scrollToItem(With... with) {
        ViewElement viewElement = findElement(with);
        for (int attempts = 0; attempts < MAX_SCROLL_ATTEMPTS && viewElement instanceof EmptyViewElement; attempts++) {
            getTestDriver().swipeUp();
            viewElement = findElement(with);
        }
        viewElement.dragIntoFullVerticalVisibility();
        return viewElement;
    }

    @Deprecated
    public ViewElement getItemAt(int position) {
        int childCount = recyclerView.getChildCount();
        Log.i(TAG, "child count: " + childCount);
        for (int i = 0; i < childCount; i++) {
            View view = recyclerView.getChildAt(i);
            Log.i(TAG, "view at position " + i + ": " + view);
            if (recyclerView.getChildAdapterPosition(view) == position) {
                Log.i(TAG, "found item at position " + i);
                DefaultViewElement viewElement = new DefaultViewElement(view, getTestDriver());
                if (viewElement.isVisible()) {
                    Log.i(TAG, "element was visible");
                    return viewElement;
                }
            }
        }
        return new EmptyViewElement("RecyclerView Item");
    }

    @Deprecated
    public ViewElement scrollToItemAt(int position) {
        getTestDriver().scrollToPosition(recyclerView.recyclerView, position);
        return getItemAt(position);
    }

    @Deprecated
    public ViewElement scrollToItem(Criteria criteria) {
        int itemCount = getItemCount();
        Log.i(TAG, String.format("Has %d items", itemCount));
        for (int i = 0; i < itemCount; i++) {
            ViewElement listItem = scrollToItemAt(i);
            if (criteria.isSatisfied(listItem)) {
                Log.i(TAG, String.format("View matching criteria: %s found", criteria.description()));
                scrollViewToBeFullyVisible(i, itemCount);
                return listItem;
            }
            Log.i(TAG, String.format("View matching criteria: %s not found", criteria.description()));
        }
        return new EmptyViewElement("Couldn't find list element matching " + criteria.description());
    }

    @Deprecated
    private void scrollViewToBeFullyVisible(int position, int boundary) {
        while (!getItemAt(position).isFullyVisible() && position < boundary) {
            scrollToItemAt(position++);
        }
    }

    public RecyclerViewElement scrollToBottom() {
        getTestDriver().scrollToPosition(recyclerView.recyclerView, getItemCount() - 1);
        return this;
    }

    public int getItemCount() {
        return getAdapter().getItemCount();
    }

    public RecyclerView.Adapter getAdapter() {
        return recyclerView.getAdapter();
    }
}
