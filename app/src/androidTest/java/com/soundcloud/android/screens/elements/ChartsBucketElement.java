package com.soundcloud.android.screens.elements;

import com.soundcloud.android.R;
import com.soundcloud.android.framework.Han;
import com.soundcloud.android.framework.viewelements.ViewElement;
import com.soundcloud.android.framework.with.With;
import com.soundcloud.android.screens.discovery.AllGenresScreen;
import com.soundcloud.android.screens.discovery.ChartsScreen;

public class ChartsBucketElement {
    private final Han testDriver;

    public ChartsBucketElement(Han testDriver) {
        this.testDriver = testDriver;
    }

    public AllGenresScreen clickViewAll() {
        viewAllGenres().click();
        return new AllGenresScreen(testDriver);
    }

    public ChartsScreen clickNewAndHot() {
        newAndHot().click();
        return new ChartsScreen(testDriver);
    }

    private ViewElement viewAllGenres() {
        return testDriver.scrollToItem(With.id(R.id.charts_genre_view_all_text));
    }

    private ViewElement newAndHot() {
        return testDriver.scrollToItem(With.text(R.string.charts_trending));
    }
}
