package com.soundcloud.android.screens;

import com.soundcloud.android.main.MainActivity;
import com.soundcloud.android.screens.elements.PlayerElement;
import com.soundcloud.android.tests.Han;

public class StreamScreen extends Screen {
    private static final Class ACTIVITY = MainActivity.class;

    public StreamScreen(Han solo) {
        super(solo);
        waiter.waitForFragmentByTag("stream_fragment");
    }

    public void clickFirstTrack() {
        solo.clickInList(0);
    }

    @Override
    protected Class getActivity() {
        return ACTIVITY;
    }
}
