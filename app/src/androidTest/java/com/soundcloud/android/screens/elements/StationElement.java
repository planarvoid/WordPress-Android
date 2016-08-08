package com.soundcloud.android.screens.elements;

import com.soundcloud.android.R;
import com.soundcloud.android.framework.Han;
import com.soundcloud.android.framework.viewelements.TextElement;
import com.soundcloud.android.framework.viewelements.ViewElement;
import com.soundcloud.android.framework.with.With;
import com.soundcloud.android.screens.stations.StationHomeScreen;

public class StationElement {
    private final Han testDriver;
    private final ViewElement wrapped;

    public StationElement(Han testDriver, ViewElement wrapped) {
        this.testDriver = testDriver;
        this.wrapped = wrapped;
    }

    public String getTitle() {
        return new TextElement(wrapped.findOnScreenElement(With.id(R.id.title))).getText();
    }

    public boolean isVisible() {
        return wrapped.isOnScreen();
    }

    public VisualPlayerElement click() {
        wrapped.click();
        return new VisualPlayerElement(testDriver);
    }

    public StationHomeScreen open() {
        wrapped.click();
        return new StationHomeScreen(testDriver);
    }

    static With WithTitle(final Han testDriver, final String title) {
        return new With() {

            @Override
            public boolean apply(ViewElement view) {
                return new StationElement(testDriver, view).getTitle().equals(title);
            }

            @Override
            public String getSelector() {
                return String.format("Station with title %s", title);
            }
        };
    }

    public boolean isPlaying() {
        return nowPlaying().isOnScreen();
    }

    private ViewElement nowPlaying() {
        return wrapped.findElement(With.id(R.id.now_playing));
    }
}
