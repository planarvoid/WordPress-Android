package com.soundcloud.android.framework.viewelements;

import com.soundcloud.android.framework.Han;
import com.soundcloud.android.framework.with.With;

import android.support.v7.widget.RecyclerView;
import android.view.View;

import java.util.List;

public class RecyclerViewElement {
    protected static final int MAX_SCROLLS_TO_FIND_ITEM = 20;

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

    public RecyclerViewElement scrollToPosition(int position){
        testDriver.scrollToPosition(recyclerView, position);
        return this;
    }

    public RecyclerViewElement scrollToBottomOfPage() {
        testDriver.scrollToPosition(recyclerView, getItemCount() - 1);
        return this;
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

    public ViewElement scrollToItemWithChild(With with, final With child) {
        Criteria itemWithChildCriteria = new Criteria() {
            @Override
            public boolean isSatisfied(ViewElement viewElement) {
                return !(viewElement instanceof EmptyViewElement) && itemHasVisibleChild(viewElement, child);
            }
        };
        return scrollUntil(with, itemWithChildCriteria);
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

    public int lastBoundItemIndex() {
        return getBoundItemCount() - 1;
    }

    public int lastItemIndex() {
        return getItemCount() - 1;
    }

    public ViewElement scrollUntil(With with, Criteria criteria) {
        int tries = 0;
        ViewElement result = testDriver.findElement(with);
        while (!criteria.isSatisfied(result)) {
            int scrollPosition = tries + 1;

            if (scrollPosition > lastItemIndex() || tries > MAX_SCROLLS_TO_FIND_ITEM) {
                return new EmptyViewElement("Unable to scroll to item; item not in list");
            }

            testDriver.scrollToPosition(recyclerView, scrollPosition);
            result = testDriver.findElement(with);
            tries++;
        }
        return result;
    }

    public interface Criteria {
        boolean isSatisfied(ViewElement viewElement);
    }

    private RecyclerView.LayoutManager getLayoutManager() {
        return recyclerView.getLayoutManager();
    }

    private boolean itemHasVisibleChild(ViewElement item, With child) {
        return item.findElement(child).isVisible();
    }
}
