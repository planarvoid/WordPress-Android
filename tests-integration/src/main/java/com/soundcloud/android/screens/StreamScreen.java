package com.soundcloud.android.screens;

import com.soundcloud.android.main.MainActivity;
import com.soundcloud.android.screens.elements.StreamList;
import com.soundcloud.android.tests.Han;
import com.soundcloud.android.tests.ViewElement;
import com.soundcloud.android.tests.with.With;

public class StreamScreen extends Screen {
    private static final Class ACTIVITY = MainActivity.class;

    public StreamScreen(Han solo) {
        super(solo);
        waiter.waitForFragmentByTag("stream_fragment");
    }

    public String getTitle() {
        return actionBar().getTitle();
    }

    public int getItemCount() {
        waiter.waitForContentAndRetryIfLoadingFailed();
        return streamList().getItemCount();
    }

    @Override
    protected Class getActivity() {
        return ACTIVITY;
    }

    public void clickFirstItem() {
        waiter.waitForContentAndRetryIfLoadingFailed();
        testDriver.findElement(With.id(android.R.id.list)).click();
    }

    private StreamList streamList() {
        ViewElement list = testDriver.findElement(With.id(android.R.id.list));
        return new StreamList(list);
    }
}
