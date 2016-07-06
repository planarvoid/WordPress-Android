package com.soundcloud.android.tests.search;

import com.soundcloud.android.framework.TestUser;
import com.soundcloud.android.framework.annotation.EventTrackingTest;
import com.soundcloud.android.framework.helpers.mrlogga.TrackingActivityTest;
import com.soundcloud.android.main.MainActivity;
import com.soundcloud.android.screens.discovery.SearchScreen;

@EventTrackingTest
public class SearchResultsTrackingTest extends TrackingActivityTest<MainActivity> {
    private static final String ALBUMS_IN_SEARCH = "albums_in_search";
    private SearchScreen searchScreen;

    public SearchResultsTrackingTest() {
        super(MainActivity.class);
    }

    @Override
    protected void logInHelper() {
        TestUser.stationsUser.logIn(getInstrumentation().getTargetContext());
    }

    @Override
    public void setUp() throws Exception {
        super.setUp();
        searchScreen = mainNavHelper.goToDiscovery().clickSearch();
    }

    public void testTappingAlbumOnAlbumsTabOpensAlbumDetails() {
        startEventTracking();

        searchScreen.doSearch("clownstep").goToAlbumsTab().findAndClickFirstAlbumItem();

        finishEventTracking(ALBUMS_IN_SEARCH);
    }
}
