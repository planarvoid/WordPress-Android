package com.soundcloud.android.tests.discovery;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

import com.soundcloud.android.R;
import com.soundcloud.android.framework.TestUser;
import com.soundcloud.android.framework.annotation.DiscoveryChartsTest;
import com.soundcloud.android.framework.helpers.mrlogga.TrackingActivityTest;
import com.soundcloud.android.main.MainActivity;
import com.soundcloud.android.screens.discovery.AllGenresScreen;
import com.soundcloud.android.screens.discovery.ChartsScreen;
import com.soundcloud.android.screens.discovery.DiscoveryScreen;
import com.soundcloud.android.screens.elements.VisualPlayerElement;

import java.util.Locale;

@DiscoveryChartsTest
public class ChartsTest extends TrackingActivityTest<MainActivity> {
    public static final String CHARTS_TRACKING_SCENARIO = "charts-tracking";
    private DiscoveryScreen discoveryScreen;

    public ChartsTest() {
        super(MainActivity.class);
    }

    @Override
    protected TestUser getUserForLogin() {
        return TestUser.chartsTestUser;
    }

    @Override
    public void setUp() throws Exception {
        super.setUp();
        discoveryScreen = mainNavHelper.goToDiscovery();
        discoveryScreen.waitForContentAndRetryIfLoadingFailed();
    }

    public void testPlayTrackFromCharts() throws Exception {
        startEventTracking();

        // New and Hot
        ChartsScreen newAndHotScreen = discoveryScreen.chartBucket().clickNewAndHot();
        assertThat(newAndHotScreen.activeTabTitle(), equalTo(solo.getString(R.string.charts_trending)));

        // Go to Top 50
        newAndHotScreen.swipeLeft();
        assertThat(newAndHotScreen.activeTabTitle(), equalTo(solo.getString(R.string.charts_top)));

        // Go back to Discovery Page
        solo.goBack();

        // View all charts
        final AllGenresScreen allGenresScreen = discoveryScreen.chartBucket().clickViewAll();
        assertThat(allGenresScreen.getActionBarTitle(), equalTo(solo.getString(R.string.all_genres)));

        // Click the Country Genre
        final ChartsScreen countryScreen = allGenresScreen.clickGenre(solo.getString(R.string.charts_country));
        assertThat(countryScreen.getActionBarTitle(), equalTo("Country charts"));
        assertThat(countryScreen.activeTabTitle(), equalTo(solo.getString(R.string.charts_trending)));

        // Like the first track
        countryScreen.firstTrack().clickOverflowButton().toggleLike();

        // Go back to View All Charts
        solo.goBack();

        // Go to Audio
        allGenresScreen.swipeLeft();
        assertThat(countryScreen.activeTabTitle(), equalTo(solo.getString(R.string.charts_audio).toUpperCase(Locale.US)));

        // Go to Science section
        final ChartsScreen scienceScreen = allGenresScreen.clickGenre(solo.getString(R.string.charts_business));

        // Play first track
        final String trackTitle = scienceScreen.firstTrackTitle();
        final VisualPlayerElement player = scienceScreen.clickFirstTrack();

        assertThat(player.isExpandedPlayerPlaying(), equalTo(true));
        assertThat(trackTitle, equalTo(player.getTrackTitle()));

        finishEventTracking(CHARTS_TRACKING_SCENARIO);
    }
}
