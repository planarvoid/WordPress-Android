package com.soundcloud.android.screens;

import com.soundcloud.android.main.MainActivity;
import com.soundcloud.android.screens.elements.StreamList;
import com.soundcloud.android.screens.elements.VisualPlayerElement;
import com.soundcloud.android.screens.explore.ExploreScreen;
import com.soundcloud.android.tests.Han;
import com.soundcloud.android.R;
import com.soundcloud.android.tests.ViewElement;
import com.soundcloud.android.tests.with.With;

public class StreamScreen extends Screen {
    private static final Class ACTIVITY = MainActivity.class;
    private final MenuScreen menuScreen;

    public StreamScreen(Han solo) {
        super(solo);
        waiter.waitForFragmentByTag("stream_fragment");
        menuScreen = new MenuScreen(solo);
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

    public VisualPlayerElement clickFirstTrack() {
        waiter.waitForContentAndRetryIfLoadingFailed();
        testDriver
                .findElement(With.id(android.R.id.list))
                .findElements(With.id(R.id.track_list_item))
                .get(0).click();
        waiter.waitForExpandedPlayer();
        return new VisualPlayerElement(testDriver);
    }

    private StreamList streamList() {
        ViewElement list = testDriver.findElement(With.id(android.R.id.list));
        return new StreamList(list);
    }

    public ExploreScreen openExploreFromMenu() {
        return menuScreen.open().clickExplore();
    }
}
