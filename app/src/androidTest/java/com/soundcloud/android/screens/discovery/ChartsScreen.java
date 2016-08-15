package com.soundcloud.android.screens.discovery;

import com.soundcloud.android.R;
import com.soundcloud.android.framework.Han;
import com.soundcloud.android.framework.with.With;
import com.soundcloud.android.screens.Screen;
import com.soundcloud.android.screens.elements.TrackItemElement;
import com.soundcloud.android.screens.elements.ViewPagerElement;
import com.soundcloud.android.screens.elements.VisualPlayerElement;

public class ChartsScreen extends Screen {
    private static final Class ACTIVITY = ChartsScreen.class;

    @Override
    protected Class getActivity() {
        return ACTIVITY;
    }

    public ChartsScreen(Han solo) {
        super(solo);
        waitForTracksToLoad();
    }

    @Override
    public boolean isVisible() {
        return waitForTracksToLoad();
    }

    public String activeTabTitle() {
        return getViewPager().getCurrentTabText();
    }

    public String firstTrackTitle() {
        return firstTrack().getTitle();
    }

    public VisualPlayerElement clickFirstTrack() {
        firstTrack().click();
        return new VisualPlayerElement(testDriver);
    }

    public TrackItemElement firstTrack() {
        return new TrackItemElement(testDriver, scrollToItem(With.id(R.id.track_list_item)));
    }

    private ViewPagerElement getViewPager() {
        return new ViewPagerElement(testDriver);
    }

    private boolean waitForTracksToLoad() {
        return waiter.waitForElement(R.id.track_list_item);
    }
}
