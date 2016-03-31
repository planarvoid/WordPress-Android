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

import java.util.List;

public class ListElement extends AdapterElement {
    private final AbsListView absListView;

    public ListElement(View element, Han driver) {
        super(element, driver);
        absListView = (AbsListView)element;
    }

    public ViewElement getItemAt(int index) {
        return new DefaultViewElement(absListView.getChildAt(index), getTestDriver());
    }

    public int getItemCount() {
        return getAdapter().getCount();
    }

    public ListAdapter getAdapter(){
        waiter.waitForItemCountToIncrease(absListView.getAdapter(), 0);
        return absListView.getAdapter();
    }

    public ListElement scrollToBottom() {
        getTestDriver().scrollToBottom(absListView);
        return this;
    }
}
