package com.soundcloud.android.tests.stations;

import static com.soundcloud.android.framework.matcher.element.IsVisible.visible;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import com.soundcloud.android.framework.TestUser;
import com.soundcloud.android.framework.annotation.Ignore;
import com.soundcloud.android.framework.annotation.StationsTest;
import com.soundcloud.android.framework.with.With;
import com.soundcloud.android.main.LauncherActivity;
import com.soundcloud.android.properties.Flag;
import com.soundcloud.android.screens.CollectionsScreen;
import com.soundcloud.android.screens.PlaylistDetailsScreen;
import com.soundcloud.android.screens.ViewAllStationsScreen;
import com.soundcloud.android.screens.elements.TrackItemElement;
import com.soundcloud.android.tests.ActivityTest;

@StationsTest
@Ignore // Disabling since collection is not up and running on the CI yet.
public class StationsCollectionTest extends ActivityTest<LauncherActivity> {
    private CollectionsScreen collectionsScreen;

    public StationsCollectionTest() {
        super(LauncherActivity.class);
    }

    @Override
    protected void logInHelper() {
        TestUser.stationsUser.logIn(getInstrumentation().getTargetContext());
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        setRequiredEnabledFeatures(Flag.STATIONS_SOFT_LAUNCH, Flag.COLLECTIONS);

        collectionsScreen = menuScreen
                .open()
                .clickCollections();
    }

    public void testRecentStationsBucket() {
        assertTrue(collectionsScreen.clickRecentStations().isVisible());
    }

    public void testStartedStationShouldBeAddedToRecentStations() {
        final String stationTitle = startStationAndReturnTitle();

        final ViewAllStationsScreen viewAllStationsScreen = collectionsScreen.clickRecentStations();

        assertThat(viewAllStationsScreen.getFirstStation().getTitle(), is(equalTo(stationTitle)));
        assertThat(viewAllStationsScreen.getFirstStation().click(), is(visible()));
    }

    private String startStationAndReturnTitle() {
        final PlaylistDetailsScreen playlistDetailsScreen = collectionsScreen
                .clickOnPlaylist(With.text("track-stations"));

        playlistDetailsScreen.waitForContentAndRetryIfLoadingFailed();

        final TrackItemElement track = playlistDetailsScreen.getTrack(1);
        final String title = track.getTitle();

        track.clickOverflowButton().clickStartStation().pressBackToCollapse();
        solo.goBack();

        return title;
    }
}
