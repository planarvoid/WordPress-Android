package com.soundcloud.android.screens;

import com.soundcloud.android.R;
import com.soundcloud.android.framework.Han;
import com.soundcloud.android.framework.viewelements.ViewElement;
import com.soundcloud.android.framework.with.With;
import com.soundcloud.android.main.MainActivity;
import com.soundcloud.android.screens.elements.StreamList;
import com.soundcloud.android.screens.elements.TrackItemMenuElement;
import com.soundcloud.android.screens.elements.VisualPlayerElement;
import com.soundcloud.android.screens.explore.ExploreScreen;

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
        waiter.waitForElements(R.id.track_list_item);
        testDriver
                .findElements(With.id(R.id.track_list_item))
                .get(0).click();
        VisualPlayerElement visualPlayerElement = new VisualPlayerElement(testDriver);
        visualPlayerElement.waitForExpandedPlayer();
        return visualPlayerElement;
    }

    public TrackItemMenuElement clickFirstTrackOverflowButton() {
        waiter.waitForContentAndRetryIfLoadingFailed();
        testDriver
                .findElements(With.id(R.id.overflow_button))
                .get(0).click();
        return new TrackItemMenuElement(testDriver);
    }

    private StreamList streamList() {
        ViewElement list = testDriver.findElement(With.id(android.R.id.list));
        return new StreamList(list);
    }

    public MenuScreen openMenu() {
        return menuScreen.open();
    }

    public ExploreScreen openExploreFromMenu() {
        return menuScreen.open().clickExplore();
    }
}
