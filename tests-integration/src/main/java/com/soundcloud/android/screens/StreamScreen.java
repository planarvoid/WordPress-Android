package com.soundcloud.android.screens;

import com.soundcloud.android.main.MainActivity;
import com.soundcloud.android.screens.elements.ListElement;
import com.soundcloud.android.tests.Han;
import com.soundcloud.android.tests.with.With;

public class StreamScreen extends Screen {
    private static final Class ACTIVITY = MainActivity.class;

    public StreamScreen(Han solo) {
        super(solo);
        waiter.waitForFragmentByTag("stream_fragment");
    }

    @Override
    protected Class getActivity() {
        return ACTIVITY;
    }

    private ListElement streamList() {
        return testDriver.findElement(With.id(android.R.id.list)).toListView();
    }
}
