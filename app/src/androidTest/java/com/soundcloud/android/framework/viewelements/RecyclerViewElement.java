package com.soundcloud.android.framework.viewelements;

import com.soundcloud.android.framework.Han;
import com.soundcloud.android.framework.with.With;

import android.support.v7.widget.RecyclerView;
import android.view.View;

import java.util.List;

public class RecyclerViewElement {
    protected static final int MAX_SCROLLS_TO_FIND_ITEM = 10;

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

    public ViewElement getItemAt(int index) {
        return new DefaultViewElement(recyclerView.getChildAt(index), testDriver);
    }

    public RecyclerViewElement scrollDown() {
        // scrolls partially down the screen, usually about 5 items
        testDriver.scrollDown();
        return this;
    }

    public RecyclerViewElement scrollToPosition(int position){
        testDriver.scrollToPosition(recyclerView, position);
        return this;
    }

    public RecyclerViewElement scrollToBottomOfPage() {
        testDriver.scrollToPosition(recyclerView, getItemCount() - 1);
        return this;
    }

    public ViewElement getItemWithChild(With with, With child) {
        scrollToItem(child);
        final List<ViewElement> items = testDriver.findElements(with);

        for (ViewElement item : items) {
            if (item.findElement(child).isVisible()) {
                return item;
            }
        }

        return new EmptyViewElement("Unable to find an item with the given child");
    }

    public RecyclerView.Adapter getAdapter() {
        return recyclerView.getAdapter();
    }

    public ViewElement scrollToItem(With with) {
        Criteria atLeastPartiallyVisibleCriteria = new Criteria() {
            @Override
            public boolean isSatisfied(ViewElement viewElement) {
                return !(viewElement instanceof EmptyViewElement);
            }
        };

        return scrollUntil(with, atLeastPartiallyVisibleCriteria);
    }

    public ViewElement scrollToFullyVisibleItem(With with) {
        Criteria fullyVisibleItemCriteria = new Criteria() {
            @Override
            public boolean isSatisfied(ViewElement viewElement) {
                return ( !(viewElement instanceof EmptyViewElement) && viewElement.isFullyVisible());
            }
        };

        return scrollUntil(with, fullyVisibleItemCriteria);
    }

    private ViewElement scrollUntil(With with, Criteria criteria) {
        int tries = 0;
        ViewElement result = testDriver.findElement(with);
        while (!criteria.isSatisfied(result)) {
            int previouslyViewedItems = tries * getBoundItemCount();
            int scrollPosition = previouslyViewedItems + lastBoundItemIndex();

            if (scrollPosition > lastItemIndex() || tries > MAX_SCROLLS_TO_FIND_ITEM) {
                return new EmptyViewElement("Unable to scroll to item; item not in list");
            }

            testDriver.scrollToPosition(recyclerView, scrollPosition);
            result = testDriver.findElement(with);
            tries++;
        }
        return result;
    }

    private interface Criteria {
        boolean isSatisfied(ViewElement viewElement);
    }

    private int lastBoundItemIndex() {
        return getBoundItemCount() - 1;
    }

    private int lastItemIndex() {
        return getItemCount() - 1;
    }

    private RecyclerView.LayoutManager getLayoutManager() {
        return recyclerView.getLayoutManager();
    }
}
