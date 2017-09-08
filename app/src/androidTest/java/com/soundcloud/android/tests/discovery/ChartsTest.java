package com.soundcloud.android.tests.discovery;

import static com.soundcloud.android.R.string;
import static com.soundcloud.android.R.string.all_genres;
import static com.soundcloud.android.R.string.charts_audio;
import static com.soundcloud.android.R.string.charts_business;
import static com.soundcloud.android.R.string.charts_country;
import static com.soundcloud.android.R.string.charts_top;
import static com.soundcloud.android.R.string.charts_trending;
import static com.soundcloud.android.framework.TestUser.chartsTestUser;
import static com.soundcloud.android.properties.Flag.DISCOVER_BACKEND;
import static java.util.Locale.US;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

import com.soundcloud.android.R;
import com.soundcloud.android.framework.TestUser;
import com.soundcloud.android.main.MainActivity;
import com.soundcloud.android.properties.Flag;
import com.soundcloud.android.screens.discovery.AllGenresScreen;
import com.soundcloud.android.screens.discovery.ChartsScreen;
import com.soundcloud.android.screens.discovery.OldDiscoveryScreen;
import com.soundcloud.android.screens.elements.VisualPlayerElement;
import com.soundcloud.android.tests.ActivityTest;
import org.junit.After;
import org.junit.Test;

import java.util.Locale;

public class ChartsTest extends ActivityTest<MainActivity> {
    private static final String CHARTS_TRACKING_SCENARIO = "specs/charts-tracking.spec";
    private OldDiscoveryScreen discoveryScreen;

    public ChartsTest() {
        super(MainActivity.class);
    }

    @Override
    protected TestUser getUserForLogin() {
        return chartsTestUser;
    }

    @Override
    protected void beforeActivityLaunched() {
        getFeatureFlags().disable(DISCOVER_BACKEND);
    }

    @Override
    @After
    public void tearDown() throws Exception {
        getFeatureFlags().reset(DISCOVER_BACKEND);
        super.tearDown();
    }

    @Override
    public void setUp() throws Exception {
        super.setUp();
        discoveryScreen = mainNavHelper.goToOldDiscovery();
        discoveryScreen.waitForContentAndRetryIfLoadingFailed();
    }

    @Test
    public void testPlayTrackFromCharts() throws Exception {
        mrLocalLocal.startEventTracking();

        // New and Hot
        ChartsScreen newAndHotScreen = discoveryScreen.chartBucket().clickNewAndHot();
        assertThat(newAndHotScreen.activeTabTitle(), equalTo(solo.getString(charts_trending)));

        // Go to Top 50
        newAndHotScreen.swipeLeft();
        assertThat(newAndHotScreen.activeTabTitle(), equalTo(solo.getString(charts_top)));

        // Go back to Discovery Page
        solo.goBack();

        // View all charts
        final AllGenresScreen allGenresScreen = discoveryScreen.chartBucket().clickViewAll();
        assertThat(allGenresScreen.getActionBarTitle(), equalTo(solo.getString(all_genres)));

        // Click the Country Genre
        final ChartsScreen countryScreen = allGenresScreen.clickGenre(solo.getString(charts_country));
        assertThat(countryScreen.getActionBarTitle(), equalTo("Country charts"));
        assertThat(countryScreen.activeTabTitle(), equalTo(solo.getString(charts_trending)));

        // Like the first track
        countryScreen.firstTrack().clickOverflowButton().toggleLike();

        // Go back to View All Charts
        solo.goBack();

        // Go to Audio
        allGenresScreen.swipeLeft();
        assertThat(countryScreen.activeTabTitle(), equalTo(solo.getString(charts_audio).toUpperCase(US)));

        // Go to Science section
        final ChartsScreen scienceScreen = allGenresScreen.clickGenre(solo.getString(charts_business));

        // Play first track
        final String trackTitle = scienceScreen.firstTrackTitle();
        final VisualPlayerElement player = scienceScreen.clickFirstTrack();

        assertThat(player.isExpandedPlayerPlaying(), equalTo(true));
        assertThat(trackTitle, equalTo(player.getTrackTitle()));

        mrLocalLocal.verify(CHARTS_TRACKING_SCENARIO);
    }
}
