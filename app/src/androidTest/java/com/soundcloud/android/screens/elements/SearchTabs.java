package com.soundcloud.android.screens.elements;

import com.soundcloud.android.R;
import com.soundcloud.android.framework.Han;
import com.soundcloud.android.framework.with.With;
import com.soundcloud.android.screens.discovery.SearchResultsScreen;

public class SearchTabs extends Tabs {

    private enum Tab {
        ALL, TRACKS, PLAYLISTS, PEOPLE
    }

    public SearchTabs(Han testDriver) {
        super(testDriver);
    }

    public SearchResultsScreen goToAllTab() {
        getTabAt(Tab.ALL.ordinal()).click();
        return new SearchResultsScreen(testDriver);
    }

    public SearchResultsScreen goToTracksTab() {
        getTabAt(Tab.TRACKS.ordinal()).click();
        return new SearchResultsScreen(testDriver);
    }

    public SearchResultsScreen goToPlaylistsTab() {
        getTabAt(Tab.PLAYLISTS.ordinal()).click();
        return new SearchResultsScreen(testDriver);
    }

    public SearchResultsScreen goToPeopleTab() {
        getTabAt(Tab.PEOPLE.ordinal()).click();
        return new SearchResultsScreen(testDriver);
    }

    @Override
    public boolean isVisible() {
        return testDriver.findOnScreenElement(With.id(R.id.tab_indicator)).isOnScreen();
    }
}
