package com.soundcloud.android.screens.discovery;

import com.soundcloud.android.R;
import com.soundcloud.android.framework.Han;
import com.soundcloud.android.framework.viewelements.TextElement;
import com.soundcloud.android.framework.viewelements.ViewElement;
import com.soundcloud.android.framework.with.With;
import com.soundcloud.android.screens.Screen;
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
        return new TextElement(firstTrack()).getText();
    }

    public VisualPlayerElement clickFirstTrack() {
        firstTrack().click();
        return new VisualPlayerElement(testDriver);
    }

    private ViewElement firstTrack() {
        return scrollToItem(With.id(R.id.list_item_subheader));
    }

    private ViewPagerElement getViewPager() {
        return new ViewPagerElement(testDriver);
    }

    private boolean waitForTracksToLoad() {
        return waiter.waitForElement(R.id.track_list_item);
    }
}
