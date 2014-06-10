package com.soundcloud.android.screens.elements;

import com.robotium.solo.Solo;
import com.soundcloud.android.tests.DefaultViewElement;
import com.soundcloud.android.tests.ViewElement;

import android.view.View;
import android.widget.AbsListView;

public class ListElement {
    private final Solo testDriver;
    private final AbsListView absListView;

    public ListElement(View element, Solo driver) {
        testDriver = driver;
        absListView = (AbsListView)element;
    }

    public void clickItemAt(int index) {
        getItemAt(index).click();
    }

    public ViewElement getItemAt(int index) {
        return new DefaultViewElement(absListView.getChildAt(index), testDriver);
    }

    public void scrollToBottom() {
        testDriver.scrollToBottom();
    }

    public int getCount() {
        return absListView.getAdapter().getCount();
    }
}
