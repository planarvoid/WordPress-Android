package com.soundcloud.android.screens;

import com.soundcloud.android.framework.Han;
import com.soundcloud.android.framework.Waiter;
import com.soundcloud.android.framework.viewelements.ViewElement;
import com.soundcloud.android.framework.with.With;
import com.soundcloud.android.screens.elements.ToolBarElement;
import com.soundcloud.android.screens.elements.GoBackOnlineDialogElement;

import android.R;

public abstract class Screen {
    protected Han testDriver;
    protected Waiter waiter;
    protected static final int CONTENT_ROOT = R.id.content;

    public Screen(Han solo) {
        this.testDriver = solo;
        this.waiter = new Waiter(solo);
        waiter.waitForActivity(getActivity());
        waiter.waitForElement(CONTENT_ROOT);
    }

    public void pullToRefresh() {
        testDriver.swipeDown();
        waiter.waitForTextToDisappear("Loading");
    }

    public void swipeLeft() {
        testDriver.swipeLeft();
        testDriver.sleep(1000); // wtf
    }

    public ViewElement emptyView(){
        return testDriver.findElement(With.id(com.soundcloud.android.R.id.empty_view_holder));
    }

    public void swipeRight() {
        testDriver.swipeRight();
    }

    public boolean isVisible() {
        return getActivity().getSimpleName().equals(testDriver.getCurrentActivity().getClass().getSimpleName());
    }

    public ToolBarElement actionBar() {
        return new ToolBarElement(testDriver);
    }

    public GoBackOnlineDialogElement getGoBackOnlineDialog() {
        return new GoBackOnlineDialogElement(testDriver);
    }

    abstract protected Class getActivity();

    @Override
    public String toString() {
        return String.format("Page: %s, Activity: %s, CurrentActivity: %s",
                getClass().getSimpleName().toString(),
                getActivity().getSimpleName().toString(),
                testDriver.getCurrentActivity().toString()
        );
    }
}
