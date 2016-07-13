package com.soundcloud.android.screens.discovery;

import com.soundcloud.android.R;
import com.soundcloud.android.framework.Han;
import com.soundcloud.android.framework.with.With;
import com.soundcloud.android.screens.Screen;
import com.soundcloud.android.screens.elements.ViewPagerElement;

public class AllGenresScreen extends Screen {
    private static final Class ACTIVITY = AllGenresScreen.class;

    @Override
    protected Class getActivity() {
        return ACTIVITY;
    }

    public AllGenresScreen(Han solo) {
        super(solo);
        waitForGenresToLoad();
    }

    @Override
    public boolean isVisible() {
        return waitForGenresToLoad();
    }

    public String activeTabTitle() {
        return getViewPager().getCurrentTabText();
    }

    public ChartsScreen clickGenre(String genre) {
        testDriver.findOnScreenElement(With.text(genre)).click();
        return new ChartsScreen(testDriver);
    }

    private ViewPagerElement getViewPager() {
        return new ViewPagerElement(testDriver);
    }

    private boolean waitForGenresToLoad() {
        return waiter.waitForElement(R.id.chart_list_item);
    }
}
