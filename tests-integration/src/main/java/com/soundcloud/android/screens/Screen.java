package com.soundcloud.android.screens;

import com.soundcloud.android.screens.elements.ActionBarElement;
import com.soundcloud.android.screens.search.SearchPlaylistTagsScreen;
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
        this.actionBar = new ActionBarElement(solo);
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

    public SearchPlaylistTagsScreen clickPhysicalSearchButton() {
        solo.sendKey(KeyEvent.KEYCODE_SEARCH);
        return new SearchPlaylistTagsScreen(solo);
    }

    public boolean isVisible() {
        return getActivity().getSimpleName().equals(solo.getCurrentActivity().getClass().getSimpleName());
    }

    public ActionBarElement actionBar() {
        return actionBar;
    }

    abstract protected Class getActivity();
}