package com.soundcloud.android.tests.upsell;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import com.soundcloud.android.framework.TestUser;
import com.soundcloud.android.framework.helpers.ConfigurationHelper;
import com.soundcloud.android.framework.helpers.mrlogga.TrackingActivityTest;
import com.soundcloud.android.main.MainActivity;
import com.soundcloud.android.properties.Flag;
import com.soundcloud.android.screens.PlaylistDetailsScreen;
import com.soundcloud.android.screens.StreamScreen;
import com.soundcloud.android.screens.TrackLikesScreen;
import com.soundcloud.android.screens.UpgradeScreen;
import com.soundcloud.android.screens.search.SearchResultsScreen;

public class UpsellTest extends TrackingActivityTest<MainActivity> {
    private static final String MIDTIER_TEST_SCENARIO = "midtier-tracking-test";

    private StreamScreen streamScreen;

    public UpsellTest() {
        super(MainActivity.class);
    }

    @Override
    protected void logInHelper() {
        TestUser.upsellUser.logIn(getInstrumentation().getTargetContext());
    }

    @Override
    public void setUp() throws Exception {
        setDependsOn(Flag.OFFLINE_SYNC);
        super.setUp();
        ConfigurationHelper.enableUpsell(solo.getCurrentActivity());

        streamScreen = new StreamScreen(solo);
    }

    public void testClickingOnMidTierTrackInStreamOpensUpsell() {
        final UpgradeScreen upgradeScreen = streamScreen.clickMidTierTrackForUpgrade("mid-tier");

        assertUpgradeScreenVisible(upgradeScreen);
    }

    public void ignore_testClickingOnMidTierTrackInLikesOpensUpsell() {
        final TrackLikesScreen likesScreen = streamScreen.openMenu().clickLikes();

        final UpgradeScreen upgradeScreen = likesScreen.clickMidTierTrackForUpgrade(0);

        assertUpgradeScreenVisible(upgradeScreen);
    }

    public void ignore_testClickingOnMidTierTrackFiresTrackingEvents() {
        mrLoggaVerifier.startLogging();

        final TrackLikesScreen likesScreen = streamScreen.openMenu().clickLikes();
        likesScreen.clickMidTierTrackForUpgrade(0);

        mrLoggaVerifier.finishLogging();
        mrLoggaVerifier.isValid(MIDTIER_TEST_SCENARIO);
    }

    public void ignore_testClickingOnMidTierTrackInPlaylistOpensUpsell() {
        final PlaylistDetailsScreen playlistDetailsScreen = streamScreen.openMenu().clickPlaylists().clickPlaylistAt(0);

        final UpgradeScreen upgradeScreen = playlistDetailsScreen.clickMidTierTrackForUpgrade(0);

        assertUpgradeScreenVisible(upgradeScreen);
    }

    public void ignore_testClickingOnMidTierTrackInSearchEverythingOpensUpsell() {
        final SearchResultsScreen searchResultsScreen = streamScreen.actionBar()
                .clickSearchButton()
                .actionBar()
                .doSearch("idon'tgetit muff");

        final UpgradeScreen upgradeScreen = searchResultsScreen.clickMidTierTrackForUpgrade("Muff");

        assertUpgradeScreenVisible(upgradeScreen);
    }

    public void ignore_testClickingOnMidTierTrackInSearchTracksOpensUpsell() {
        final SearchResultsScreen searchResultsScreen = streamScreen.actionBar()
                .clickSearchButton()
                .actionBar()
                .doSearch("idon'tgetit muff");

        final UpgradeScreen upgradeScreen = searchResultsScreen
                .touchTracksTab()
                .clickMidTierTrackForUpgrade("Muff");

        assertUpgradeScreenVisible(upgradeScreen);
    }

    private void assertUpgradeScreenVisible(UpgradeScreen upgradeScreen) {
        assertThat(upgradeScreen.isVisible(), is(true));
    }

}
