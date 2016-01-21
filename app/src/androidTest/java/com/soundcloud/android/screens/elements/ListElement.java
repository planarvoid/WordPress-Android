package com.soundcloud.android.screens.elements;

import com.soundcloud.android.framework.Han;
import com.soundcloud.android.framework.Waiter;
import com.soundcloud.android.framework.viewelements.DefaultViewElement;
import com.soundcloud.android.framework.viewelements.EmptyViewElement;
import com.soundcloud.android.framework.viewelements.ViewElement;
import com.soundcloud.android.framework.with.With;

import android.view.View;
import android.widget.AbsListView;
import android.widget.ListAdapter;

public class ListElement {
    private final Han testDriver;
    private final AbsListView absListView;
    private final Waiter waiter;

    public ListElement(View element, Han driver) {
        testDriver = driver;
        absListView = (AbsListView)element;
        waiter = new Waiter(testDriver);
    }

    public ViewElement getItemAt(int index) {
        return new DefaultViewElement(absListView.getChildAt(index), testDriver);
    }

    public int getVisibleItemViewCount() {
        return absListView.getChildCount();
    }

    public int getItemCount() {
        return getAdapter().getCount();
    }

    public ListAdapter getAdapter(){
        waiter.waitForItemCountToIncrease(absListView.getAdapter(), 0);
        return absListView.getAdapter();
    }

    public void scrollToBottom() {
        testDriver.scrollToBottom(absListView);
    }
}
