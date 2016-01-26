package com.soundcloud.android.tests.discovery;

import static com.soundcloud.android.framework.matcher.player.IsPlaying.playing;
import static com.soundcloud.android.framework.matcher.screen.IsVisible.visible;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import com.soundcloud.android.framework.TestUser;
import com.soundcloud.android.main.MainActivity;
import com.soundcloud.android.properties.Flag;
import com.soundcloud.android.screens.UpgradeScreen;
import com.soundcloud.android.screens.discovery.SearchResultsScreen;
import com.soundcloud.android.screens.elements.VisualPlayerElement;
import com.soundcloud.android.tests.ActivityTest;

public class PremiumSearchResultsTest extends ActivityTest<MainActivity> {

    private static final String PREMIUM_SEARCH_QUERY = "booht";

    private SearchResultsScreen searchResultsScreen;

    public PremiumSearchResultsTest() {
        super(MainActivity.class);
    }

    @Override
    protected void logInHelper() {
        TestUser.searchHighTierUser.logIn(getInstrumentation().getTargetContext());
    }

    @Override
    public void setUp() throws Exception {
        setRequiredEnabledFeatures(Flag.SEARCH_RESULTS_HIGH_TIER);
        super.setUp();
        searchResultsScreen = mainNavHelper
                .goToDiscovery()
                .clickSearch()
                .setSearchQuery(PREMIUM_SEARCH_QUERY)
                .clickOnCurrentSearchQuery();
    }

    public void testSearchHighTierBucketIsOnScreen() {
        assertThat("Search premium content should be on screen", searchResultsScreen.premiumContentIsOnScreen());
    }

    public void testClickOnPremiumTrackPlaysIt() {
        assertThat("Player should play premium track", searchResultsScreen.clickOnPremiumContent(), is(playing()));
    }

    public void testClickOnPremiumBucketHelpOpensUpgradeScreen() {
        assertThat("Upgrade subscription screen should be visible", searchResultsScreen.clickOnPremiumContentHelp(), is(visible()));
    }

    public void testClickOnPremiumContentBucketOpenSearchPremiumResults() {
        assertThat("Search premium results screen should be visible", searchResultsScreen.clickOnViewPremiumContent(), is(visible()));
    }

    public void testPlaysPremiumTrackFromSearchPremiumResultsScreen() {
        final VisualPlayerElement playerElement =
                searchResultsScreen
                        .clickOnViewPremiumContent()
                        .findAndClickFirstTrackItem();

        assertThat("Player should play premium track", playerElement, is(playing()));
    }

    public void testClickOnUpsellOpensUpgradeScreen() {
        final UpgradeScreen upgradeScreen =
                searchResultsScreen
                        .clickOnViewPremiumContent()
                        .clickOnUpgradeSubscription();

        assertThat("Upgrade subscription screen should be visible", upgradeScreen, is(visible()));
    }
}
