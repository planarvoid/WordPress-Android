package com.soundcloud.android.screens.elements;

import com.soundcloud.android.tests.Han;

import android.view.View;
import android.widget.AbsListView;

public class ListElement {
    private final Han testDriver;
    private final AbsListView mView;

    public ListElement(View element, Han driver) {
        testDriver = driver;
        mView = (AbsListView)element;
    }

    public void clickItemAt(int index) {
        getItemAt(index).click();
    }

    public ViewElement getItemAt(int index) {
        return new ViewElement(mView.getChildAt(index), testDriver);
    }

    public void scrollToBottom() {
        testDriver.scrollToBottom(mView);
    }

    public int getCount() {
        return mView.getAdapter().getCount();
    }
}