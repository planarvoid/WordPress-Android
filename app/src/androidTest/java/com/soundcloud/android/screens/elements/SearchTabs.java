package com.soundcloud.android.screens.elements;

import static com.robotium.solo.Solo.LEFT;
import static com.soundcloud.android.framework.with.With.text;

import com.soundcloud.android.R;
import com.soundcloud.android.framework.Han;
import com.soundcloud.android.framework.with.With;
import com.soundcloud.android.screens.discovery.SearchResultsScreen;

public class SearchTabs extends Tabs {

    public SearchTabs(Han testDriver) {
        super(testDriver);
    }

    public SearchResultsScreen swipeLeftToAllTab() {
        return swipeLeftToTab(R.string.search_type_all);
    }

    public SearchResultsScreen swipeLeftToTracksTab() {
        return swipeLeftToTab(R.string.search_type_tracks);
    }

    public SearchResultsScreen swipeLeftToPlaylistsTab() {
        return swipeLeftToTab(R.string.search_type_playlists);
    }

    public SearchResultsScreen swipeLeftToAlbumsTab() {
        return swipeLeftToTab(R.string.search_type_albums);
    }

    public SearchResultsScreen swipeLeftToPeopleTab() {
        return swipeLeftToTab(R.string.search_type_people);
    }

    private SearchResultsScreen swipeLeftToTab(int resourceId) {
        testDriver.swipeToItem(LEFT, text(testDriver.getString(resourceId))).click();
        return new SearchResultsScreen(testDriver);
    }

    @Override
    public boolean isVisible() {
        return testDriver.findOnScreenElement(With.id(R.id.tab_indicator)).isOnScreen();
    }
}
