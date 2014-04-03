package com.soundcloud.android.screens;

import com.soundcloud.android.screens.elements.ActionBarElement;
import com.soundcloud.android.screens.search.PlaylistTagsScreen;
import com.soundcloud.android.tests.Han;
import com.soundcloud.android.tests.Waiter;

import android.R;
import android.view.KeyEvent;

public abstract class Screen {
    protected Han solo;
    protected Waiter waiter;
    protected static final int CONTENT_ROOT = R.id.content;

    protected ActionBarElement actionBar;

    public Screen(Han solo) {
        this.solo = solo;
        this.waiter = new Waiter(solo);
        waiter.waitForActivity(getActivity());
        waiter.waitForElement(CONTENT_ROOT);
    }

    public void pullToRefresh() {
        solo.swipeDownToRefresh();
        waiter.waitForTextToDisappear("Loading");
    }

    public void swipeLeft() {
        solo.swipeLeft();
    }

    public void swipeRight() {
        solo.swipeRight();
    }

    public PlaylistTagsScreen clickPhysicalSearchButton() {
        solo.sendKey(KeyEvent.KEYCODE_SEARCH);
        return new PlaylistTagsScreen(solo);
    }

    public boolean isVisible() {
        return getActivity().getSimpleName().equals(solo.getCurrentActivity().getClass().getSimpleName());
    }

    public ActionBarElement actionBar() {
        return new ActionBarElement(solo);
    }

    abstract protected Class getActivity();
}